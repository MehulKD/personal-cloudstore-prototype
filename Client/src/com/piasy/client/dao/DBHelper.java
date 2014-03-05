package com.piasy.client.dao;

import com.piasy.client.model.Constant;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * <p>This class is the database helper class in the SQLite mechanism.
 * It create the table at the creation of this application if it
 * does not exist.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:44 2013/12/17</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class DBHelper extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "FILES.db";
	private static final int DATABASE_VERSION = 1;
	
	/**
	 * Constructor.
	 * @param the application context.
	 * */
	public DBHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	/**
	 * Another constructor.
	 * @param
	 * application context,
	 * database name,
	 * cursor factory,
	 * version
	 * */
	public DBHelper(Context context, String name, CursorFactory factory,
			int version)
	{
		super(context, name, factory, version);
	}

	/**
	 * create method, create the table `files` if not exist.
	 * @param SQLite database
	 * */
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Constant.DATABASE_FILE_TABLE_NAME +  
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, cloudPath TEXT, cloudDigest TEXT, localRecordTime INT8, localStatus INTEGER, " + 
				"localPath TEXT, localModTime INT8, localSize INT8, localDigest TEXT)"); 
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Constant.DATABASE_BLOCK_TABLE_NAME +  
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, filename TEXT, status INTEGER, tag INTEGER, offset INTEGER, size INTEGER)"); 
	}

	/**
	 * upgrade method, do something if necessary.
	 * @param 
	 * SQLite database,
	 * old version,
	 * new version
	 * */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		  
	}

}
