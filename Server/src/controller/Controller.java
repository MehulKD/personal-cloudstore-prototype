package controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
	public Controller(Socket controlSocket, DBManager dbManager)
	{
		this.controlSocket = controlSocket;
		this.dbManager = dbManager;
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
	 * Add a block into database
	 * */
	public void addBlock(JSONObject block, String filename)
	{
		dbManager.addBlock(block, filename, username);
	}

	/**
	 * Add a file into database
	 * */
	public void addFile(JSONObject params)
	{
		dbManager.addFile(params, username);
	}

	/**
	 * Add a block into a file
	 * */
	public void addBlockToFile(JSONObject block, String filename)
	{
		dbManager.addBlockToFile(block, filename, username);
	}

	/**
     * Query whether there is a block existing in the database
     * */
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

    /**
     * Get info of one file
     * */
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
			if (!controlChannel.init())
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
