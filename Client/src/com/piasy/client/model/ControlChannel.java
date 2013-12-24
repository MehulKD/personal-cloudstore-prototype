package com.piasy.client.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import com.piasy.client.controller.Controller;
import com.piasy.client.dao.DBManager;
import com.piasy.client.dao.FileEntry;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * <p>This class is the control channel between server 
 * and client, transmit the control command and response.</p>
 *  
 * <p>The basic assumption is that the Socket provide the
 * fully reliable service. So the complex accuracy transmitted
 * through the Socket won't be validated in this prototype.
 * But that doesn't mean we assume that the TCP connection has
 * no delay or packet loss, these things are handled by TODO setting
 * the Socket timeout threshold, and TODO check exceptions.</p>
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:44 2013/12/17</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class ControlChannel
{
	
	String name;
	Socket socket = null;
	BufferedReader is = null;
	PrintWriter os = null;
	Handler controllerHandler = null;
	Controller myController = null;
	
	/**
	 * The public constructor client name.
	 * @param the name of this client
	 * */
	public ControlChannel(String name, Handler controllerHandler)
	{
		this.name = name;
		this.controllerHandler = controllerHandler;
		myController = Controller.getController();
	}
	
	/**
	 * Initialize the `socket`, `is` and `os`
	 * */
	public boolean init()
	{
		boolean ret = false;
		try
		{
			socket = new Socket(Setting.SERVER_IP, Setting.SERVER_PORT);
			is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			os = new PrintWriter(socket.getOutputStream());
			
			Thread listeningThread = new Thread(listenRunnable);
			listeningThread.start();
			
			ret = true;
		}
		catch (UnknownHostException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel init : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel init : UnknownHostException");
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel init : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel init : IOException");
		}
		return ret;
	}
	
	/**
	 * Send the client name as his identifier to server.
	 * */
	public void sentInitInfo()
	{
		try
		{
			JSONObject initInfo = new JSONObject();
			initInfo.put("type", "init");
			initInfo.put("name", name);
			
			os.println(initInfo.toString());
			os.flush();
			
			Log.i(Constant.LOG_LEVEL_INFO, "send init info : " + initInfo.toString());
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel init : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel init : JSONException");
		}
	}
	
	/**
	 * Send query request to server, and receive the response from server.
	 * We assume that the Socket provide totally credible transmission.
	 * If the Socket has exception, null will be returned.
	 * */
	public void query()
	{
		JSONObject queryInfo = new JSONObject();
		
		try
		{
			queryInfo.put("type", "query");
			
			os.println(queryInfo.toString());
			os.flush();
			
			Log.i(Constant.LOG_LEVEL_INFO, "send query request : " + queryInfo.toString());
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel query : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel query : JSONException");
		}
	}
	
	/**
	 * When all blocks of a file(task) have been acknowledged,
	 * client will notify server to close the data channel, and
	 * stop the listening thread.
	 * @param the finished file name.
	 * */
	public void sendTransferFinishInfo(String filename)
	{
		JSONObject queryInfo = new JSONObject();
		
		try
		{
			queryInfo.put("type", "finish");
			queryInfo.put("filename", filename);
			
			os.println(queryInfo.toString());
			os.flush();
			
			Log.i(Constant.LOG_LEVEL_INFO, "send finish info : " + queryInfo.toString());
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel send finish info : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel send finish info : JSONException");
		}
	}
	
	/**
	 * Send exit message to server. And stop listening to server.
	 * */
	public void exit()
	{
		JSONObject queryInfo = new JSONObject();
		
		try
		{
			queryInfo.put("type", "exit");
			
			os.println(queryInfo.toString());
			os.flush();
			
			keepListening = false;
			is.close();
			os.close();
			socket.close();
			
			Log.i(Constant.LOG_LEVEL_INFO, "send exit request : " + queryInfo.toString());
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel exit : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel exit : JSONException");
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel exit : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel exit : IOException");
		}
	}
	
	/**
	 * Send upload request to server, including the file name, file size,
	 * and digest information, providing for server to decide whether this
	 * file need to be transferred. And add a record to the local database.
	 * @param name of the file to be uploaded.
	 * */
	public boolean upload(String filename)
	{
		boolean ret = false;
		JSONObject uploadInfo = new JSONObject();
		try
		{
			File upFile = new File(filename);
			String digest = IndexModel.getMD5(upFile);
			
			if (digest != null)
			{
				int index = 0;
				for (index = filename.length() - 1; 0 <= index; index --)
					if (filename.charAt(index) == '/')
						break;
				//in server, the file will be stored in the folder named as the user name,
				//and keep the original file name.
				String cloudPath = myController.getName() + "/" + filename.substring(index + 1);
				
				FileEntry entry = new FileEntry(cloudPath, digest, 0, Constant.FILE_STATUS_UPLOAD_PARTLY, 0,
								upFile.getAbsolutePath(), upFile.lastModified(), upFile.length(), digest);
				DBManager dbManager = myController.getDbManager();
				synchronized (dbManager)
				{
					dbManager.insert(entry);
				}
				
				uploadInfo.put("type", "upload");
				uploadInfo.put("filename", upFile.getAbsolutePath());
				uploadInfo.put("filesize", upFile.length());
				uploadInfo.put("digest", digest);
				
				os.println(uploadInfo.toString());
				os.flush();
				
				ret = true;
			}
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel upload : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel upload : JSONException");
		}
		
		return ret;
	}
	
	boolean keepListening = true;
	Runnable listenRunnable = new Runnable()
	{
		/**
		 * The listening thread body. Listening for server's
		 * message. And notify controller to process them.
		 * */
		@Override
		public void run()
		{
			try
			{
				while (keepListening)
				{
					String rcvStr = is.readLine();
					
					//We must make sure that this packet has been sent here correctly.
					if (rcvStr != null)
					{
						//TO-DO parse message here and notify controller
						//just transmit it to controller.
						Log.i(Constant.LOG_LEVEL_INFO, "receive reply : " + rcvStr);
						Message msg = Message.obtain();
						msg.obj = rcvStr;
						controllerHandler.sendMessage(msg);						
					}
					//The receive String is null means that this connection is broken.
					else
					{
						Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel listenRunnable : receive null response");
						keepListening = false;
					}
				}
			}
			catch (IOException e)
			{
				if (e.getMessage() != null)
					Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel listenRunnable : " + e.getMessage());
				else
					Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel listenRunnable : IOException");
			}
		}
	};
	
}
