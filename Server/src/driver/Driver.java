package driver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import model.Constant;
import model.Setting;
import util.Loger;
import controller.Controller;
import dao.DBManager;

/**
 * <p>This class is the begin point of the server program,
 * it accepts clients' connect request, and instantiate a
 * controller to interact with him and process his other request.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:44 2013/12/17</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class Driver
{

	public static void main(String[] args)
	{
		Loger.Log(Constant.LOG_LEVEL_INFO, "Server start!", "Driver accept");
		
		boolean keepRunning = true;
		try
		{
			ServerSocket server = new ServerSocket(Setting.GATEWAY_PORT);
			Loger.init(System.out, Constant.LEVEL_INFO);
			DBManager dbManager = new DBManager();
			if (dbManager.init())
			{
				//keep listening and accepting clients' connect request.
				while (keepRunning)
				{
					Socket client = server.accept();
					if (client.getLocalPort() != -1 && client.getPort() != 0)
					{
						Loger.Log(Constant.LOG_LEVEL_INFO, "Accept a client at " 
								+ client.getRemoteSocketAddress().toString(), "Driver accept");
						Controller controller = new Controller(client, dbManager);
						controller.start();
					}
				}
				
				server.close();
			}			
		}
		catch (IOException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "Driver create server socket");
		}
	}
}
