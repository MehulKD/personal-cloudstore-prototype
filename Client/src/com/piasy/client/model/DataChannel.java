package com.piasy.client.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

import android.util.Log;

/**
 * <p>This is the data channel class.
 * The data channel transferring files.
 * In current framework, it will only transfer
 * one inner block in one channel, for bind operation,
 * we will design other mechanism later.</p>
 * 
 * <p>One data channel instance only transfer one block
 * in one independent thread.</p>
 * 
 * <p>Due to the limitation of mobile phone's system resources,
 * the maximum number of channels existing at the same time will
 * be limited. But one channel could do both upload and download
 * job, just call the different method, using the channel(socket)
 * assigned at constructor.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		21:58 2013/12/19</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class DataChannel
{
	Block block = null;
	Socket dataSocket = null;
	DataInputStream dis = null;
	DataOutputStream dos = null;
	boolean free = true;
	boolean running = true;
	
	/**
	 * Constructor, assign the socket in this channel.
	 * @param data channel socket
	 * */
	public DataChannel(Socket dataSocket)
	{
		this.dataSocket = dataSocket;
		try
		{
			dis = new DataInputStream(dataSocket.getInputStream());
			dos = new DataOutputStream(dataSocket.getOutputStream());
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at DataChannel init : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at DataChannel init : IOException");
		}
	}
	
	/**
	 * Query that whether this channel is free(the former block's transfer
	 * has finished).
	 * @param whether this channel is free
	 * */
	public boolean free()
	{
		return free;
	}
	
	/**
	 * Upload method, it will create a new thread to upload this block to 
	 * server.
	 * @param the block to be uploaded.
	 * */
	public void upload(Block block)
	{
		free = false;
		this.block = block;
		Thread uploadThread = new Thread(uploadRunnable);
		uploadThread.start();
	}
	
	/**
	 * The stop method, stop the unfinished job when user
	 * want to exit this program.
	 * */
	public void stopMe()
	{
		try
		{
			dis.close();
			dos.close();
			dataSocket.close();
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at DataChannel stopMe : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at DataChannel stopMe : IOException");
		}
		running = false;
	}
	
	Runnable uploadRunnable = new Runnable()
	{
		/**
		 * The thread body of upload thread, just upload all data
		 * in the block.
		 * */
		@Override
		public void run()
		{
			ByteBuffer byteBuffer = ByteBuffer.allocate(8);
			Log.d(Constant.LOG_LEVEL_DEBUG, "tag = " + block.tag() + ", size = " + block.size());
			byteBuffer.putInt(block.tag());
			byteBuffer.putInt(block.size());
			try
			{
				dos.write(byteBuffer.array());
				dos.flush();
				
				
				byte buf[] = new byte[Constant.BUFFER_SIZE];
				//TO-DO synchronize or not? needn't
				int read = block.nextBuf(buf, Constant.BUFFER_SIZE);
				while ((read != -1) && running)
				{
					dos.write(buf, 0, read);
					//TO-DO clear buf or not needn't
					read = block.nextBuf(buf, Constant.BUFFER_SIZE);
				}
				dos.flush();
				close();
			}
			catch (IOException e)
			{
				if (e.getMessage() != null)
					Log.e(Constant.LOG_LEVEL_ERROR, "at DataChannel upload : " + e.getMessage());
				else
					Log.e(Constant.LOG_LEVEL_ERROR, "at DataChannel upload : IOException");
			}
			
			free = true;
		}
	};
	
	/**
	 * Close the socket and streams after the transfer has finished.
	 * */
	protected void close()
	{
		try
		{
			dis.close();
			dos.close();
			dataSocket.close();
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at DataChannel close : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at DataChannel close : IOException");
		}
	}
}
