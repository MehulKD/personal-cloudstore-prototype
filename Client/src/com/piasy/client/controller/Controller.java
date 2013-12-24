package com.piasy.client.controller;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.piasy.client.dao.DBManager;
import com.piasy.client.dao.FileEntry;
import com.piasy.client.model.Constant;
import com.piasy.client.model.ControlChannel;
import com.piasy.client.model.DataChannel;
import com.piasy.client.model.Flag;
import com.piasy.client.model.Setting;
import com.piasy.client.model.TransferTask;

/**
 * <p>This class is the controller class in the MVC design pattern.
 * This class takes charge of responding the viewer's(activities presented
 * to user) request, by driving models to process data, sending
 * process result to viewer, maintaining the top level logic of this
 * software. It also implements the singletom design pattern, to make
 * sure that all activities will get the same controller, and get the
 * same data in the whole life cycle of this application.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:44 2013/12/17</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class Controller
{
	private static int instanceCount = 0;
	private static Controller instance = null;
	
	/**
	 * The static method to get the reference of the singel
	 * controller.
	 * If there is no instance of controller, this method will
	 * new an instance, and return it, otherwise return it directly.
	 * @return the single instance of controller
	 * */
	public static Controller getController()
	{
		if (instanceCount == 0)
		{
			instance = new Controller();
			instanceCount ++;
		}
		
		return instance;
	}
	
	ControlChannel controlChannel = null;
	String name = null;
	Handler viewerHandler = null;
	Thread controllerThread = null;
	DBManager dbManager = null;
	/**
	 * Initialize the necessary modules of this application, including
	 * the database manager, handler, and control channel. All the query
	 * result won't be returned by the query method, but be sent to viewer
	 * through handler, because the activity's thread can't be blocked.
	 * @param
	 * the viewer's handler,
	 * the database manager,
	 * the name of this user
	 * */
	public boolean init(Handler viewerHandler, DBManager dbManager, String name)
	{
		boolean ret = false;
		this.name = name;
		this.dbManager = dbManager;
		this.viewerHandler = viewerHandler;
		controlChannel = new ControlChannel(name, myHandler);
		ret = controlChannel.init();
		if (ret)
		{
			controllerThread = new Thread(controllerRunnable);
			controllerThread.start();
			
			if (this.dbManager != null && !this.dbManager.dbIsOpen())
				this.dbManager.openDB();
			
			List<FileEntry> entries = dbManager.query();
			for (int i = 0; i < entries.size(); i ++)
			{
				FileEntry entry = entries.get(i);
				if (entry.localStatus == Constant.FILE_STATUS_UPLOAD_PARTLY)
				{
					upload(entry.localPath, entry.transferSize);
				}
				else
				{
					if (entries.get(i).localStatus == Constant.FILE_STATUS_DOWNLOAD_PARTLY)
					{
						//TODO keep download again
					}
				}
			}
		}
		return ret;
	}
	
	Flag exitFlag = null;
	/**
	 * The exit method, it will be called when user want to
	 * exit this application. We also only note down this 
	 * request here, it will be processed in controller's 
	 * thread.
	 * */
	public void exit()
	{
		synchronized (exitFlag)
		{
			exitFlag.setValue(true);
			Log.d(Constant.LOG_LEVEL_DEBUG, "exit...");
		}
	}
	
	/**
	 * Get the username.
	 * @return the user name.
	 * */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Get the reference of database manager. To do something on
	 * database.
	 * @return the reference of database manager.
	 * */
	public DBManager getDbManager()
	{
		return dbManager;
	}
	
	/**
	 * The query method, just note down that the user has made
	 * a query request, and it will be processed in the controller's
	 * own thread. The query result will be sent to viewer via handler.
	 * */
	public void query()
	{
		synchronized (queryFlag)
		{
			queryFlag.setValue(true);
			Log.d(Constant.LOG_LEVEL_DEBUG, "get query");
		}
	}
	
	ArrayList<String> uploadFileName = new ArrayList<String>();
	ArrayList<Long> uploadOffset = new ArrayList<Long>();
	ArrayList<Boolean> requested = new ArrayList<Boolean>();	//look for this list to find the first unrequested file
	Flag uploadFlag = null;
	/**
	 * The upload method, just note down that the user has made
	 * a upload request, and save the file(s) name to be sent,
	 * and it will be processed in the controller's own thread. 
	 * After the upload, controller will make a query request
	 * itself, and than the viewer could update the cloud file
	 * information.
	 * TODO change to files someday in the future.
	 * @param file(s) name
	 * */
	public void upload(String filename, long offset)
	{
		synchronized (uploadFlag)
		{
			synchronized (uploadFileName)
			{
				uploadFileName.add(filename);
			}
			synchronized (uploadOffset)
			{
				uploadOffset.add(Long.valueOf(offset));
			}
			synchronized (requested)
			{
				requested.add(Boolean.valueOf(false));
			}
			uploadFlag.setValue(true);
		}
	}
	
	
	boolean keepRunning = true;
	Flag queryFlag = null;
	Flag transferFlag = null;
	ArrayList<DataChannel> channels = new ArrayList<DataChannel>();		//the list of channel
	Queue<TransferTask> tasks = new LinkedList<TransferTask>();			//the queue of transfer tasks
	ArrayList<TransferTask> bufferedTasks = new ArrayList<TransferTask>();	//the tasks to be acked
	int streamTag = 0;
	Runnable controllerRunnable = new Runnable()
	{
		/**
		 * The thread body of controller.
		 * All works are done in the time-sharing mechanism,
		 * this thread will check all possible transaction,
		 * if there is any of them, process it via driving 
		 * different models.
		 * */
		@Override
		public void run()
		{
			controlChannel.sentInitInfo();
			while (keepRunning)
			{
				synchronized (queryFlag)
				{
					//process the query transaction
					if (queryFlag.getValue())
					{
						Log.d(Constant.LOG_LEVEL_DEBUG, "handle query");
						controlChannel.query();
						queryFlag.setValue(false);
						
						//find the task that has finished, send the message to server and remove this task
						//from buffer area
						synchronized (tasks)
						{
							for (int i = 0; i < bufferedTasks.size(); i ++)
							{
								if (bufferedTasks.get(i).finished())
								{
									controlChannel.sendTransferFinishInfo(bufferedTasks.get(i).getFileName());
									bufferedTasks.remove(i);
								}
							}
						}
					}
				}
				
				synchronized (exitFlag)
				{
					//stop controller's thread, control channel's thread, stop
					//all transfer in data channels.
					if (exitFlag.getValue())
					{
						Log.d(Constant.LOG_LEVEL_DEBUG, "realy exit...");
						controlChannel.exit();
						exitFlag.setValue(false);
						keepRunning = false;
						for (int i = 0; i < channels.size(); i ++)
						{
							channels.get(i).stopMe();
							channels.remove(i);
						}
					}
				}
				
				synchronized (uploadFlag)
				{
					//process the upload transaction
					if (uploadFlag.getValue())
					{
						int index = -1;
						synchronized (requested)
						{
							for (int i = 0; i < requested.size(); i ++)
							{
								if (!requested.get(i).booleanValue())
								{
									index = i;
									break;
								}
							}
						}
						
						synchronized (uploadFileName)
						{
							if ((0 <= index) && (index < uploadFileName.size()))
							{
								if (!controlChannel.upload(uploadFileName.get(index)))
								{
									Message msg = Message.obtain();
									JSONObject err = new JSONObject();
									try
									{
										err.put("type", "error");
										err.put("status", "Send upload request failed!");
										msg.obj = err.toString();
										synchronized (viewerHandler)
										{
											viewerHandler.sendMessage(msg);
										}
									}
									catch (JSONException e)
									{
										if (e.getMessage() != null)
											Log.e(Constant.LOG_LEVEL_ERROR, "at Controller upload : " + e.getMessage());
										else
											Log.e(Constant.LOG_LEVEL_ERROR, "at Controller upload : JSONException");
									}
								}
								else
								{
									synchronized (requested)
									{
										requested.set(index, Boolean.valueOf(true));
									}
								}
							}
						}
						
						synchronized (requested)
						{
							if ((index < 0) || (requested.size() <= index))
							{
								uploadFlag.setValue(false);
							}
						}
					}
				}
				
				synchronized (transferFlag)
				{
					//process the transfer transaction, this transaction won't be
					//ordered by user, it is a part of our file transfer protocol.
					if (transferFlag.getValue())
					{
						try
						{
							synchronized (tasks)
							{
								if (!tasks.isEmpty())
								{
									DataChannel channel = null;
									//new a channel if the pool is not full
									if (channels.size() < Constant.CHANNEL_POOL_SIZE)
									{
										Socket socket = new Socket(Setting.SERVER_IP, tasks.peek().transferPort());	//TO-DO validate this is the right value?
										channel = new DataChannel(socket);
									}
									//otherwise search for a free channel
									else
									{
										for (int i = 0; i < channels.size(); i ++)
										{
											if (channels.get(i).free())
											{
												channels.remove(i);
												Socket socket = new Socket(Setting.SERVER_IP, tasks.peek().transferPort());	//TO-DO validate this is the right value?
												channel = new DataChannel(socket);
												break;
											}
										}
									}
									//otherwise, do nothing
									
									if (channel != null)
									{
										switch (tasks.peek().mode())
										{
										case Constant.TRANSFER_MODE_UPLOAD:
											channel.upload(tasks.peek().nextBlock(streamTag));
											streamTag ++;
											channels.add(channel);
											break;
										case Constant.TRANSFER_MODE_DOWNLOAD:
											
											break;
										case Constant.TRANSFER_MODE_UPDATE_UP:
											
											break;
										case Constant.TRANSFER_MODE_UPDATE_DOWN:
											
											break;
										default:
											break;
										}
										
										if (!tasks.peek().hasNextBlock())
										{
											bufferedTasks.add(tasks.poll());
										}
									}
								}
								
								if (tasks.isEmpty())
								{
									transferFlag.setValue(false);
								}
							}
						}
						catch (UnknownHostException e)
						{
							if (e.getMessage() != null)
								Log.e(Constant.LOG_LEVEL_ERROR, "at Controlle transfer : " + e.getMessage());
							else
								Log.e(Constant.LOG_LEVEL_ERROR, "at Controlle transfer : UnknownHostException");
						}
						catch (IOException e)
						{
							if (e.getMessage() != null)
								Log.e(Constant.LOG_LEVEL_ERROR, "at Controlle transfer : " + e.getMessage());
							else
								Log.e(Constant.LOG_LEVEL_ERROR, "at Controlle transfer : IOException");
						}
					}
				}
			}
		}
	};
	
	@SuppressLint("HandlerLeak")
	Handler myHandler = new Handler()
	{
		/**
		 * Controller's handler, it will receive the message from
		 * models, and process them, or transmit them to viewer.
		 * */
		public void handleMessage(Message msg)
		{
			try
			{
				JSONObject info = new JSONObject((String) msg.obj);
				Log.d(Constant.LOG_LEVEL_DEBUG, "receive response at controller :" + info.toString());
				String type = info.getString("type");
				
				//query response from server, update local database
				//and transmit it to viewer.
				if (type.equals("query"))
				{
					Message outMSG = Message.obtain();
					outMSG.obj = msg.obj;
					synchronized (viewerHandler)
					{
						viewerHandler.sendMessage(outMSG);
					}
					
					try
					{
						//TODO update local database properly
						JSONArray content = info.getJSONArray("content");
						for (int i = 0; i < content.length(); i ++)
						{
							JSONObject item = content.getJSONObject(i);
							
							List<FileEntry> files = dbManager.query();
							for (int j = 0; j < files.size(); j ++)
							{
								FileEntry tmp = files.get(j);
								if (tmp.cloudPath.equals(item.getString("path")))
								{
									if (tmp.localDigest.equals(item.getString("digest")))
									{
										FileEntry file = new FileEntry(tmp.id, item.getString("path"), item.getString("digest"), 
													item.getInt("modtime"), Constant.FILE_STATUS_SYNCED, item.getLong("size"), 
													tmp.localPath, tmp.localModTime, tmp.localSize, tmp.localDigest);
										dbManager.update(file);
									}
									else
									{
										//TODO do something
									}
									break;
								}
							}
						}
						
					}
					catch (JSONException e)
					{
						if (e.getMessage() != null)
							Log.e(Constant.LOG_LEVEL_ERROR, "at Controller query : " + e.getMessage());
						else
							Log.e(Constant.LOG_LEVEL_ERROR, "at Controller query : JSONException");
					}
				}
				else
				{
					//upload response from server
					if (type.equals("upload"))
					{
						//keep uploading
						if (info.getString("action").equals("upload"))
						{
							int port = info.getInt("port");
							
							synchronized (transferFlag)
							{
								transferFlag.setValue(true);
								//TODO make sure copy of ref?
								//now work file, but really? need to do a fully test.
								synchronized (tasks)
								{
									String filename = info.getString("filename");
									File file = new File(filename);
									int index = -1;
									synchronized (uploadFileName)
									{
										for (int i = 0; i < uploadFileName.size(); i ++)
										{
											if (uploadFileName.get(i).equals(filename))
											{
												index = i;
												uploadFileName.remove(i);
												break;
											}
										}
									}
																		
									synchronized (uploadOffset)
									{
										if ((0 <= index) && (index < uploadOffset.size()))
										{
											tasks.offer(new TransferTask(file, uploadOffset.get(index), port, Constant.TRANSFER_MODE_UPLOAD));
											uploadOffset.remove(index);
											synchronized (requested)
											{
												requested.remove(index);
											}
										}
									}
								}
								Log.d(Constant.LOG_LEVEL_DEBUG, "prepare to transfer");
							}
						}
						else
						{
							//there is a copy of my file on cloud, we needn't upload
							if (info.getString("action").equals("success"))
							{
								Message outMSG = Message.obtain();
								JSONObject msgJsonObject = new JSONObject();
								msgJsonObject.put("type", "upload");
								msgJsonObject.put("status", "success");
								outMSG.obj = msgJsonObject.toString();
								synchronized (viewerHandler)
								{
									viewerHandler.sendMessage(outMSG);
								}
							}
						}
					}
					else
					{
						if (type.equals("download"))
						{
							
						}
						else
						{
							if (type.equals("update"))
							{
								
							}
							else
							{
								if (type.equals("ack"))
								{
									//TO-DO process ack
									synchronized (tasks)
									{
										if (!tasks.isEmpty())
										{
											tasks.peek().ack(info.getInt("tag"));
										}
										
										//a task will be moved to buffer area immediately after
										//it's last block is transferred, so after ack, only task
										//in buffer area may be finished.
									}
									synchronized (bufferedTasks)
									{
										for (int i = 0; i < bufferedTasks.size(); i ++)
										{
											bufferedTasks.get(i).ack(info.getInt("tag"));
											
											if (bufferedTasks.get(i).finished())
											{
												Message outMSG = Message.obtain();
												JSONObject msgJsonObject = new JSONObject();
												
												switch (bufferedTasks.get(i).mode())
												{
												case Constant.TRANSFER_MODE_UPLOAD:
													msgJsonObject.put("type", "upload");
													break;
												case Constant.TRANSFER_MODE_DOWNLOAD:
													msgJsonObject.put("type", "download");
													break;
												case Constant.TRANSFER_MODE_UPDATE_UP:
													msgJsonObject.put("type", "upload");
													break;
												case Constant.TRANSFER_MODE_UPDATE_DOWN:
													msgJsonObject.put("type", "download");
													break;
												default:
													break;
												}
												
												msgJsonObject.put("status", "success");
												outMSG.obj = msgJsonObject.toString();
												synchronized (viewerHandler)
												{
													viewerHandler.sendMessage(outMSG);
												}
												//bufferedTasks.remove(i);
												query();
											}
										}
									}
									Log.d(Constant.LOG_LEVEL_DEBUG, "receive ack : tag = " + info.getInt("tag"));
								}
								else
								{
									//nothing
								}
							}
						}
					}
				}
			}
			catch (JSONException e)
			{
				if (e.getMessage() != null)
					Log.e(Constant.LOG_LEVEL_ERROR, "at MainActivity Handler : " + e.getMessage());
				else
					Log.e(Constant.LOG_LEVEL_ERROR, "at MainActivity Handler : JSONException");
			}
		}		
	};
	
	
	private Controller()
	{
		queryFlag = new Flag(false);
		uploadFlag = new Flag(false);
		transferFlag = new Flag(false);
		exitFlag = new Flag(false);
	}
}
