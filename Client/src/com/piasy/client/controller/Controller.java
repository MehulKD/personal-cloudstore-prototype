package com.piasy.client.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.piasy.client.dao.DBManager;
import com.piasy.client.model.Constant;
import com.piasy.client.model.ControlChannel;
import com.piasy.client.model.Request;
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
	
	static Context context;
	public static void setContext(Context cont)
	{
		context = cont;
	}
	
	public static void makeToast(String text)
	{
		Toast.makeText(context, text, Toast.LENGTH_LONG).show();
	}
	
	ControlChannel controlChannel = null;
	String username = null;
	Handler viewerHandler = null;
	Thread controllerThread = null;
	DBManager dbManager = null;
	Queue<Request> requests = new LinkedList<Request>();
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
	public boolean init(Handler viewerHandler, DBManager dbManager, String username)
	{
		boolean ret = true;
		this.username = username;
		this.dbManager = dbManager;
		this.viewerHandler = viewerHandler;
		
		File cloudDir = new File(Constant.APP_BASE_DIR);
		cloudDir.mkdir();
		File logDir = new File(Constant.APP_BASE_DIR + "/log");
		logDir.mkdir();
		File tmpDir = new File(Constant.TMP_DIR);
		tmpDir.mkdir();
				
		if (ret)
		{
			controllerThread = new Thread(controllerRunnable);
			controllerThread.start();
			
			if (this.dbManager != null && !this.dbManager.dbIsOpen())
			{
				this.dbManager.openDB();
			}
			
			
//			List<BlockEntry> entries = dbManager.queryFromBlocks();
//			//TO-DO for each filename, only re-request once...
//			ArrayList<BlockEntry> reTransferBlocks = new ArrayList<BlockEntry>();
//			for (int i = 0; i < entries.size(); i ++)
//			{
//				BlockEntry entry = entries.get(i);
//				boolean unacked = (entry.tag == Constant.BLOCK_UPLOAD_UNACKED) ||
//							   (entry.tag == Constant.BLOCK_DOWNLOAD_UNACKED) ||
//							   (entry.tag == Constant.BLOCK_UPDATE_UP_UNACKED) ||
//							   (entry.tag == Constant.BLOCK_UPDATE_DOWN_UNACKED);
//				if (unacked)
//				{
//					boolean exist = false;
//					int j = 0;
//					for (j = 0; j < reTransferBlocks.size(); j ++)
//					{
//						if (entry.filename.equals(reTransferBlocks.get(j).filename))
//						{
//							exist = true;
//							break;
//						}
//					}
//					
//					if (!exist)
//					{
//						reTransferBlocks.add(entry);
//					}
//					else
//					{
//						if (entry.tag < reTransferBlocks.get(j).tag)
//						{
//							reTransferBlocks.set(j, entry);
//						}
//					}
//				}
//			}
//			
//			for (int i = 0; i < reTransferBlocks.size(); i ++)
//			{
//				BlockEntry entry = reTransferBlocks.get(i);
//				
//				switch ((int) entry.status)
//				{
//				case Constant.BLOCK_UPLOAD_UNACKED:
//					upload(entry.filename, entry.offset);
//					break;
//				case Constant.BLOCK_DOWNLOAD_UNACKED:
//					//TODO keep download again
//					break;
//				case Constant.BLOCK_UPDATE_UP_UNACKED:
//					//TODO keep update up again
//					break;
//				case Constant.BLOCK_UPDATE_DOWN_UNACKED:
//					//TODO keep update down again
//					break;
//				default:
//					break;
//				}
//			}
		}
		return ret;
	}
	
	/**
	 * The exit method, it will be called when user want to
	 * exit this application. We also only note down this 
	 * request here, it will be processed in controller's 
	 * thread.
	 * */
	public void exit()
	{
		synchronized (requests)
		{
			dbManager.closeDB();
			Request exit = new Request();
			exit.type = Constant.REQUEST_TYPE_EXIT;
			requests.offer(exit);
		}
	}
	
	/**
	 * Get the username.
	 * @return the user name.
	 * */
	public String getName()
	{
		return username;
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
		synchronized (requests)
		{
			Request query = new Request();
			query.type = Constant.REQUEST_TYPE_QUERY;
			requests.offer(query);
		}
	}
	
	/**
	 * The upload method, just note down that the user has made
	 * a upload request, and save the file(s) name to be sent,
	 * and it will be processed in the controller's own thread. 
	 * After the upload, controller will make a query request
	 * itself, and than the viewer could update the cloud file
	 * information. If there are many files to upload, call this
	 * method one by one.
	 * @param params : upload params
	 * {
	 * "files" : [
	 * 				{
	 * 				"filename" : filename
	 * 				} ...
	 * 		     ]
	 * }
	 * */
	public void upload(JSONObject params)
	{
		synchronized (requests)
		{
			Request upload = new Request();
			upload.type = Constant.REQUEST_TYPE_UPLOAD;
			upload.params = params;
			requests.offer(upload);
		}
	}

	public void pureUpload(JSONObject params)
	{
		synchronized (requests)
		{
			Request upload = new Request();
			upload.type = Constant.REQUEST_TYPE_PURE_UPLOAD;
			upload.params = params;
			requests.offer(upload);
		}
	}
	
	public void pureDownload(JSONObject params)
	{
		synchronized (requests)
		{
			Request upload = new Request();
			upload.type = Constant.REQUEST_TYPE_PURE_DOWNLOAD;
			upload.params = params;
			requests.offer(upload);
		}
	}
	
	public void updateUp(JSONObject params)
	{
		synchronized (requests)
		{
			Request upload = new Request();
			upload.type = Constant.REQUEST_TYPE_UPDATE_UP;
			upload.params = params;
			requests.offer(upload);
		}
	}
	
	public void updateUpFast(JSONObject params)
	{
		synchronized (requests)
		{
			Request upload = new Request();
			upload.type = Constant.REQUEST_TYPE_UPDATE_UP_FAST;
			upload.params = params;
			requests.offer(upload);
		}
	}
	
	public void updateDown(JSONObject params)
	{
		synchronized (requests)
		{
			Request upload = new Request();
			upload.type = Constant.REQUEST_TYPE_UPDATE_DOWN;
			upload.params = params;
			requests.offer(upload);
		}
	}
	
	/**
	 * The download method, just note down that the user has made
	 * a download request, and save the file(s) name to be sent,
	 * and it will be processed in the controller's own thread. 
	 * If there are many files to download, call this
	 * method one by one.
	 * @param params : download params, the same format as upload
	 * */
	public void download(JSONObject params)
	{
		synchronized (requests)
		{
			Request download = new Request();
			download.type = Constant.REQUEST_TYPE_DOWNLOAD;
			download.params = params;
			requests.offer(download);			
		}
	}
	
	public void parseResponse(String rcvStr)
	{
		try
		{
			JSONObject response = new JSONObject(rcvStr);
			String type = response.getString("type");
			
			if (type.equals("query"))
			{
				Message msg = Message.obtain();
				msg.obj = rcvStr;	//TODO add some information
				synchronized (viewerHandler)
				{
					viewerHandler.sendMessage(msg);
				}
			}
			else
			{
				
			}
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at controller parseResponse : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at controller parseResponse : JSONException");
		}
	}
	
	public void transferFinish()
	{
		synchronized (runningTasks)
		{
			for (int i = 0; i < runningTasks.size(); i ++)
			{
				if (runningTasks.get(i).finished())
				{
					runningTasks.remove(i);
					break;
				}
			}
			
			synchronized (waitingTasks)
			{
				while (!waitingTasks.isEmpty() 
						&& (runningTasks.size() < Constant.CHANNEL_POOL_SIZE))
				{
					TransferTask task = waitingTasks.poll();
					task.start();
					runningTasks.add(task);
				}
			}
		}
	}
	
	public void transferFileFinish(String filename, int mode)
	{
		try
		{
			JSONObject info = new JSONObject();
			switch (mode)
			{
			case Constant.TRANSFER_MODE_DOWNLOAD:
			case Constant.TRANSFER_MODE_UPDATE_DOWN:
			case Constant.TRANSFER_MODE_PURE_DOWNLOAD:
				info.put("type", "download");
				break;
			case Constant.TRANSFER_MODE_UPLOAD:
			case Constant.TRANSFER_MODE_UPDATE_UP:
			case Constant.TRANSFER_MODE_PURE_UPLOAD:
			case Constant.TRANSFER_MODE_UPDATE_UP_FAST:
				info.put("type", "upload");
				break;
			default:
				break;
			}
			info.put("filename", filename);
			info.put("status", "success");
			Message msg = Message.obtain();
			msg.obj = info.toString();
			Log.d(Constant.LOG_LEVEL_DEBUG, info.toString());
			synchronized (viewerHandler)
			{
				viewerHandler.sendMessage(msg);
			}
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at controller transferFileFinish : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at controller transferFileFinish : JSONException");
		}
	}
	
	int dataport;
	public void setDataPort(int port)
	{
		dataport = port;
	}
	
	//the queue of transfer tasks
	Queue<TransferTask> waitingTasks = new LinkedList<TransferTask>();
	ArrayList<TransferTask> runningTasks = new ArrayList<TransferTask>();
	
	boolean keepRunning = true;
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
			controlChannel = new ControlChannel(username);
			if(controlChannel.init())
			{
				Message msg = Message.obtain();
				JSONObject info = new JSONObject();
				try
				{
					info.put("type", "init");
					info.put("status", "success");
					msg.obj = info.toString();
					synchronized (viewerHandler)
					{
						viewerHandler.sendMessage(msg);
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				Message msg = Message.obtain();
				JSONObject info = new JSONObject();
				try
				{
					info.put("type", "init");
					info.put("status", "fail");
					msg.obj = info.toString();
					synchronized (viewerHandler)
					{
						viewerHandler.sendMessage(msg);
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				
				return;
			}
			
			while (keepRunning)
			{
				synchronized (requests)
				{
					if (!requests.isEmpty())
					{
						Request request = requests.poll();
						switch (request.type)
						{
						case Constant.REQUEST_TYPE_QUERY:
							controlChannel.query();
							break;
						case Constant.REQUEST_TYPE_UPLOAD:
						case Constant.REQUEST_TYPE_DOWNLOAD:
						case Constant.REQUEST_TYPE_PURE_DOWNLOAD:
						case Constant.REQUEST_TYPE_UPDATE_UP:
						case Constant.REQUEST_TYPE_UPDATE_DOWN:
						case Constant.REQUEST_TYPE_PURE_UPLOAD:
						case Constant.REQUEST_TYPE_UPDATE_UP_FAST:
							try
							{
								ArrayList<String> files = new ArrayList<String>();
								JSONArray filesInfo = request.params.getJSONArray("files");
								for (int i = 0; i < filesInfo.length(); i ++)
								{
									files.add(filesInfo.getJSONObject(i).getString("filename"));
								}
								//TODO split files into more tasks
								TransferTask task = new TransferTask(request.type, 
										files, false, dataport);
								synchronized (runningTasks)
								{
									if (runningTasks.size() < Constant.CHANNEL_POOL_SIZE)
									{
										task.start();
										runningTasks.add(task);
									}
									else
									{
										synchronized (waitingTasks)
										{
											waitingTasks.offer(task);
										}
									}
								}
							}
							catch (JSONException e)
							{
								if (e.getMessage() != null)
									Log.e(Constant.LOG_LEVEL_ERROR, "at controller run : " + e.getMessage());
								else
									Log.e(Constant.LOG_LEVEL_ERROR, "at controller run : JSONException");
							}
							break;
						case Constant.REQUEST_TYPE_EXIT:
							
							break;
						default:
							break;
						}
					}
				}

			}
		}
	};
		
	private Controller()
	{
	}
}
