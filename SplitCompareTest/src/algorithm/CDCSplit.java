package algorithm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import com.planetj.math.rabinhash.PJLProvider;

import util.Block;
import util.IndexModel;

public class CDCSplit extends SplitAlgorithm
{
//	static MessageDigest MD5 = null;
//
//	//initialize the MD5 algorithm instance
//    static 
//    {
//        try 
//        {
//        	MD5 = MessageDigest.getInstance("MD5");
//        } 
//        catch (NoSuchAlgorithmException e) 
//        {
//        	e.printStackTrace();
//			System.exit(0);
//        }
//    }
	
	int CDC_WINDOW_SIZE;
	int SLID_WINDOW_STEP;
	int CDC_MAGIC;
	int CDC_MASK;
	int BUFFER_SIZE;
	int MAX_BLOCK_SIZE;
	int MIN_BLOCK_SIZE;
	FileOutputStream fout, rFout;
	int THREAD_NUM;
	int threadCount = 0;
	
	public CDCSplit(int buffer, int thread, int window, int step, int magic, int mask, int min, int max,
			File srcDir, File dstDir, File logDir)
	{
		super(srcDir, dstDir);
		THREAD_NUM = thread;
		CDC_WINDOW_SIZE = window;
		SLID_WINDOW_STEP = step;
		CDC_MAGIC = magic;
		CDC_MASK = mask;
		BUFFER_SIZE = buffer;
		MAX_BLOCK_SIZE = max;
		MIN_BLOCK_SIZE = min;
		File file = new File(logDir.getAbsolutePath() + "/CDCSplit." 
				+ step + ".blocks.csv");
		File rFile = new File(logDir.getAbsolutePath() + "/CDCSplit." 
			+ step + ".redundance.csv");
		try
		{
			if (!file.exists())
			{
				file.createNewFile();
			}
			if (!rFile.exists())
			{
				rFile.createNewFile();
			}
			fout = new FileOutputStream(file);
			rFout = new FileOutputStream(rFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	protected MessageDigest getRollHashFunc() throws NoSuchAlgorithmException
	{
		return MessageDigest.getInstance("RHF64", new PJLProvider());
	}
	
	@Override
	public ArrayList<Block> split(File file)
    {
    	ArrayList<Block> blocks = new ArrayList<Block>();
    	try
		{
    		IndexModel index = new IndexModel(BUFFER_SIZE);
    		MessageDigest digest = getRollHashFunc();
			FileInputStream fin = new FileInputStream(file);
			byte[] buffer = new byte[BUFFER_SIZE];
			int read = fin.read(buffer);
			long offset = 0, pos = 0;
			int i = 0;
			long size = 0;
			boolean added = false;
			while (read != -1)
			{
				//calculate the last window, cause if the stream reaches end, it won't
				//enter the loop.
				for (; i <= read - CDC_WINDOW_SIZE; i += SLID_WINDOW_STEP)
				{
					digest.reset();
					digest.update(buffer, i, CDC_WINDOW_SIZE);
					ByteBuffer bf = ByteBuffer.allocate(digest.getDigestLength());
					bf.put(digest.digest());
					int end = bf.getInt(digest.getDigestLength() - 4) & CDC_MASK;
					size = pos - offset + CDC_WINDOW_SIZE;
					if (((end == CDC_MAGIC) && (MIN_BLOCK_SIZE <= size))
							|| (size == MAX_BLOCK_SIZE))
					{
						blocks.add(new Block(file.getAbsolutePath(), offset, size, 
								index.getMD5(file, offset, size)));
						if (size == MAX_BLOCK_SIZE)
						{
							maxSizeBlockNum ++;
						}
						else if (size == MIN_BLOCK_SIZE)
						{
							minSizeBlockNum ++;
						}
						
						offset = pos + CDC_WINDOW_SIZE;
						pos = offset;
						//skip CDC_WINDOW_SIZE - SLID_WINDOW_STEP calculations
						i += CDC_WINDOW_SIZE - SLID_WINDOW_STEP;
						added = true;
					}
					else
					{
						pos += SLID_WINDOW_STEP;
						added = false;
					}
				}
				
				//if the file is smaller than window size, add it as one block
				if (read < CDC_WINDOW_SIZE)
				{
					size = read;
					read = fin.read(buffer, 0, BUFFER_SIZE);
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
							read = fin.read(buffer, 0, BUFFER_SIZE);
							
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
									BUFFER_SIZE - left);

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
							read = fin.read(buffer, 0, BUFFER_SIZE);
							
							i = 0;
							//! needn't change pos anymore...
//							pos = i;
						}
					}
					else
					{
						read = fin.read(buffer, 0, BUFFER_SIZE);
						
						i = 0;
						//! needn't change pos anymore...
//						pos = i;
					}
				}
			}
			fin.close();
			
			//add the end part if haven't
			if (!added)
			{
				size = file.length() - offset;
				blocks.add(new Block(file.getAbsolutePath(), offset, size, 
						index.getMD5(file, offset, size)));
				if (size == MAX_BLOCK_SIZE)
				{
					maxSizeBlockNum ++;
				}
				else if (size == MIN_BLOCK_SIZE)
				{
					minSizeBlockNum ++;
				}
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
			System.exit(0);
		}
    	return blocks;
    }
	
	@Override
	public void log(String content)
	{
		try
		{
			synchronized (fout)
			{
				fout.write(content.getBytes("UTF-8"));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
//			System.exit(0);
		}
	}
	
	@Override
	public String getAlgorithmName()
	{
		return "CDC split";
	}

	@Override
	public String getTableName()
	{
		return "cdcsplitblocks";
	}
	
	ArrayList<SplitThread> threads = new ArrayList<SplitThread>();
	HashMap<String, Block> base = new HashMap<String, Block>();
	@Override
	public void test()
	{
		System.out.println("Start test of " + getAlgorithmName());
		System.out.println("Begin indexing dstDir...");

		long start = System.currentTimeMillis();
		stepSplit(dstDir);
		
		while (threadCount != 0)
		{
			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		System.out.println("Indexing dstDir finish!");
		long end = System.currentTimeMillis();
		System.out.println("Use time : " + (end - start) + " ms");
		
		stepDeRedundant(dstDir);
		
		System.out.println("Test result:");
		System.out.println("maxSizeBlockNum : " + maxSizeBlockNum 
				+ ", minSizeBlockNum : " + minSizeBlockNum);
	}
	
	
	protected void logForRedundant(String content)
	{
		try
		{
			synchronized (rFout)
			{
				rFout.write(content.getBytes("UTF-8"));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
//			System.exit(0);
		}
	}

	HashMap<String, RedundanceInfo> redundance = new HashMap<String, RedundanceInfo>();
	protected void stepDeRedundant(File dir)
	{
		for (String key : allBlocks.keySet())
		{
			ArrayList<Block> blocks = allBlocks.get(key);
			RedundanceInfo info = redundance.get(key);
			
			for (Block block : blocks)
			{
				if (base.get(block.hash).redundant)
				{
					info.redundance.put(block.hash, block);
				}
			}
			
			logForRedundant(key + "," + info.totalBlockNum + "," 
					+ info.getExistBlockNum() + "," 
					+ info.totalFileSize + "," 
					+ info.getExistFileSize() + "\n");
		}
	}

	HashMap<String, ArrayList<Block>> allBlocks = new HashMap<String, ArrayList<Block>>();
	protected void stepSplit(File dir)
	{	
		ArrayList<File> files = new ArrayList<File>();
		ArrayList<File> dirs = new ArrayList<File>();
		
		File [] mFiles = dir.listFiles();
		if (mFiles != null)
		{
			for (File file : mFiles)
			{
				if (file.isDirectory())
				{
					dirs.add(file);
				}
				else
				{
					files.add(file);
				}
			}
		}
		
		for (File file : dirs)
		{
			synchronized (threads)
			{
				if (threads.size() < THREAD_NUM)
				{
					SplitThread thread = new SplitThread(file);
					thread.start();
					threads.add(thread);
				}
				else
				{
					stepSplit(file);
				}
			}
		}
		
		for (File file : files)
		{
			ArrayList<Block> blocks = split(file);
			synchronized (allBlocks)
			{
				allBlocks.put(file.getAbsolutePath(), blocks);
			}
			RedundanceInfo info = new RedundanceInfo(blocks.size(), file.length());
			synchronized (redundance)
			{
				redundance.put(file.getAbsolutePath(), info);
			}
			
						
			for (Block block : blocks)
			{
				log(block.hash + "," + block.filepath + "," 
						+ block.offset + "," + block.size + "\n");
				
				synchronized (base)
				{
					if (base.containsKey(block.hash))
					{
						base.get(block.hash).redundant = true;
					}
					else
					{
						base.put(block.hash, block);
					}
				}
			}
		}
	}
	
	class SplitThread extends Thread
	{
		File dir;
		
		public SplitThread(File dir)
		{
			this.dir = dir;
		}
		
		@Override
		public void run()
		{
			threadCount ++;
			System.out.println("Thread " + threadCount + " start.");
			stepSplit(dir);
			System.out.println("Thread " + threadCount + " finish.");
			threadCount --;
			synchronized (threads)
			{
				threads.remove(this);
			}
		}
	}
	
	class RedundanceInfo
	{
		public int totalBlockNum;
		public long totalFileSize;
		public HashMap<String, Block> redundance = new HashMap<String, Block>();
		
		public RedundanceInfo(int totalBlockNum, long totalFileSize)
		{
			this.totalBlockNum = totalBlockNum;
			this.totalFileSize = totalFileSize;
		}
		
		public int getExistBlockNum()
		{
			return redundance.size();
		}
		
		public long getExistFileSize()
		{
			long size = 0;
			for (String key : redundance.keySet())
			{
				size += redundance.get(key).size;
			}
			
			return size;
		}
	}
}
