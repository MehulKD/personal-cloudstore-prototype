package com.piasy.client.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.piasy.client.model.Config;
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
	public boolean init(Handler viewerHandler, String username)
	{
		boolean ret = true;
		this.username = username;
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
	 * "filename" : filename
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
			e.printStackTrace();
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
				info.put("type", "download");
				break;
			case Constant.TRANSFER_MODE_UPLOAD:
				info.put("type", "upload");
				break;
			default:
				break;
			}
			info.put("filename", filename);
			info.put("status", "success");
			Message msg = Message.obtain();
			msg.obj = info.toString();
			System.out.println("Controller.transferFileFinish() " + info.toString());
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
							try
							{
								TransferTask task = new TransferTask(request.type, 
										request.params.getString("filename"), dataport);
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
								e.printStackTrace();
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
		
	public Config getConf(long size)
	{
		//TODO init index model according to
		//network speed and split time/efficiency
		return new Config(Constant.CDC_MAGIC, Constant.CDC_MASK, 
				Constant.MIN_BLOCK_SIZE, Constant.MAX_BLOCK_SIZE, Constant.SLID_WINDOW_STEP);
	}
	
	private Controller()
	{
	}
}
