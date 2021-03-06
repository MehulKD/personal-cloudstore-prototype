package model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Hex;

import com.planetj.math.rabinhash.PJLProvider;

import util.Loger;


/**
 * <p>This is the index model(util), it calculate the message digest
 * of a file. In the current framework, we calculate the MD5 digest,
 * it could be expanded.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:34 2013/12/20</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class IndexModel
{
	static MessageDigest MD5 = null;

	//initialize the MD5 algorithm instance
    static 
    {
        try 
        {
        	MD5 = MessageDigest.getInstance("MD5");
        } 
        catch (NoSuchAlgorithmException e) 
        {
        	Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "IndexModel init");
        }
    }
    
    /**
     * Get MD5 hash of a string
     * */
    public static String getMD5(String str) 
    {
    	MD5.reset();
    	try
		{
			MD5.update(str.getBytes("UTF-8"));
			return new String(Hex.encodeHex(MD5.digest()));
		}
		catch (UnsupportedEncodingException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "IndexModel getMD5");
			return null;
		}
    }
    
    /**
     * Get the MD5 digest.
     * @param the target file
     * @return the MD5 string, in lower case
     * */
    public static String getMD5(File file) 
    {
    	String digest = null;
	    FileInputStream fileInputStream = null;
	    try
	    {
	    	MD5.reset();
		    fileInputStream = new FileInputStream(file);
		    byte[] buffer = new byte[Constant.BUFFER_SIZE];
		    int length;
		    while ((length = fileInputStream.read(buffer)) != -1) 
		    {
		    	MD5.update(buffer, 0, length);
		    }
		    fileInputStream.close();
		    digest = new String(Hex.encodeHex(MD5.digest()));
	    } 
	    catch (FileNotFoundException e) 
	    {
	    	Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "IndexModel getMD5");
	    } 
	    catch (IOException e) 
	    {
	    	Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "IndexModel getMD5");
	    } 
	    finally
	    {
		    try
		    {
		    	if (fileInputStream != null)
		    		fileInputStream.close();
		    } 
		    catch (IOException e) 
		    {
		    	Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "IndexModel getMD5");
		    }
	    }
	    
	    return digest;
    }
    
    /**
     * Apply CDC to a file
     * */
    public static ArrayList<Block> CDCSplit(File file)
    {
    	return split(file);
    }
    
    protected static MessageDigest getRollHashFunc() throws NoSuchAlgorithmException
	{
		return MessageDigest.getInstance("RHF64", new PJLProvider());
	}

    /**
     * Get the MD5 of part of a file
     * */
    public static String getMD5(File file, long offset, long size) 
    {
    	String digest = null;
	    try
	    {
	    	RandomAccessFile fin = new RandomAccessFile(file, "r");
	    	fin.seek(offset);
	    	MD5.reset();
	    	byte[] buffer = new byte[Constant.BUFFER_SIZE];
		    int length;
		    long totalRead = 0;
		    while (totalRead < size) 
		    {
		    	length = fin.read(buffer, 0, (int) ((size - totalRead < Constant.BUFFER_SIZE)
		    			? (size - totalRead) : Constant.BUFFER_SIZE));
		    	MD5.update(buffer, 0, length);
		    	totalRead += length;
		    }
		    fin.close();
		    digest = new String(Hex.encodeHex(MD5.digest()));
	    } 
	    catch (FileNotFoundException e) 
	    {
	    	Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "at IndexModel getMD5");
	    } 
	    catch (IOException e) 
	    {
	    	Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "at IndexModel getMD5");
	    }
	    
	    return digest;
    }

    /**
     * Apply CDC to a file
     * */
    public static ArrayList<Block> split(File file)
    {
    	ArrayList<Block> blocks = new ArrayList<Block>();
    	try
		{
    		MessageDigest digest = getRollHashFunc();
			FileInputStream fin = new FileInputStream(file);
			byte[] buffer = new byte[Constant.BUFFER_SIZE];
			int read = fin.read(buffer);
			long offset = 0, pos = 0;
			int i = 0;
			long size = 0;
			boolean added = false;
			while (read != -1)
			{
				//calculate the last window, cause if the stream reaches end, it won't
				//enter the loop.
				for (; i <= read - Constant.CDC_WINDOW_SIZE; i += Constant.SLID_WINDOW_STEP)
				{
					digest.reset();
					digest.update(buffer, i, Constant.CDC_WINDOW_SIZE);
					ByteBuffer bf = ByteBuffer.allocate(digest.getDigestLength());
					bf.put(digest.digest());
					int end = bf.getInt(digest.getDigestLength() - 4) & Constant.CDC_MASK;
					size = pos - offset + Constant.CDC_WINDOW_SIZE;
					if (((end == Constant.CDC_MAGIC) && (Constant.MIN_BLOCK_SIZE <= size))
							|| (size == Constant.MAX_BLOCK_SIZE))
					{
						blocks.add(new Block(file.getAbsolutePath(), size, offset, blocks.size(), 
								getMD5(file, offset, size)));
						
						offset = pos + Constant.CDC_WINDOW_SIZE;
						pos = offset;
						//skip CDC_WINDOW_SIZE - SLID_WINDOW_STEP calculations
						i += Constant.CDC_WINDOW_SIZE - Constant.SLID_WINDOW_STEP;
						added = true;
					}
					else
					{
						pos += Constant.SLID_WINDOW_STEP;
						added = false;
					}
				}
				
				//if the file is smaller than window size, add it as one block
				if (read < Constant.CDC_WINDOW_SIZE)
				{
					size = read;
					read = fin.read(buffer, 0, Constant.BUFFER_SIZE);
					//read must be -1
					i = 0;
				}
				else
				{
					if (!added)
					{
						if (0 < i - read)
						{
							//skip first i - read bytes of next read
							read = fin.read(buffer, 0, Constant.BUFFER_SIZE);
							
							i = i - read;
							//! needn't change pos anymore...
//							pos = i;
						}
						else if (i - read < 0)
						{
							//if the last window hasn't been added,
							//just copy the last window except its first byte.
							//CDC_WINDOW_SIZE - SLID_WINDOW_STEP == read - i
							int left = read - i;
							byte[] tail = new byte[left];
							for (int j = 0; j < left; j ++)
							{
								tail[j] = buffer[i + j];
							}
							read = fin.read(buffer, left, 
									Constant.BUFFER_SIZE - left);

							if (read != -1)
							{
								for (int j = 0; j < left; j ++)
								{
									buffer[j] = tail[j];
								}
								read += left;	//pretend that the copied left bytes are read from disk
							}
							
							i = 0;
							//! needn't change pos anymore...
//							pos = i;
						}
						else
						{
							//!oh fuck...
							read = fin.read(buffer, 0, Constant.BUFFER_SIZE);
							
							i = 0;
							//! needn't change pos anymore...
//							pos = i;
						}
					}
					else
					{
						read = fin.read(buffer, 0, Constant.BUFFER_SIZE);
						
						i = 0;
						//! needn't change pos anymore...
//						pos = i;
					}
				}
				
//				System.out.println("finish " + offset + " / " + file.length());
			}
			fin.close();
			
			//add the end part if haven't
			if (!added)
			{
				size = file.length() - offset;
				blocks.add(new Block(file.getAbsolutePath(), size, offset, blocks.size(), 
						getMD5(file, offset, size)));
			}
		}
		catch (FileNotFoundException e)
		{
        	e.printStackTrace();
//			System.exit(0);
		}
		catch (IOException e)
		{
        	e.printStackTrace();
//			System.exit(0);
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
//			System.exit(0);
		}
    	return blocks;
    }
}
