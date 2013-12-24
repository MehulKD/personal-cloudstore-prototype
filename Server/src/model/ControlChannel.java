package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import util.Loger;
import controller.Controller;


/**
 * <p>This class is the control channel between server 
 * and client, transmit the control command and response.</p>
 *  
 * <p>The basic assumption is that the Socket provide the
 * fully reliable service. So the complex accuracy transmitted
 * through the Socket won't be validated in this prototype.
 * But that doesn't mean we assume that the TCP connection has
 * no delay or packet loss, these things are handled by TODO setting
 * the Socket timeout threshold.</p>
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:44 2013/12/17</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class ControlChannel
{

	Socket controlSocket = null;
	BufferedReader is = null;
	PrintWriter os = null;
	Controller myController = null;
	int localPort;
	
	/**
	 * The public constructor, setting the control channel socket,
	 * which is got by accept a request from client, and the controller.
	 * @param 
	 * control channel socket,
	 * the reference of controller
	 * */
	public ControlChannel(Socket controlSocket, Controller myController)
	{
		this.controlSocket = controlSocket;
		localPort = controlSocket.getLocalPort();
		this.myController = myController;
	}
	
	/**
	 * Initialize the `is` and `os`, and get the client name
	 * as his identifier.
	 * */
	public boolean init()
	{
		boolean ret = false;
		try
		{
			is = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
			os = new PrintWriter(controlSocket.getOutputStream());
			
			String rcvStr = is.readLine();
			
			//We assume that once a packet has been received, it is accurate.
			/*boolean legalMSG = rcvInfo != null && rcvInfo.has("type") && 
					   		   rcvInfo.getString("type").equals("init") &&
					   		   rcvInfo.has("name") && rcvInfo.getString("name") != null;
			
			if (legalMSG)
			{
				myController.assignName(rcvInfo.getString("name"));
				ret = true;
			}
			else
			{
				Loger.Log(Constant.LOG_LEVEL_WARNING, "Receive illegal message from client: " + rcvStr, "ControlChannel init");
			}*/
			
			//But we must make sure that this packet has been sent here correctly.
			if (rcvStr != null)
			{
				Loger.Log(Constant.LOG_LEVEL_INFO, "Receive init message from client: " + 
						rcvStr, "ControlChannel parseMessage " + localPort);
				
				JSONObject rcvInfo = new JSONObject(rcvStr);
				myController.assignName(rcvInfo.getString("name"));
				ret = true;
			}
			//The receive String is null means that this connection is broken,
			//because client will sent all messages in one line at one time.
			else
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, "Receive null message from client", 
						"ControlChannel listenRunnable " + localPort);
			}
		}
		catch (IOException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "ControlChannel init");
		}
		catch (JSONException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "ControlChannel init");
		}
		return ret;
	}
	
	/**
	 * The method to send response of one request to client,
	 * all the messages are in json format. More information
	 * about the format is available in the wiki page 
	 * {Server And Client Interface In Control Channel}. 
	 * */
	public void sendResponse(JSONObject response)
	{
		synchronized (os)
		{
			os.println(response.toString());
			os.flush();
			Loger.Log(Constant.LOG_LEVEL_INFO, "send response to client: " + 
					  response.toString(), "ControlChannel sendResponse " + localPort);
		}
	}
	
	Thread listenThread = null;
	/**
	 * The method to begin listening client's request, in a new thread.
	 * */
	public void listen()
	{
		if (!keepListening || listenThread == null)
		{
			listenThread = new Thread(listenRunnable);
			listenThread.start();
		}
	}
	
	/**
	 * The method to stop listening to client.
	 * */
	public void stopListen()
	{
		keepListening = false;
	}

	boolean keepListening = true;
	Runnable listenRunnable = new Runnable()
	{
		
		/**
		 * The listening thread body, receive messages from
		 * client, and parse them, and send these requests 
		 * to controller to handle them.
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
						parseMessage(rcvStr);
					//The receive String is null means that this connection is broken,
					//because client will sent all messages in one line at one time.
					else
					{
						Loger.Log(Constant.LOG_LEVEL_ERROR, "Receive null message from client", 
								"ControlChannel listenRunnable " + localPort);
						keepListening = false;
					}
				}
			}
			catch (IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "ControlChannel listenRunnable " + localPort);
			}
		}
	};
	
	/**
	 * Parse the messages(requests) received from client, and ask controller
	 * to handle them.
	 * */
	protected void parseMessage(String message)
	{
		Loger.Log(Constant.LOG_LEVEL_INFO, "Receive message from client: " + 
				  message, "ControlChannel parseMessage " + localPort);
		try
		{
			JSONObject request = new JSONObject(message);
			String type = request.getString("type");
			
			//query for his files information in cloud center.
			if (type.equals("query"))
			{
				myController.query();
			}
			else
			{
				if (type.equals("upload"))
				{
					myController.upload(request.getString("filename"), request.getString("digest"));
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
							if (type.equals("exit"))
							{
								keepListening = false;
								is.close();
								os.close();
								controlSocket.close();
								myController.exit();
							}
							else
							{
								if (type.equals("finish"))
								{
									myController.finishFileTransfer(request.getString("filename"));
								}
								else
								{
									
								}
							}
						}
					}
				}
			}
		}
		catch (JSONException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "ControlChannel parseMessage " + localPort);
		}
		catch (IOException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "ControlChannel parseMessage " + localPort);
		}
	}
}
