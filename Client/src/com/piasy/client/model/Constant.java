package com.piasy.client.model;

import android.os.Environment;

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
	
	public static final String APP_BASE_DIR = Environment.getExternalStorageDirectory().getPath() + "/CloudDir";
	public static final String TMP_DIR = APP_BASE_DIR + "/.tmp";
	public static final String LOG_FILE_NAME = APP_BASE_DIR + "/log/test.log";
	
	public static final String DATABASE_FILE_TABLE_NAME = "files";
	public static final String DATABASE_BLOCK_TABLE_NAME = "blocks";
	
	public static final int CHANNEL_POOL_SIZE = 20;
	public static final int BLOCK_SIZE = 4 * 1024 * 1024;
	
	public static final int FILE_STATUS_NONEXIST = 1;
	public static final int FILE_STATUS_OLDER = FILE_STATUS_NONEXIST + 1;
	public static final int FILE_STATUS_SYNCED = FILE_STATUS_OLDER + 1;
	public static final int FILE_STATUS_UPLOAD_PARTLY = FILE_STATUS_SYNCED + 1;
	public static final int FILE_STATUS_DOWNLOAD_PARTLY = FILE_STATUS_UPLOAD_PARTLY + 1;
	public static final int FILE_STATUS_NEWER = FILE_STATUS_DOWNLOAD_PARTLY + 1;
	
	public static final int BLOCK_UPLOAD_UNACKED = 1;
	public static final int BLOCK_UPLOAD_ACKED = BLOCK_UPLOAD_UNACKED + 1;
	public static final int BLOCK_DOWNLOAD_UNACKED = BLOCK_UPLOAD_ACKED + 1;
	public static final int BLOCK_DOWNLOAD_ACKED = BLOCK_DOWNLOAD_UNACKED + 1;
	public static final int BLOCK_UPDATE_UP_UNACKED = BLOCK_DOWNLOAD_ACKED + 1;
	public static final int BLOCK_UPDATE_UP_ACKED = BLOCK_UPDATE_UP_UNACKED + 1;
	public static final int BLOCK_UPDATE_DOWN_UNACKED = BLOCK_UPDATE_UP_ACKED + 1;
	public static final int BLOCK_UPDATE_DOWN_ACKED = BLOCK_UPDATE_DOWN_UNACKED + 1;
	
	public static final int REQUEST_TYPE_QUERY = 1;
	public static final int REQUEST_TYPE_UPLOAD = 2;
	public static final int REQUEST_TYPE_DOWNLOAD = 3;
	public static final int REQUEST_TYPE_UPDATE_UP = 4;
	public static final int REQUEST_TYPE_UPDATE_DOWN = 5;
	public static final int REQUEST_TYPE_PURE_UPLOAD = 6;
	public static final int REQUEST_TYPE_PURE_DOWNLOAD = 7;
	public static final int REQUEST_TYPE_UPDATE_UP_FAST = 8;
	public static final int REQUEST_TYPE_EXIT = 100;
	
	public static final int TRANSFER_MODE_NONE = 0;
	public static final int TRANSFER_MODE_UPLOAD = REQUEST_TYPE_UPLOAD;
	public static final int TRANSFER_MODE_DOWNLOAD = REQUEST_TYPE_DOWNLOAD;
	public static final int TRANSFER_MODE_UPDATE_UP = REQUEST_TYPE_UPDATE_UP;
	public static final int TRANSFER_MODE_UPDATE_DOWN = REQUEST_TYPE_UPDATE_DOWN;
	public static final int TRANSFER_MODE_PURE_UPLOAD = REQUEST_TYPE_PURE_UPLOAD;
	public static final int TRANSFER_MODE_PURE_DOWNLOAD = REQUEST_TYPE_PURE_DOWNLOAD;
	public static final int TRANSFER_MODE_UPDATE_UP_FAST = REQUEST_TYPE_UPDATE_UP_FAST;
	
	public static final int CDC_WINDOW_SIZE = 48;
	public static final int SLID_WINDOW_STEP = 8;
	public static final int CDC_MAGIC = 0x00000847;
	public static final int CDC_MASK = 0x00000fff;
	public static final int BUFFER_SIZE = CDC_WINDOW_SIZE * CDC_WINDOW_SIZE * 20;
	public static final int MAX_BLOCK_SIZE = 4 * 1024 * 1024;
	public static final int MIN_BLOCK_SIZE = 128 * 1024;
	
	public static final int CLOSE_SOCKET_TIMEOUT = 5 * 60 * 1000;
	
	public static final int DATACHANNEL_ACTIVE = 1;
	public static final int DATACHANNEL_WAIT = 2;
	public static final int DATACHANNEL_DEAD = 3;
	
	public static final double MODIFY_P = 0.005;
}
