package model;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import util.Loger;
import controller.Controller;
import dao.DataOperator;
import dao.FileEntry;

/**
 * <p>This is the data channel class. The data channel 
 * transferring files. This file may be split into many
 * blocks, all the transferring of them will connect to
 * the same data channel in server. After one block's transfer
 * has finished, we will acknowledge it to client via controller.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		20:42 2013/12/22</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class DataChannel extends Thread
{

	boolean keepRunning = true;
	ServerSocket dataServerSocket = null;
	int mode = Constant.TRANSFER_MODE_NONE;
	Controller myController = null;
	String filename = null;
	String saveName = null;
	DataOutputStream fullDos = null;
	ArrayList<String> partFiles = new ArrayList<>();
	boolean finished = false;
	
	/**
	 * Constructor, assign the server socket listening to client, 
	 * transfer mode, file name, controller's reference.
	 * */
	public DataChannel(ServerSocket dataServerSocket, int mode, String filename, Controller myController)
	{
		this.dataServerSocket = dataServerSocket;
		this.mode = mode;
		this.filename = filename;
		this.myController = myController;
		
		int index = 0;
		for (index = filename.length() - 1; 0 <= index; index --)
			if (filename.charAt(index) == '/')
				break;
		saveName = myController.getUserName() + "/" + filename.substring(index + 1);
	}
	
	/**
	 * Get the file name transferring.
	 * @return the file name transferring
	 * */
	public String getTransferFileName()
	{
		return filename;
	}
	
	/**
	 * The thread body of data channel.
	 * Create the target file(directory), listening to client.
	 * */
	@Override
	public void run()
	{
		while (keepRunning)
		{
			try
			{
				dataServerSocket.setSoTimeout(Constant.DATA_CHANNEL_ACCEPT_TIMEOUT);
				Socket socket = dataServerSocket.accept();
				TransferThread thread = new TransferThread(socket, mode);
				thread.start();
				Loger.Log(Constant.LOG_LEVEL_INFO, "Accept a client data channel at port " + 
						  socket.getLocalPort(), "DataChannel accept");
			}
			catch (IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel accept");
			}
		}
		
		try
		{
			dataServerSocket.close();
		}
		catch (IOException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel run close");
		}
		Loger.Log(Constant.LOG_LEVEL_INFO, "Data channel for " + filename + " closed.", "DataChannel run");
	}
	
	/**
	 * When the file transfer has finished, 
	 * this thread should be stopped
	 * */
	public void stopMe()
	{
		keepRunning = false;
		merge();
	}
	
	/**
	 * Merge the split file into the target file, stop listening thread,
	 * and update local database. Blocks' tag are in increasing order,
	 * because they connect to server one by one, and the tag increasing
	 * in this procedure.
	 * */
	public boolean merge()
	{
		boolean ret = false;
		File formerPart = new File(saveName + ".part");
		File fullFile = new File(saveName);
		File dir = new File(myController.getUserName());
		dir.mkdir();
		DataInputStream dis;
		FileOutputStream output;
		byte [] bigBuf;
		int read;
		try
		{
			if (formerPart.exists())
			{
				output = new FileOutputStream(formerPart, true);	//set append mode
			}
			else
			{
				fullFile.createNewFile();
				output = new FileOutputStream(fullFile, false);	//ban append mode
			}
			
			synchronized (partFiles)
			{
				for (int i = 0; i < partFiles.size(); i ++)
				{
					//merge finish part file to full file
					File partFile = new File(partFiles.get(i));
					dis = new DataInputStream(new FileInputStream(partFile));
					bigBuf = new byte[Constant.BUFFER_SIZE];
					read = dis.read(bigBuf, 0, Constant.BUFFER_SIZE);
					while (read != -1)
					{
						output.write(bigBuf, 0, read);
						read = dis.read(bigBuf, 0, Constant.BUFFER_SIZE);
					}
					dis.close();
					partFile.delete();
				}
				
				output.close();
			}
			
			if (formerPart.exists())
			{
				if (finished)
				{
					formerPart.renameTo(fullFile);
				}
			}
			else
			{
				if (!finished)
				{
					fullFile.renameTo(formerPart);
				}
			}
			
			if (finished)
			{
				DataOperator dataOperator = new DataOperator();
				switch (mode)
				{
				case Constant.TRANSFER_MODE_UPLOAD:
					FileEntry entry = new FileEntry(myController.getUserName(), saveName, IndexModel.getMD5(fullFile));
					dataOperator.insert(entry);
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
			}
			
			ret = true;
		}
		catch (IOException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel merge");
		}
		
		return ret;
	}
	
	/**
	 * Call this method when receive a finish message from client.
	 * */
	public void finish()
	{
		finished = true;
	}
	
	/**
	 * Send ack to client.
	 * @param the stream tag of this block
	 * */
	protected void ack(int tag)
	{
		//TO-DO ack via controller
		myController.ack(tag);
	}

	/**
	 * The transfer thread of data channel.
	 * */
	class TransferThread extends Thread
	{
		Socket dataSocket = null;
		int mode = Constant.TRANSFER_MODE_NONE;
		
		/**
		 * Assign data socket and transfer mode.
		 * */
		public TransferThread(Socket dataSocket, int mode)
		{
			this.dataSocket = dataSocket;
			this.mode = mode;
		}
		
		/**
		 * Thread body, transfer file.
		 * */
		@Override
		public void run()
		{
			switch (mode)
			{
			case Constant.TRANSFER_MODE_UPLOAD:
				upload();
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
		}
		
		/**
		 * Receive file from client, save it as the part file.
		 * */
		protected void upload()
		{
			File partFile = null;
			try
			{
				DataInputStream dis = new DataInputStream(dataSocket.getInputStream());
				byte [] buf = new byte[Constant.BUFFER_SIZE];
				
				//get tag and size
				int read = dis.read(buf, 0, 8);
				if (read == 8)
				{
					ByteBuffer byteBuffer = ByteBuffer.allocate(8);
					byteBuffer.put(buf, 0, 8);
					int tag = byteBuffer.getInt(0);
					int size = byteBuffer.getInt(4);
					
					Loger.Log(Constant.LOG_LEVEL_INFO, "Data Transfer stream : tag = " + tag + ", size = " + size, "DataChannel TransferThread upload");
					
					partFile = new File(saveName + "." + tag);
					
					Loger.Log(Constant.LOG_LEVEL_INFO, "save file path = " + partFile.getAbsolutePath(), "DataChannel accept");
					
					DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(partFile)));
					int totalRead = 0;
					//TO-DO clear buf or not, needn't!
					read = dis.read(buf, 0, Constant.BUFFER_SIZE);
					while ((read != -1) && keepRunning)
					{
						totalRead += read;
						//TO-DO clear buf or not, needn't!
						//TODO convert buf to file data 
						dos.write(buf, 0, read);
						read = dis.read(buf, 0, Constant.BUFFER_SIZE);
					}
					
					dos.close();
					dis.close();
					dataSocket.close();
					if (totalRead == size)
					{
						ack(tag);
						//this part file will count only when this part has transfer to server fully.
						//if the TCP connection has failed, this block won't count, but it should be delete.
						synchronized (partFiles)	
						{
							partFiles.add(partFile.getAbsolutePath());
						}
					}
					//this condition is the user exit the client app in force,
					//delete the part if its transfer hasn't finished(useless).
					else
					{
						partFile.delete();
					}
				}
			}
			catch (IOException e)
			{
				//if exception(TCP connection has failed) occur, delete the useless part file.
				if ((partFile != null) && partFile.exists())
				{
					partFile.delete();
				}
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread upload");
			}
		}
	}
	
}