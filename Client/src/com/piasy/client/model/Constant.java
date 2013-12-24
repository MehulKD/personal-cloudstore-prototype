package com.piasy.client.model;

/**
 * <p>Constants will be used in the program.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:54 2013/12/22</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class Constant
{
	public static final String LOG_LEVEL_INFO = "INFO";
	public static final String LOG_LEVEL_DEBUG = "DEBUG";
	public static final String LOG_LEVEL_WARNING = "WARNING";
	public static final String LOG_LEVEL_ERROR = "ERROR";
	public static final String LOG_LEVEL_FATAL = "FATAL";
	
	public static final String DATABASE_TABLE_NAME = "files";
	
	public static final int BUFFER_SIZE = 8 * 1024;
	
	public static final int CHANNEL_POOL_SIZE = 20;
	public static final int BLOCK_SIZE = 4 * 1024 * 1024;
	
	public static final int TRANSFER_MODE_NONE = 0;
	public static final int TRANSFER_MODE_UPLOAD = TRANSFER_MODE_NONE + 1;
	public static final int TRANSFER_MODE_DOWNLOAD = TRANSFER_MODE_UPLOAD + 1;
	public static final int TRANSFER_MODE_UPDATE_UP = TRANSFER_MODE_DOWNLOAD + 1;
	public static final int TRANSFER_MODE_UPDATE_DOWN = TRANSFER_MODE_UPDATE_UP + 1;
	
	public static final int FILE_STATUS_NONEXIST = 1;
	public static final int FILE_STATUS_OLDER = FILE_STATUS_NONEXIST + 1;
	public static final int FILE_STATUS_SYNCED = FILE_STATUS_OLDER + 1;
	public static final int FILE_STATUS_UPLOAD_PARTLY = FILE_STATUS_SYNCED + 1;
	public static final int FILE_STATUS_DOWNLOAD_PARTLY = FILE_STATUS_UPLOAD_PARTLY + 1;
	public static final int FILE_STATUS_NEWER = FILE_STATUS_DOWNLOAD_PARTLY + 1;
}
