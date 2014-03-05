package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;


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
        	e.printStackTrace();
			System.exit(0);
        }
    }
    
	int BUFFER_SIZE;
    
	public IndexModel(int buffer)
	{
		BUFFER_SIZE = buffer;
	}
	
    
    /**
     * Get the MD5 digest.
     * @param the target file
     * @return the MD5 string, in lower case
     * */
    public String getMD5(File file) 
    {
    	String digest = null;
	    FileInputStream fileInputStream = null;
	    try
	    {
	    	MD5.reset();
		    fileInputStream = new FileInputStream(file);
		    byte[] buffer = new byte[BUFFER_SIZE];
		    int length;
		    while ((length = fileInputStream.read(buffer)) != -1) 
		    	MD5.update(buffer, 0, length);
		    fileInputStream.close();
		    digest = new String(Hex.encodeHex(MD5.digest()));
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
	    
	    return digest;
    }
    
    public String getMD5(File file, long offset, long size) 
    {
    	String digest = null;
	    try
	    {
	    	RandomAccessFile fin = new RandomAccessFile(file, "r");
	    	fin.seek(offset);
	    	MD5.reset();
	    	byte[] buffer = new byte[BUFFER_SIZE];
		    int length;
		    long totalRead = 0;
		    while (totalRead < size) 
		    {
		    	length = fin.read(buffer, 0, (int) ((size - totalRead < BUFFER_SIZE)
		    			? (size - totalRead) : BUFFER_SIZE));
		    	MD5.update(buffer, 0, length);
		    	totalRead += length;
		    }
		    fin.close();
		    digest = new String(Hex.encodeHex(MD5.digest()));
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
	    
	    return digest;
    }
}
