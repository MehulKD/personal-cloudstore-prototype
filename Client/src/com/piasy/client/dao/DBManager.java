package com.piasy.client.dao;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.piasy.client.model.Constant;


/**
 * <p>This class is the database manager class,which encapsulate 
 * all the operation on the database, and provide a secure
 * way to operate the database.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:44 2013/12/17</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class DBManager
{
	private DBHelper helper;
	private SQLiteDatabase database;
	
	/**
	 * Constructor.
	 * @param application context
	 * */
	public DBManager(Context context)
	{
		helper = new DBHelper(context);
		database = helper.getWritableDatabase();
	}
	
	/**
	 * Query that whether the database is open
	 * @return whether the database is open
	 * */
	public boolean dbIsOpen()
	{
		return database.isOpen();
	}
	
	/**
	 * Open the database
	 * */
	public void openDB()
	{
		database = helper.getWritableDatabase();
	}
	
	/**
	 * Insert one record into the files table.
	 * @param the record to be inserted
	 * @return whether the insertion is successful
	 * */
	public boolean insertIntoFiles(FileEntry record)
	{
		boolean ret = false;
		ContentValues cv = new ContentValues();
		cv.put("cloudPath", record.cloudPath);
		cv.put("cloudDigest", record.cloudDigest);
		cv.put("localRecordTime", record.localRecordTime);
		cv.put("localStatus", record.localStatus);
		cv.put("localPath", record.localPath);
		cv.put("localModTime", record.localModTime);
		cv.put("localSize", record.localSize);
		cv.put("localDigest", record.localDigest);
		
		Log.d(Constant.LOG_LEVEL_DEBUG, "insert : recordtime = " + record.localRecordTime);
		
		database.beginTransaction();
		try
		{
			database.insert(Constant.DATABASE_FILE_TABLE_NAME, null, cv);
			database.setTransactionSuccessful();
			ret = true;
		} 
		finally
		{
			database.endTransaction();
		}
		show();
		return ret;
	}
	
	/**
	 * Delete one record from the files table.
	 * @param the record to be deleted
	 * @return whether the deletion is successful
	 * */
	public boolean deleteFromFiles(int id)
	{
		boolean ret = false;
		database.beginTransaction();
		try
		{
			database.delete(Constant.DATABASE_FILE_TABLE_NAME, "id = ?", new String[]{"" + id});
			database.setTransactionSuccessful();
			ret = true;
		} 
		finally
		{
			database.endTransaction();
		}
		show();
		return ret;
	}
	
	/**
	 * Update one record in the files table.
	 * @param the record to be updated
	 * @return whether the operation is successful
	 * */
	public boolean updateFiles(FileEntry record)
	{
		boolean ret = false;
		ContentValues cv = new ContentValues();
		cv.put("cloudPath", record.cloudPath);
		cv.put("cloudDigest", record.cloudDigest);
		cv.put("localRecordTime", record.localRecordTime);
		cv.put("localStatus", record.localStatus);
		cv.put("localPath", record.localPath);
		cv.put("localModTime", record.localModTime);
		cv.put("localSize", record.localSize);
		cv.put("localDigest", record.localDigest);
		
		database.beginTransaction();
		try
		{
			database.update(Constant.DATABASE_FILE_TABLE_NAME, cv, "id = ?", new String[]{"" + record.id});
			database.setTransactionSuccessful();
			ret = true;
		} 
		finally
		{
			database.endTransaction();
		}
		show();
		return ret;
	}
	
	/**
	 * Query all the records in the files table. TODO add query method with where clause
	 * @return all the records in the database
	 * */
	public ArrayList<FileEntry> queryFromFiles()
	{
		ArrayList<FileEntry> records = new ArrayList<FileEntry>();
		Cursor cursor = queryTheCursor(Constant.DATABASE_FILE_TABLE_NAME);
		while (cursor.moveToNext())
		{
			FileEntry record = new FileEntry(cursor.getInt(cursor.getColumnIndex("id")), 
											 cursor.getString(cursor.getColumnIndex("cloudPath")), 
											 cursor.getString(cursor.getColumnIndex("cloudDigest")), 
											 cursor.getLong(cursor.getColumnIndex("localRecordTime")), 
											 cursor.getInt(cursor.getColumnIndex("localStatus")), 
											 cursor.getString(cursor.getColumnIndex("localPath")), 
											 cursor.getLong(cursor.getColumnIndex("localModTime")), 
											 cursor.getLong(cursor.getColumnIndex("localSize")), 
											 cursor.getString(cursor.getColumnIndex("localDigest")));
			
			Log.d(Constant.LOG_LEVEL_DEBUG, "get : recordtime = " + cursor.getLong(cursor.getColumnIndex("localRecordTime")));
			Log.d(Constant.LOG_LEVEL_DEBUG, "return : recordtime = " + record.localRecordTime);
			records.add(record);
		}
		cursor.close();
		show(records, null);
		return records;
	}
	
	/**
	 * Insert one record into the blocks table.
	 * @param the record to be inserted
	 * @return whether the insertion is successful
	 * */
	public boolean insertIntoBlocks(BlockEntry record)
	{
		boolean ret = false;
		ContentValues cv = new ContentValues();
		cv.put("filename", record.filename);
		cv.put("status", record.status);
		cv.put("tag", record.tag);
		cv.put("offset", record.offset);
		cv.put("size", record.size);
		
		database.beginTransaction();
		try
		{
			database.insert(Constant.DATABASE_BLOCK_TABLE_NAME, null, cv);
			database.setTransactionSuccessful();
			ret = true;
		} 
		finally
		{
			database.endTransaction();
		}
		show();
		return ret;
	}
	
	/**
	 * Delete one record from the blocks table.
	 * @param the record to be deleted
	 * @return whether the deletion is successful
	 * */
	public boolean deleteFromBlocks(int id)
	{
		boolean ret = false;
		database.beginTransaction();
		try
		{
			database.delete(Constant.DATABASE_BLOCK_TABLE_NAME, "id = ?", new String[]{"" + id});
			database.setTransactionSuccessful();
			ret = true;
		} 
		finally
		{
			database.endTransaction();
		}
		show();
		return ret;
	}
	
	/**
	 * Update one record in the blocks table.
	 * @param the record to be updated
	 * @return whether the operation is successful
	 * */
	public boolean updateBlocks(BlockEntry record)
	{
		boolean ret = false;
		ContentValues cv = new ContentValues();
		cv.put("filename", record.filename);
		cv.put("status", record.status);
		cv.put("tag", record.tag);
		cv.put("offset", record.offset);
		cv.put("size", record.size);
		
		database.beginTransaction();
		try
		{
			database.update(Constant.DATABASE_BLOCK_TABLE_NAME, cv, "id = ?", new String[]{"" + record.id});
			database.setTransactionSuccessful();
			ret = true;
		} 
		finally
		{
			database.endTransaction();
		}
		show();
		return ret;
	}
	
	/**
	 * Query all the records in the blocks table.
	 * @return all the records in the database
	 * */
	public ArrayList<BlockEntry> queryFromBlocks()
	{
		ArrayList<BlockEntry> records = new ArrayList<BlockEntry>();
		Cursor cursor = queryTheCursor(Constant.DATABASE_BLOCK_TABLE_NAME);
		while (cursor.moveToNext())
		{
			BlockEntry record = new BlockEntry(cursor.getInt(cursor.getColumnIndex("id")), 
											   cursor.getString(cursor.getColumnIndex("filename")), 
											   cursor.getInt(cursor.getColumnIndex("status")), 
											   cursor.getInt(cursor.getColumnIndex("tag")), 
											   cursor.getInt(cursor.getColumnIndex("offset")), 
											   cursor.getInt(cursor.getColumnIndex("size")));
			records.add(record);
		}
		cursor.close();
		show(null, records);
		return records;
	}
	
	/**
	 * Get the cursor of one query.
	 * @return the cursor of one query
	 * */
	protected Cursor queryTheCursor(String table)
	{
		Cursor cursor = database.rawQuery("SELECT * FROM " + table, null);
		return cursor;
	}
	
	/**
	 * Close the database.
	 * */
	public void closeDB()
	{
		database.close();
	}
	
	private void show()
	{
		ArrayList<FileEntry> files = queryFromFiles();
		ArrayList<BlockEntry> blocks = queryFromBlocks();
		
		String logStr = "";
		logStr += "Files:\n";
		for (int i = 0; i < files.size(); i ++)
		{
			FileEntry file = files.get(i);
			logStr += file.id + " " + file.cloudPath + " " + file.cloudDigest + " " +
			file.localRecordTime + " " + file.localPath + " " + file.localDigest + " " + 
					file.localStatus + " " + file.localModTime + " " + file.localSize + "\n";
		}
		logStr += "Blocks:\n";
		for (int i = 0; i < blocks.size(); i ++)
		{
			BlockEntry block = blocks.get(i);
			logStr += block.id + " " + block.filename + " " + block.tag + " " +
					block.status + " " + block.offset + " " + block.size + "\n";
		}
		
		Log.d(Constant.LOG_LEVEL_DEBUG, logStr);
	}
	
	private void show(ArrayList<FileEntry> files, ArrayList<BlockEntry> blocks)
	{		
		String logStr = "";
		if (files != null)
		{
			logStr += "Files:\n";
			for (int i = 0; i < files.size(); i ++)
			{
				FileEntry file = files.get(i);
				logStr += file.id + " " + file.cloudPath + " " + file.cloudDigest + " " +
				file.localRecordTime + " " + file.localPath + " " + file.localDigest + " " + 
						file.localStatus + " " + file.localModTime + " " + file.localSize + "\n";
			}
		}
		
		if (blocks != null)
		{
			logStr += "Blocks:\n";
			for (int i = 0; i < blocks.size(); i ++)
			{
				BlockEntry block = blocks.get(i);
				logStr += block.id + " " + block.filename + " " + block.tag + " " +
						block.status + " " + block.offset + " " + block.size + "\n";
			}
		}
		
		Log.d(Constant.LOG_LEVEL_DEBUG, logStr);
	}
}
