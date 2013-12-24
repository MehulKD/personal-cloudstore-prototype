package com.piasy.client.dao;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
	 * Insert one record into the database.
	 * @param the record to be inserted
	 * @return whether the insertion is successful
	 * */
	public boolean insert(FileEntry record)
	{
		boolean ret = false;
		ContentValues cv = new ContentValues();
		cv.put("cloudPath", record.cloudPath);
		cv.put("cloudDigest", record.cloudDigest);
		cv.put("localRecordTime", record.localRecordTime);
		cv.put("localStatus", record.localStatus);
		cv.put("transferSize", record.transferSize);
		cv.put("localPath", record.localPath);
		cv.put("localModTime", record.localModTime);
		cv.put("localSize", record.localSize);
		cv.put("localDigest", record.localDigest);
		
		database.beginTransaction();
		try
		{
			database.insert(Constant.DATABASE_TABLE_NAME, null, cv);
			database.setTransactionSuccessful();
			ret = true;
		} 
		finally
		{
			database.endTransaction();
		}
		
		return ret;
	}
	
	/**
	 * Delete one record from the database.
	 * @param the record to be deleted
	 * @return whether the deletion is successful
	 * */
	public boolean delete(int id)
	{
		boolean ret = false;
		database.beginTransaction();
		try
		{
			database.delete(Constant.DATABASE_TABLE_NAME, "id = ?", new String[]{"" + id});
			database.setTransactionSuccessful();
			ret = true;
		} 
		finally
		{
			database.endTransaction();
		}
		return ret;
	}
	
	/**
	 * Update one record in the database.
	 * @param the record to be updated
	 * @return whether the operation is successful
	 * */
	public boolean update(FileEntry record)
	{
		boolean ret = false;
		ContentValues cv = new ContentValues();
		cv.put("cloudPath", record.cloudPath);
		cv.put("cloudDigest", record.cloudDigest);
		cv.put("localRecordTime", record.localRecordTime);
		cv.put("localStatus", record.localStatus);
		cv.put("transferSize", record.transferSize);
		cv.put("localPath", record.localPath);
		cv.put("localModTime", record.localModTime);
		cv.put("localSize", record.localSize);
		cv.put("localDigest", record.localDigest);
		
		database.beginTransaction();
		try
		{
			database.update(Constant.DATABASE_TABLE_NAME, cv, "id = ?", new String[]{"" + record.id});
			database.setTransactionSuccessful();
			ret = true;
		} 
		finally
		{
			database.endTransaction();
		}
		return ret;
	}
	
	/**
	 * Query all the records in the database. TODO add query method with where clause
	 * @return all the records in the database
	 * */
	public ArrayList<FileEntry> query()
	{
		ArrayList<FileEntry> records = new ArrayList<FileEntry>();
		Cursor cursor = queryTheCursor();
		while (cursor.moveToNext())
		{
			FileEntry record = new FileEntry(cursor.getInt(cursor.getColumnIndex("id")), 
											 cursor.getString(cursor.getColumnIndex("cloudPath")), 
											 cursor.getString(cursor.getColumnIndex("cloudDigest")), 
											 cursor.getInt(cursor.getColumnIndex("localRecordTime")), 
											 cursor.getInt(cursor.getColumnIndex("localStatus")), 
											 cursor.getInt(cursor.getColumnIndex("transferSize")),
											 cursor.getString(cursor.getColumnIndex("localPath")), 
											 cursor.getInt(cursor.getColumnIndex("localModTime")), 
											 cursor.getInt(cursor.getColumnIndex("localSize")), 
											 cursor.getString(cursor.getColumnIndex("localDigest")));
			records.add(record);
		}
		cursor.close();
		return records;
	}
	
	/**
	 * Get the cursor of one query.
	 * @return the cursor of one query
	 * */
	protected Cursor queryTheCursor()
	{
		Cursor cursor = database.rawQuery("SELECT * FROM " + Constant.DATABASE_TABLE_NAME, null);
		return cursor;
	}
	
	/**
	 * Close the database.
	 * */
	public void closeDB()
	{
		database.close();
	}
}
