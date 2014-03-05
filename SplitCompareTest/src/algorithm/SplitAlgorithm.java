package algorithm;

import java.io.File;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
import java.util.ArrayList;

import util.Block;

public abstract class SplitAlgorithm
{
	File srcDir, dstDir;
	String url = "jdbc:mysql://localhost/dropboxappdata";
    String user = "root";
    String pwd = "1qaz2wsx!@"; 
	long maxSizeBlockNum = 0, minSizeBlockNum = 0;
	
	public SplitAlgorithm(File srcDir, File dstDir)
	{
		this.srcDir = srcDir;
		this.dstDir = dstDir;
	}
	
	public abstract ArrayList<Block> split(File file);
	
	public abstract void log(String content);
	
	public abstract String getAlgorithmName();
	
	public abstract String getTableName();
	
	public void test()
	{
//		try
//		{
//			Class.forName("com.mysql.jdbc.Driver").newInstance();
//			Connection conn = DriverManager.getConnection(url, user, pwd);
//			if (!conn.isClosed())
//			{
//				Statement stmt = conn.createStatement();
				
				System.out.println("Start test of " + getAlgorithmName());
				
				long start = System.currentTimeMillis();
				
				System.out.println("Begin indexing dstDir...");
				
//				HashMap<String, Block> base = new HashMap<String, Block>();
				ArrayList<File> dstFiles = getAllFiles(dstDir);
				for (File file : dstFiles)
				{
					ArrayList<Block> blocks = split(file);
					for (Block block : blocks)
					{
						log(block.hash + "," + block.filepath + "," 
								+ block.offset + "," + block.size + "\n");
//						String sql = "INSERT INTO " + getTableName() + "(hash, filepath, offset, size) "
//								+ "VALUES" + "('" + block.hash + "', '" + block.filepath + "', " 
//								+ block.offset + ", " + block.size + ")";
//						stmt.execute(sql);
//						base.put(block.hash, block);
					}
				}
				
				System.out.println("Indexing dstDir finish!");
//				System.out.println("Begin indexing srcDir...");
//				
//				long totalBlockNum = 0, existBlockNum = 0;
//				long totalFileSize = 0, existFileSize = 0;
//				ArrayList<File> srcFiles = getAllFiles(srcDir);
//				for (File file : srcFiles)
//				{
//					totalFileSize += file.length();
//					ArrayList<Block> blocks = split(file);
//					totalBlockNum += blocks.size();
//					for (Block block : blocks)
//					{
//						String sql = "SELECT * FROM " + getTableName() + " WHERE hash = '" 
//									+ block.hash + "'";
//						ResultSet rst = stmt.executeQuery(sql);
////						if (base.containsKey(block.hash))
//						if (rst.next())
//						{
//							existBlockNum ++;
//							existFileSize += block.size;
//						}
//					}
//				}
//				
//				System.out.println("Indexing srcDir finish!");
				long end = System.currentTimeMillis();
				System.out.println("Use time : " + (end - start) + " ms");
				
				
				System.out.println("Test result:");
				System.out.println("maxSizeBlockNum : " + maxSizeBlockNum 
						+ ", minSizeBlockNum : " + minSizeBlockNum);
//				System.out.println("Block num : " + existBlockNum + " / " + totalBlockNum 
//						+ " = " + ((double) existBlockNum / (double) totalBlockNum));
//				System.out.println("Size : " + existFileSize + " / " + totalFileSize
//						+ " = " + ((double) existFileSize / (double) totalFileSize));
//			}
//		}
//		catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e)
//		{
//			e.printStackTrace();
//			System.exit(0);
//		}
	}

	protected ArrayList<File> getAllFiles(File dir)
	{
		ArrayList<File> ret = new ArrayList<File>();
		File [] files = dir.listFiles();
		if (files != null)
		{
			for (File file : files)
			{
				if (file.isDirectory())
				{
					ret.addAll(getAllFiles(file));
				}
				else
				{
					ret.add(file);
				}
			}
		}
		return ret;
	}
}
