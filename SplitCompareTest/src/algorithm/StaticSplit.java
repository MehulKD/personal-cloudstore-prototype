package algorithm;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import util.Block;
import util.IndexModel;

public class StaticSplit extends SplitAlgorithm
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
    
    int STATIC_BLOCK_SIZE;
    int BUFFER_SIZE;
	IndexModel index;
    
    public StaticSplit(int buffer, int size,
			File srcDir, File dstDir, File logDir)
    {
		super(srcDir, dstDir);
    	STATIC_BLOCK_SIZE = size;
    	BUFFER_SIZE = size;
		index = new IndexModel(buffer);
    }

	@Override
    public ArrayList<Block> split(File file)
    {
    	ArrayList<Block> blocks = new ArrayList<Block>();
    	long totalLength = file.length();
    	long num = totalLength / STATIC_BLOCK_SIZE;
		if ((file.length() % STATIC_BLOCK_SIZE) != 0)
		{
			num ++;
		}
		
		long offset = 0;
		for (int i = 0; i < num; i ++)
		{
			long size = (totalLength - offset < STATIC_BLOCK_SIZE) ? (totalLength - offset) : STATIC_BLOCK_SIZE;
			blocks.add(new Block(file.getAbsolutePath(), offset, size, 
					index.getMD5(file, offset, size)));
			offset += size;
		}
    	return blocks;
    }

	@Override
	public void log(String content)
	{
		
	}

	@Override
	public String getAlgorithmName()
	{
		return "Static split";
	}

	@Override
	public String getTableName()
	{
		return "staticsplitblocks";
	}
}
