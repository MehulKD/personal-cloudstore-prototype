package com.piasy.client.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.piasy.client.controller.Controller;
import com.piasy.client.dao.DBManager;

import android.util.Log;

/**
 * <p>The transfer task class, it takes charge of the transfer of
 * one file, including the splitting and acknowledgment</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:54 2013/12/22</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class TransferTask
{
	File file;
	long baseOffset = 0;
	int transferPort = -1;
	ArrayList<Block> blocks = new ArrayList<Block>();
	int index = 0;
	int blockNum = 0;
	int mode = Constant.TRANSFER_MODE_NONE;
	
	/**
	 * Constructor, assign the file to be transferred, the port server listening, the
	 * transfer mode. The latter two information will be used when a block is allocated
	 * to a data channel.
	 * @param the file to be transferred
	 * @param the port server listening
	 * @param the transfer mode
	 * */
	public TransferTask(File file, long baseOffset, int transferPort, int mode)
	{
		this.file = file;
		this.baseOffset = baseOffset;
		this.transferPort = transferPort;
		this.mode = mode;
		blockNum = (int) Math.ceil((double) file.length() / (double) Constant.BLOCK_SIZE);
		int allocSize = 0;
		
		Log.d(Constant.LOG_LEVEL_DEBUG, "task : " + file.getAbsolutePath() + ", size = " + file.length() + ", blocknum = " + blockNum);
		//split the file to small blocks
		//TODO decide the block size dynamically, using some algorithm.
		for (int i = 0; i < blockNum; i ++)
		{
			try
			{
				RandomAccessFile filePointer = new RandomAccessFile(file, "r");
				//TODO it will influence both size and pointer
				filePointer.seek(i * Constant.BLOCK_SIZE + baseOffset);
				Block block;
				Log.d(Constant.LOG_LEVEL_DEBUG, "file.length() - allocSize = " + (file.length() - allocSize));
				if ((file.length() - allocSize) < Constant.BLOCK_SIZE)
				{
					block = new Block(filePointer, (int) (file.length() - allocSize));
					Log.d(Constant.LOG_LEVEL_DEBUG, "alloc 1 " + block.size());
				}
				else
				{
					block = new Block(filePointer, Constant.BLOCK_SIZE);
					Log.d(Constant.LOG_LEVEL_DEBUG, "alloc 2 " + block.size());
					
				}
				allocSize += block.size();
				blocks.add(block);
			}
			catch (FileNotFoundException e)
			{
				if (e.getMessage() != null)
					Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask : " + e.getMessage());
				else
					Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask : FileNotFoundException");
			}
			catch (IOException e)
			{
				if (e.getMessage() != null)
					Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask : " + e.getMessage());
				else
					Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask : IOException");
			}
		}
	}
	
	/**
	 * Get the server listening port for the transfer of this file, all blocks'
	 * transfer will connect to this port.
	 * @return the server listening port for the transfer of this file
	 * */
	public int transferPort()
	{
		return transferPort;
	}
	
	/**
	 * Get the transfer mode of this file, upload, download, upload update or
	 * download update.
	 * @return the transfer mode of this file
	 * */
	public int mode()
	{
		return mode;
	}
	
	/**
	 * Get the name of this file.
	 * @return the name of this file
	 * */
	public String getFileName()
	{
		return file.getAbsolutePath();
	}
	
	/**
	 * Query that whether there is block remain in this file.
	 * This method should be called after 
	 * {@link com.piasy.client.model.TransferTask.nextBlock}.
	 * @return whether there is block remain in this file
	 * */
	public boolean hasNextBlock()
	{
		return (index < blockNum);
	}
	
	/**
	 * Get the next block in this file to be transferred.
	 * If no block remain, it will return null.
	 * After calling this method, you should call 
	 * {@link com.piasy.client.model.TransferTask.hasNextBlock}.
	 * @return the next block in this file to be transferred
	 * */
	public Block nextBlock(int tag)
	{
		Block block = null;
		if (index < blocks.size())
		{
			block = blocks.get(index);	//TODO reference or copy? if bug appear, here should be the first place to check.
			index ++;
			block.setTag(tag);
		}
		return block;
	}
	
	/**
	 * Query that whether the transferring of this file has finished.
	 * @return whether the transferring of this file has finished
	 * */
	public boolean finished()
	{
		boolean ret = true;
		for (int i = 0; i < blocks.size(); i ++)
		{
			if (!blocks.get(i).acked())
			{
				ret = false;
				break;
			}
		}
		return ret;
	}
	
	/**
	 * Acknowledge the block with the specific stream tag.
	 * @param stream tag of the block to be acknowledged
	 * */
	public void ack(int tag)
	{
		for (int i = 0; i < blocks.size(); i ++)
		{
			if (blocks.get(i).ack(tag))
			{
				DBManager dbManager = Controller.getController().getDbManager();
				synchronized (dbManager)
				{
					//TODO you are too young, too simple, too naive!
				}
			}
		}
	}
	
}
