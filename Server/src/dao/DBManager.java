package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import model.Block;
import model.Constant;
import model.IndexModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.Loger;

/**
 * <p>The DAO database manager class, encapsulate the sql statement with
 * a little bit safe method.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		14:13 2013/12/15</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class DBManager
{
	String url = "jdbc:mysql://localhost/dropboxappdata";
    String user = "root";
    String pwd = "1qaz2wsx!@";
    Connection conn;
    Statement stmt;
    
    Timer keepAliveTimer = new Timer();
    /**
     * To keep mysql connection alive
     * */
    TimerTask keepAliveTask = new TimerTask()
	{
		
		@Override
		public void run()
		{
			Statement stmt = getStatement();
			if (stmt != null)
			{
				String sql = "SELECT COUNT(*) FROM blocks WHERE hash = 'a67e553cf7481c39980a659ae6e175ba'";
				try
				{
					stmt.executeQuery(sql);
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				
			}
		}
	};
    
	/**
	 * Get a statement to execute a sql
	 * */
	protected Statement getStatement()
    {
    	Statement stmt = null;
    	synchronized (conn)
		{
			try
			{
				if (!conn.isClosed())
				{
					stmt = conn.createStatement();
				}
			}
			catch (SQLException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager getStatement");
			}
		}
    	return stmt;
    }
	
	/**
	 * Initialize the database manager
	 * */
    public boolean init()
    {
    	boolean ret = false;
    	try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(url, user, pwd);
			if (!conn.isClosed())
			{
				stmt = conn.createStatement();
				String  sql = "CREATE TABLE IF NOT EXISTS blocks (" +
				  			  "id int NOT NULL AUTO_INCREMENT, " + 
				  			  "PRIMARY KEY(id), " + 
				  			  "hash varchar(32), " +
				  			  "size bigint)";
				stmt.execute(sql);
				
				sql = "CREATE TABLE IF NOT EXISTS files (" +
			  			  "id int NOT NULL AUTO_INCREMENT, " + 
			  			  "PRIMARY KEY(id), " + 
			  			  "filename varchar(128), " +
			  			  "size bigint, " + 
			  			  "blocknum int, " + 
			  			  "owner varchar(64))";
				stmt.execute(sql);
				
				keepAliveTimer.schedule(keepAliveTask, Constant.DB_CON_KEEPALIVE_INTERVAL, 
						Constant.DB_CON_KEEPALIVE_INTERVAL);
			
				ret = true;
			}
		}
		catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | SQLException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager init");
		}
    	return ret;
    }
    
    /**
     * Filt the sql keyword in params
     * */
    protected String prepare(String param, int type)
    {
    	StringBuffer sb = new StringBuffer();
    	for (int i = 0; i < param.length(); i ++)
    	{
    		switch (type)
			{
			case Constant.STRING:
				if (param.charAt(i) == '\'')
	    		{
	    			sb.append("''");
	    		}
	    		else
	    		{
					sb.append(param.charAt(i));
				}
				break;
			case Constant.TABLENAME:
				if (param.charAt(i) == '`')
	    		{
	    			sb.append("``");
	    		}
	    		else
	    		{
					sb.append(param.charAt(i));
				}
				break;
			default:
				break;
			}
    	}
    	return sb.toString();
    }
    
    /**
     * Get the hashed table name from a user
     * */
    protected String getTableName(String filename, String owner)
    {
    	return IndexModel.getMD5(owner + "#$%$#" + filename);
    }
    
    /**
     * Get all files of a user
     * */
    public ArrayList<FileEntry> getAllFiles(String owner)
    {
    	ArrayList<FileEntry> ret = new ArrayList<FileEntry>();
    	String sql = "SELECT * FROM files WHERE `owner`='" + prepare(owner, Constant.STRING) + "'";
    	synchronized (stmt)
		{
			try
			{
				Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager getAllFiles");
				ResultSet res = stmt.executeQuery(sql);
				while (res.next())
				{
					FileEntry file = new FileEntry(res.getLong("id"), res.getString("filename"), 
							res.getLong("size"), res.getInt("blockNum"), res.getString("owner"));
					ret.add(file);
				}
			}
			catch (SQLException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager getAllFiles");
			}
		}
    	return ret;
    }
    
    /**
     * Get info of one file
     * */
    public JSONObject getFileInfo(String filename, String owner)
    {
    	JSONObject fileInfo = new JSONObject();
    	String sql = "SELECT * FROM `" + prepare(getTableName(filename, owner), Constant.TABLENAME)
    			+ "` ORDER BY `blockseq`";
    	synchronized (stmt)
		{
			try
			{
				fileInfo.put("filename", filename);
				ResultSet rse = stmt.executeQuery(sql);
				JSONArray blocksInfo = new JSONArray();
				long filesize = 0;
				while (rse.next())
				{
					JSONObject blockInfo = new JSONObject();
					blockInfo.put("hash", rse.getString("hash"));
					long blocksize = rse.getLong("size");
					blockInfo.put("size", blocksize);
					filesize += blocksize;
					blockInfo.put("seq", rse.getInt("blockseq"));
					blocksInfo.put(blockInfo);
				}
				fileInfo.put("blocks", blocksInfo);
				fileInfo.put("size", filesize);
			}
			catch (SQLException | JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager getFileInfo");
			}
		}
    	return fileInfo;
    }
    
    /**
     * Query whether there is a block existing in the database
     * */
    public boolean hasBlock(String hash)
    {
    	boolean ret = false;
    	String sql = "SELECT * FROM blocks WHERE `hash`='" + prepare(hash, Constant.STRING) + "'";
    	synchronized (stmt)
		{
			try
			{
				Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager hasBlock");
				ResultSet res = stmt.executeQuery(sql);
				if (res.next())
				{
					ret = true;
				}
			}
			catch (SQLException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager hasBlock");
			}
		}
    	return ret;
    }
    
    /**
     * Add a file into the database
     * */
    public boolean addFile(JSONObject params, String owner)
    {
    	boolean ret = false;
    	try
		{
    		String sql = "INSERT INTO files (filename, size, blocknum, owner) VALUES ("
    				+ "'" + prepare(params.getString("filename"), Constant.STRING) + "', "
    				+ params.getLong("size") + ", "
    				+ params.getJSONArray("blocks").length() + ", "
    				+ "'" + prepare(owner, Constant.STRING) + "')";
        	synchronized (stmt)
    		{
        		Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager addFile");
    			stmt.execute(sql);
    			sql = "CREATE TABLE IF NOT EXISTS `" + prepare(getTableName(params.getString("filename"), owner), 
    					Constant.TABLENAME) + "`" + " (id int NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), "
    					+ "hash varchar(32), size bigint, blockseq int)";
    			Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager addFile");
    			stmt.execute(sql);
    			ret = true;
    		}
		}
		catch (JSONException | SQLException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager addFile");
		}
    	return ret;
    }
    
    /**
     * Add one block to its original file
     * */
    public boolean addBlockToFile(JSONObject block, String filename, String owner)
    {
    	boolean ret = false;
    	try
		{
    		Statement stmt;
    		if ((stmt = getStatement()) != null)
    		{
				if (!hasBlockInFile(block.getString("hash"), filename, owner))
				{
					String sql = "INSERT INTO `" + prepare(getTableName(filename, owner), Constant.TABLENAME) + "` "
		        			+ "(hash, size, blockseq) VALUES ("
		    				+ "'" + prepare(block.getString("hash"), Constant.STRING) + "', "
		    				+ block.getLong("size") + ", "
		    				+ block.getInt("seq") + ")";
					Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager addBlockToFile");
    				stmt.execute(sql);
				}
				ret = true;
    		}
		}
		catch (JSONException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager addBlockToFile");
		}
		catch (SQLException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager addBlockToFile");
		}
    	return ret;
    }
    
    /**
     * Query whether there is a block in one file
     * */
    protected boolean hasBlockInFile(String hash, String filename, String owner)
    {
    	boolean ret = false;
    	try
		{
    		Statement stmt;
    		if ((stmt = getStatement()) != null)
    		{
    			String sql = "SELECT * FROM `" + prepare(getTableName(filename, owner), Constant.TABLENAME) + "` "
        				+ "WHERE `hash` = '" + prepare(hash, Constant.STRING) + "'";
    			Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager hasBlockInFile");
    			ResultSet rst = stmt.executeQuery(sql);
    			if (rst.next())
    			{
    				ret = true;
    			}
    		}
		}
		catch (SQLException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager hasBlockInFile");
		}
    	
    	return ret;
    }
    
    /**
     * Add one block into the database, its original file, too.
     * */
    public boolean addBlock(JSONObject block, String filename, String owner)
    {
    	boolean ret = false;
    	try
		{
    		Statement stmt;
    		if ((stmt = getStatement()) != null)
    		{
    			addBlockToFile(block, filename, owner);
    			
    			if (!hasBlock(block.getString("hash")))
    			{
        			String sql = "INSERT INTO `blocks` (hash, size) VALUES ("
    	    				+ "'" + prepare(block.getString("hash"), Constant.STRING) + "', "
    	    				+ block.getLong("size") + ")";
    				Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager addBlock");
    				stmt.execute(sql);
    				ret = true;
    			}
    		}
		}
		catch (JSONException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager addBlock");
		}
		catch (SQLException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager addBlock");
		}
    	return ret;
    }
    
    /**
     * Update blocks info of one file
     * */
    public boolean updateFile(ArrayList<Block> blocks, String filename, String owner)
    {
    	boolean ret = false;
    	try
		{
    		Statement stmt;
    		if ((stmt = getStatement()) != null)
    		{
				String sql = "TRUNCATE `" + prepare(getTableName(filename, owner), Constant.TABLENAME) + "`";
				Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager updateFile");
				stmt.execute(sql);
				
    			for (Block block : blocks)
    			{
    				if (!hasBlock(block.hash))
    				{
    					sql = "INSERT INTO `blocks` (hash, size) VALUES ("
        	    				+ "'" + prepare(block.hash, Constant.STRING) + "', "
        	    				+ block.size + ")";
        				Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager updateFile");
        				stmt.execute(sql);
    				}
    				
    				sql = "INSERT INTO `" + prepare(getTableName(filename, owner), Constant.TABLENAME) + "` "
		        			+ "(hash, size, blockseq) VALUES ("
		    				+ "'" + prepare(block.hash, Constant.STRING) + "', "
		    				+ block.size + ", "
		    				+ block.blockSeq + ")";
					Loger.Log(Constant.LOG_LEVEL_DEBUG, "excute query : " + sql, "DBManager updateFile");
    				stmt.execute(sql);
    			}
    			ret = true;
    		}
		}
		catch (SQLException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DBManager updateFile");
		}
    	
    	return ret;
    }
}
