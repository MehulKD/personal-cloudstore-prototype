package com.piasy.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.piasy.client.controller.Controller;

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
	Controller myController = null;
	
	/**
	 * The public constructor client name.
	 * @param the name of this client
	 * */
	public ControlChannel(String name)
	{
		this.name = name;
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
			
			sentInitInfo();
			
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
			
			String rcvStr = is.readLine();
			JSONObject response = new JSONObject(rcvStr);
			int dataPort = response.getInt("port");
			myController.setDataPort(dataPort);
			
			Log.i(Constant.LOG_LEVEL_INFO, "send init info : " + initInfo.toString());
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel sentInitInfo : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel sentInitInfo : JSONException");
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel sentInitInfo : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at ControlChannel sentInitInfo : IOException");
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
	public boolean upload(JSONObject uploadInfo)
	{
		boolean ret = false;
		os.println(uploadInfo.toString());
		os.flush();
		ret = true;		
		return ret;
	}
	
	/**
	 * Send download request to server, including the file name, 
	 * @param filename : name of the file to be uploaded, in format
	 * of username/filename
	 * */
	public boolean download(String filename)
	{
		boolean ret = false;
		JSONObject downloadInfo = new JSONObject();
		try
		{
			downloadInfo.put("type", "download");
			downloadInfo.put("filename", filename);
			
			os.println(downloadInfo.toString());
			os.flush();
			
			ret = true;
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
						myController.parseResponse(rcvStr);						
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
