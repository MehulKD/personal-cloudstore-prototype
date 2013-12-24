package controller;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dao.DataOperator;
import dao.FileEntry;
import util.Loger;
import model.Constant;
import model.ControlChannel;
import model.DataChannel;
import model.Flag;

/**
 * <p>This class is the controller class in the MVC design pattern.
 * This class takes charge of responding the viewer's(client through
 * the Internet) request, by driving models to process data, sending
 * process result to viewer, maintaining the top level logic of this
 * software.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:44 2013/12/17</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class Controller extends Thread
{
	
	Socket controlSocket = null;
	boolean keepRuning = true;
	ControlChannel controlChannel = null;
	String name = null;
	DataOperator dataOperator = null;
	
	
	/**
	 * Public constructor.
	 * @param the control channel socket
	 * */
	public Controller(Socket controlSocket)
	{
		this.controlSocket = controlSocket;
		dataOperator = new DataOperator();
		queryFlag = new Flag(false);
		uploadFlag = new Flag(false);
		ackFlag = new Flag(false);
	}
	
	Flag queryFlag = null;
	/**
	 * The query transaction.
	 * It won't be handle here. We just note down that we have
	 * a query transaction, and it will be handled in controller's
	 * own thread or thread of models.
	 * */
	public void query()
	{
		synchronized (queryFlag)
		{
			queryFlag.setValue(true);
		}
	}
	
	Flag uploadFlag = null;
	String uploadFileDigest = null;
	String uploadFileName = null;	//TODO the local absolute path on client of this file
	/**
	 * The upload transaction.
	 * It won't be handle here. We just note down that we have
	 * a upload transaction, and it will be handled in controller's
	 * own thread or thread of models.
	 * */
	public void upload(String filename, String digest)
	{
		synchronized (uploadFlag)
		{
			uploadFileDigest = digest;
			uploadFileName = filename;
			uploadFlag.setValue(true);
		}
	}
	
	int tag;
	Flag ackFlag = null;
	/**
	 * The ack transaction.
	 * It won't be handle here. We just note down that we have
	 * a ack transaction, and it will be handled in controller's
	 * own thread or thread of models.
	 * */
	public void ack(int tag)
	{
		synchronized (ackFlag)
		{
			this.tag = tag;
			ackFlag.setValue(true);
		}
	}
	
	/**
	 * Assign the client's name, as his identifier.
	 * @param client's user name, it must be unique.
	 * */
	public void assignName(String name)
	{
		this.name = name;
	}
	
	public String getUserName()
	{
		return name;
	}
	
	/**
	 * Exit method, stop controller's thread, (control channel has stoped
	 * itself before call this method), stop all data channels.
	 * */
	public void exit()
	{
		keepRuning = false;
		synchronized (dataChannels)
		{
			for (int i = 0; i < dataChannels.size(); i ++)
			{
				dataChannels.get(i).stopMe();
				dataChannels.remove(i);
			}
		}
	}
	
	/**
	 * When receive the message that say one file's transfer has finished,
	 * the data channel transferring it will be closed.
	 * @param the file name transferring finished
	 * */
	public void finishFileTransfer(String filename)
	{
		synchronized (dataChannels)
		{
			for (int i = 0; i < dataChannels.size(); i ++)
			{
				if (filename.equals(dataChannels.get(i).getTransferFileName()))
				{
					dataChannels.get(i).finish();
					dataChannels.get(i).stopMe();
					dataChannels.remove(i);
					break;
				}				
			}
		}
	}
	
	ArrayList<DataChannel> dataChannels = new ArrayList<DataChannel>();
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
		//create control channel
		controlChannel = new ControlChannel(controlSocket, this);
		if (!controlChannel.init())
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, "Control channel init fail!", "controller for " + name);
			return;
		}
		
		controlChannel.listen();
		
		while(keepRuning)
		{
			//response the request made by client (via control channel)
			synchronized (queryFlag)
			{
				//check and process query transaction
				if (queryFlag.getValue())
				{
					List<FileEntry> files = dataOperator.queryBy(Constant.TABLE_COLUMN_OWNER, name);
					JSONObject response = new JSONObject();
					JSONArray content = new JSONArray();
									
					try
					{
						response.put("type", "query");
						for (int i = 0; i < files.size(); i ++)
						{
							FileEntry file = files.get(i);
							File realFile = new File(file.getPath());
							
							//that means this file does not exist, delete this record
							if (realFile.lastModified() == 0)
							{
								dataOperator.delete(file);
							}
							else
							{
								JSONObject item = new JSONObject();
								item.put("path", file.getPath());
								item.put("digest", file.getDigest());
								item.put("modtime", realFile.lastModified());
								item.put("size", realFile.length());
								
								content.put(item);
							}
						}
						
						response.put("content", content);
						controlChannel.sendResponse(response);
					}
					catch (JSONException e)
					{
						Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "controller for " + name + " gen query response");
					}
					
					queryFlag.setValue(false);
				}
			}
			
			synchronized (uploadFlag)
			{
				if (uploadFlag.getValue())
				{
					boolean needUpload = true;
					//TO-DO check local database to decide whether this file should be upload
					List<FileEntry> files = dataOperator.queryBy(Constant.TABLE_COLUMN_DIGEST, uploadFileDigest);
					if (files.size() != 0)
						needUpload = false;
					try
					{
						JSONObject response = new JSONObject();
						response.put("type", "upload");
						if (needUpload)
						{
							response.put("action", "upload");
							response.put("filename", uploadFileName);
							ServerSocket dataServerSocket = new ServerSocket(0);
							DataChannel dataChannel = new DataChannel(dataServerSocket, Constant.TRANSFER_MODE_UPLOAD, uploadFileName, this);
							dataChannel.start();
							synchronized (dataChannels)
							{
								dataChannels.add(dataChannel);
							}
							response.put("port", dataServerSocket.getLocalPort());
						}
						else
						{
							response.put("action", "success");
						}
						
						controlChannel.sendResponse(response);
					}
					catch (JSONException e)
					{
						Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "controller for " + name + " gen upload response");
					}
					catch (IOException e)
					{
						Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "controller for " + name + " gen upload response");
					}
					
					uploadFlag.setValue(false);
				}
			}
			
			synchronized (ackFlag)
			{
				//when a block's transfer has finished, server should acknowledge it
				//to client.
				if (ackFlag.getValue())
				{
					JSONObject response = new JSONObject();
					try
					{
						response.put("type", "ack");
						response.put("tag", tag);
						controlChannel.sendResponse(response);
					}
					catch (JSONException e)
					{
						Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "controller for " + name + " gen ack response");
					}
					ackFlag.setValue(false);
				}
			}
		}
	}
	
}
