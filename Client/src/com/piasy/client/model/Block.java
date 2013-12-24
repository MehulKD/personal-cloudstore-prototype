package com.piasy.client.model;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

/**
 * <p>The class that encapsulates the access to file.
 * It is the basic unit transferred in data channel.
 * It records its own information, including tag, size, 
 * whether has been acknowledged.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:54 2013/12/22</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class Block
{
	RandomAccessFile filePointer;
	int readSize = 0;
	int size = 0;
	int tag = -1;
	boolean acked = false;
	
	/**
	 * Constructor, the basic information: file pointer, block size
	 * must be assigned.
	 * @param file pointer
	 * @param bolck size
	 * */
	public Block(RandomAccessFile filePointer, int size)
	{
		this.filePointer = filePointer;
		this.size = size;
	}
	
	/**
	 * Set the stream tag when this block is allocated
	 * to a data channel.
	 * @param stream tag
	 * */
	public void setTag(int tag)
	{
		this.tag = tag;
	}
	
	/**
	 * Acknowledge block with the specific tag.
	 * If this tag differs from this block's own
	 * tag, it won't do any thing.
	 * @param stream tag
	 * */
	public boolean ack(int tag)
	{
		boolean ret = false;
		if (this.tag == tag)
		{
			acked = true;
			ret = true;
		}
		return ret;
	}
	
	/**
	 * Get the block's size
	 * @return block size
	 * */
	public int size()
	{
		return size;
	}
	
	/**
	 * Get the block's stream tag.
	 * @return stream tag
	 * */
	public int tag()
	{
		return tag;
	}
	
	/**
	 * Get the next buffer to be sent.
	 * If it has already get all the block data,
	 * it won't read file any more.
	 * The read size in byte will be returned, or
	 * -1 if this block has finished.
	 * @param buffer area
	 * @param buffer size
	 * @param the really read size in byte, -1 if all data has been read.
	 * */
	public int nextBuf(byte[] buf, int bufSize)
	{
		int read = -1;
		try
		{
			if (readSize < Constant.BLOCK_SIZE)
			{
				read = filePointer.read(buf, 0, bufSize);
				if (read != -1)
				{
					readSize += read;
				}
			}
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at Block nextBuf : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at Block nextBuf : IOException");
		}
		return read;
	}
	
	/**
	 * Query whether this block has been acknowledged.
	 * @return whether this block has been acknowledged
	 * */
	public boolean acked()
	{
		return acked;
	}
}
