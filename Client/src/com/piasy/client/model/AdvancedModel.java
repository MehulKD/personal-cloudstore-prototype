package com.piasy.client.model;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

/**
 * The file advanced process mode class.
 * @deprecated this class shouldn't be used any more 
 * */
public class AdvancedModel
{
	
	RandomAccessFile filePointer;
	long baseOffset;
	
	public AdvancedModel(RandomAccessFile filePointer, long baseOffset)
	{
		this.filePointer = filePointer;
		this.baseOffset = baseOffset;
	}
	
	/**
	 * Initialize the Advanced model, including seek to the
	 * proper file pointer, and validate the arguments.
	 * @return whether the initialization is successful
	 * */
	public boolean init()
	{
		boolean ret = false;
		try
		{
			filePointer.seek(baseOffset);
			ret = true;
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at AdvancedModel init : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at AdvancedModel init : IOException");
		}
		
		return ret;
	}
	
	/**
	 * Get the next buffer, in all blocks.
	 * If reach one block's end, this method will return -1.
	 * The upper level could call 
	 * {@link com.piasy.client.model.AdvancedModel.hasNextBlock()} 
	 * to decide whether it should call this method next time.
	 * @param
	 * buf : the buffer area cleaning before call this method,
	 * bufSize : the maximum size the callee want to get
	 * @return the actual size this method return.
	 * */
	public int nextBuffer(byte buf[], int bufSize)
	{
		int read = -1;
		try
		{
			read = filePointer.read(buf, 0, bufSize);
			if (read != -1)
			{
				//TODO add advanced operation here
			}
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at nextBuffer : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at nextBuffer : IOException");
		}
		return read;
	}
	
	/**
	 * @deprecated This method won't make any sense.
	 * Query whether there is next block in this task.
	 * This method need be called only after the method 
	 * {@link com.piasy.client.model.AdvancedModel.nextBuffer(byte buf[], int bufSize)}
	 *  returns -1.
	 * @return whether there is next block in this task
	 * */
	public boolean hasNextBlock()
	{
		return false;//(index < files.length);
	}
}
