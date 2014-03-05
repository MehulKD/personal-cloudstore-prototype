package controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import model.Block;
import model.Constant;
import model.ControlChannel;
import model.DataChannel;
import model.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.Loger;
import dao.DBManager;
import dao.FileEntry;

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
	DataChannel dataChannel;
	String username = null;
	DBManager dbManager;
	Queue<Request> requests = new LinkedList<Request>();
	
	/**
	 * Public constructor.
	 * @param the control channel socket
	 * */
	public Controller(Socket controlSocket)
	{
		this.controlSocket = controlSocket;
		dbManager = new DBManager();
	}
	
	/**
	 * The query transaction.
	 * It won't be handle here. We just note down that we have
	 * a query transaction, and it will be handled in controller's
	 * own thread or thread of models.
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
	 * The upload transaction.
	 * It won't be handle here. We just note down that we have
	 * a upload transaction, and it will be handled in controller's
	 * own thread or thread of models.
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
	
	String downloadFileName = null;	//TODO the local absolute path on client of this file, change to queue?
	/**
	 * The upload transaction.
	 * It won't be handle here. We just note down that we have
	 * a upload transaction, and it will be handled in controller's
	 * own thread or thread of models.
	 * */
	public void download(String filename)
	{
		synchronized (requests)
		{
			Request download = new Request();
			download.type = Constant.REQUEST_TYPE_DOWNLOAD;
			requests.offer(download);
		}
	}
	
	int tag;
	/**
	 * The ack transaction.
	 * It won't be handle here. We just note down that we have
	 * a ack transaction, and it will be handled in controller's
	 * own thread or thread of models.
	 * */
	public void ack(long streamSeq)
	{
		synchronized (requests)
		{
			Request ack = new Request();
			ack.type = Constant.REQUEST_TYPE_ACK;
			JSONObject params = new JSONObject();
			try
			{
				params.put("streamSeq", streamSeq);
				requests.offer(ack);
			}
			catch (JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "controller ack");
			}
		}
	}
	
	public void updateFile(ArrayList<Block> blocks, String filename)
	{
		dbManager.updateFile(blocks, filename, username);
	}
	
	public void addBlock(JSONObject block, String filename)
	{
		dbManager.addBlock(block, filename, username);
	}
	
	public void addFile(JSONObject params)
	{
		dbManager.addFile(params, username);
	}
	
	public void addBlockToFile(JSONObject block, String filename)
	{
		dbManager.addBlockToFile(block, filename, username);
	}
	
	public boolean hasBlock(String hash)
	{
		return dbManager.hasBlock(hash);
	}
	
	/**
	 * Assign the client's name, as his identifier.
	 * @param client's user name, it must be unique.
	 * */
	public void assignName(String username)
	{
		this.username = username;
	}
	
	public String getUserName()
	{
		return username;
	}
	
	public JSONObject getFileInfo(String filename)
	{
		return dbManager.getFileInfo(filename, username);
	}
	
	/**
	 * Exit method, stop controller's thread, (control channel has stoped
	 * itself before call this method), stop all data channels.
	 * */
	public void exit()
	{
		keepRuning = false;
	}
	
	
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
		try
		{
			//create control channel
			ServerSocket dataSocket = new ServerSocket(0);
			dataChannel = new DataChannel(dataSocket, this);
			dataChannel.start();
			controlChannel = new ControlChannel(controlSocket, this, dataSocket.getLocalPort());
			if (!controlChannel.init() || !dbManager.init())
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, "Control channel init fail!", "controller for " + username);
				return;
			}
			
			controlChannel.listen();
		}
		catch (IOException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "controller run");
		}
		
		while(keepRuning)
		{
			try
			{
				synchronized (requests)
				{
					if (!requests.isEmpty())
					{
						Request request = requests.poll();
						switch (request.type)
						{
						case Constant.REQUEST_TYPE_QUERY:
							List<FileEntry> files = dbManager.getAllFiles(username);
							JSONObject queryResponse = new JSONObject();
							queryResponse.put("type", "query");
							JSONArray content = new JSONArray();
							for (FileEntry file : files)
							{
								JSONObject entry = new JSONObject();
								entry.put("filename", file.filename);
								entry.put("size", file.size);
								content.put(entry);
							}
							queryResponse.put("content", content);
							controlChannel.sendResponse(queryResponse);
							break;
						case Constant.REQUEST_TYPE_UPLOAD:
							
							break;	
						case Constant.REQUEST_TYPE_DOWNLOAD:
							
							break;
						case Constant.REQUEST_TYPE_UPDATE_UP:
							
							break;
						case Constant.REQUEST_TYPE_UPDATE_DOWN:
							
							break;
						case Constant.REQUEST_TYPE_ACK:
							
							break;
						default:
							break;
						}
					}
				}
			}
			catch (JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "controller handle requests");
			}
		}
	}
	
}
