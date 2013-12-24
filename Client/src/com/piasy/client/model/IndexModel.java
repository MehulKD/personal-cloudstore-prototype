package com.piasy.client.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

import android.util.Log;

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
        	if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at IndexModel init : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at IndexModel init : NoSuchAlgorithmException");
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
		    fileInputStream = new FileInputStream(file);
		    byte[] buffer = new byte[Constant.BUFFER_SIZE];
		    int length;
		    while ((length = fileInputStream.read(buffer)) != -1) 
		    	MD5.update(buffer, 0, length);
		
		    digest = new String(Hex.encodeHex(MD5.digest()));
	    } 
	    catch (FileNotFoundException e) 
	    {
	    	if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at IndexModel getMD5 : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at IndexModel getMD5 : FileNotFoundException");
	    } 
	    catch (IOException e) 
	    {
	    	if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at IndexModel getMD5 : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at IndexModel getMD5 : IOException");
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
		    	if (e.getMessage() != null)
					Log.e(Constant.LOG_LEVEL_ERROR, "at IndexModel getMD5 : " + e.getMessage());
				else
					Log.e(Constant.LOG_LEVEL_ERROR, "at IndexModel getMD5 : IOException");
		    }
	    }
	    
	    return digest;
    }
}
