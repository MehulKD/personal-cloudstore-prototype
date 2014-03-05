package driver;

import java.io.File;

import algorithm.CDCSplit;

public class Driver
{
	public static void main(String[] args)
	{
		new Driver().run(args);
	}
	
	String srcFilesDir = null, dstFilesDir = null, logFilesDir = null;
	File srcDir, dstDir, logDir;
	
	int CDC_WINDOW_SIZE = 48;
	int CDC_MAGIC = 0x00001847;
	int CDC_MASK = 0x00001fff;
	int BUFFER_SIZE = CDC_WINDOW_SIZE * CDC_WINDOW_SIZE * 8;
	int MAX_BLOCK_SIZE = 4 * 1024 * 1024;
	int MIN_BLOCK_SIZE = 256 * 1024;
	int STATIC_BLOCK_SIZE = 4 * 1024 * 1024;
	int SLID_WINDOW_STEP = 1;
	int THREAD_NUM = 10;
	
	protected void run(String[] args)
	{
		if (args.length == 0) 
		{
			System.out.println(usage());
			System.exit(0);
		}
		
		try
		{
			for (int i = 0; i < args.length; i++) 
			{
				if (args[i].equals("-d"))
				{
					dstFilesDir = args[++i];
				} 
				else if (args[i].equals("-s")) 
				{
					srcFilesDir = args[++i];
				}
				else if (args[i].equals("-l")) 
				{
					logFilesDir = args[++i];
				}
				else if (args[i].equals("--thread")) 
				{
					THREAD_NUM = Integer.parseInt(args[++ i]);
				}
				else if (args[i].equals("--window")) 
				{
					CDC_WINDOW_SIZE = Integer.parseInt(args[++ i]);
				}
				else if (args[i].equals("--step")) 
				{
					SLID_WINDOW_STEP = Integer.parseInt(args[++ i]);
				}
				else if (args[i].equals("--magic")) 
				{
					CDC_MAGIC = Integer.parseInt(args[++ i], 16);
				}
				else if (args[i].equals("--mask")) 
				{
					CDC_MASK = Integer.parseInt(args[++ i], 16);
				}
				else if (args[i].equals("--buffer")) 
				{
					BUFFER_SIZE = Integer.parseInt(args[++ i]);
				}
				else if (args[i].equals("--min")) 
				{
					MIN_BLOCK_SIZE = Integer.parseInt(args[++ i]);
				}
				else if (args[i].equals("--max")) 
				{
					MAX_BLOCK_SIZE = Integer.parseInt(args[++ i]);
				}
			}
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			System.out.println(usage());
			System.exit(0);
		}
		
		if (srcFilesDir == null || dstFilesDir == null
				|| logFilesDir == null)
		{
			System.out.println(usage());
			System.exit(0);
		}
		else
		{
			srcDir = new File(srcFilesDir);
			dstDir = new File(dstFilesDir);
			logDir = new File(logFilesDir);
			
			if (!srcDir.exists() || !dstDir.exists()
					|| !logDir.exists())
			{
				System.out.println("Dir not exsit");
				System.out.println(usage());
				System.exit(0);
			}
			else
			{
//				SplitAlgorithm [] algorithms = {new CDCSplit(BUFFER_SIZE, 
//						CDC_WINDOW_SIZE, CDC_MAGIC, CDC_MASK, MIN_BLOCK_SIZE, 
//						MAX_BLOCK_SIZE, srcDir, dstDir, logDir),
//				new StaticSplit(BUFFER_SIZE, STATIC_BLOCK_SIZE, srcDir, dstDir, logDir)};
//				
//				for (SplitAlgorithm algorithm : algorithms)
//				{
//					algorithm.test();
//				}
				new CDCSplit(BUFFER_SIZE, THREAD_NUM,
						CDC_WINDOW_SIZE, SLID_WINDOW_STEP, CDC_MAGIC, CDC_MASK, MIN_BLOCK_SIZE, 
						MAX_BLOCK_SIZE, srcDir, dstDir, logDir).test();
			}
		}
	}
	
	
	private String usage() 
	{
		return ("\n"
				+ "Usage:  java -jar SplitCompareTest.jar -s SOURCE_DIR -d DEST_DIR -l LOG_FILES_DIR "
				+ "[--buffer SIZE --window SIZE --magic MAGIC --mask MASK --min SIZE --max SIZE --staic SIZE]\n"
				+ "Parameters:\n"
				+ "    -s  source(old) files directory path(relative)\n"
				+ "    -d  destination(new) files directory path(relative)\n"
				+ "    -l  log files directory path(relative)\n"
				+ "Options:\n"
				+ "    --buffer  cdc split slide buffer size, default 48 * 48 * 8 B\n"
				+ "    --window  cdc split slide window size, default 48 B\n"
				+ "    --magic  cdc split break point hash magic(ending part), in hex(without starting 0x), default 00001847\n"
				+ "    --mask  cdc split break point hash magic mask, in hex(without starting 0x, shorter than 32 bits), default 00001fff\n"
				+ "    --min  cdc split minimun block size, default 256 KB\n"
				+ "    --max  cdc split maximun block size, default 4 MB\n"
				+ "    --static  static split block size, default 4 MB\n"
				+ "\n");
	}
}
