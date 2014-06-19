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
	public static final String APP_BASE_DIR = Environment.getExternalStorageDirectory().getPath() + "/CloudDir";
	public static final String TMP_DIR = APP_BASE_DIR + "/.tmp";
	public static final String LOG_FILE_NAME = APP_BASE_DIR + "/log/test.log.txt";
	
	/**
	 * Constant for raptor code
	 * */
	public static final int K_NUM = 600;
	public static final int MIN_SYMBOL_SIZE = 128;
	public static final int SYMBOL_SIZE = 1024;
	public static final double OVERHEAD = 0.02;
	public static final int UDP_FAST_INTERVAL = 15;	//ms
	public static final int UDP_SLOW_INTERVAL = 20;	//ms
	public static final int PKT_SIZE = 1500;	//byte
	public static final int CODING_THREAD_WAIT_TIME = 2 * 1000;
	public static final int CODING_THREAD_COUNT = 5;
	public static final int CODING_STATE_READY = 0;
	public static final int CODING_STATE_ENCODE_START = 1;
	public static final int CODING_STATE_ENCODE_END = 2;
	public static final int CODING_STATE_SEND_START = 3;
	public static final int CODING_STATE_SEND_END = 4;
	
	public static final int STATIC_BLOCK_SIZE = 4 * 1024 * 1024;
	

	/**
	 * Constant for file transfer
	 * */
	public static final int REQUEST_TYPE_QUERY = 1;
	public static final int REQUEST_TYPE_UPLOAD = 2;
	public static final int REQUEST_TYPE_DOWNLOAD = 3;
	public static final int REQUEST_TYPE_EXIT = 100;
	public static final int TRANSFER_MODE_NONE = 0;
	public static final int TRANSFER_MODE_UPLOAD = REQUEST_TYPE_UPLOAD;
	public static final int TRANSFER_MODE_DOWNLOAD = REQUEST_TYPE_DOWNLOAD;

	public static final int CLOSE_SOCKET_TIMEOUT = 5 * 60 * 1000;
	public static final int SO_SOCKET_TIMEOUT = 5 * 60 * 1000;

	/**
	 * Constant for CDC
	 * */
	public static final int CDC_WINDOW_SIZE = 48;
	public static final int SLID_WINDOW_STEP = 8;
	public static final int CDC_MAGIC = 0x00000247;
	public static final int CDC_MASK = 0x000007ff;
	public static final int CDC_BUFFER_SIZE = CDC_WINDOW_SIZE * CDC_WINDOW_SIZE * 8;
	public static final int BUFFER_SIZE = 4 * 1024 * 1024;
	public static final int MAX_BLOCK_SIZE = 4 * 1024 * 1024;
	public static final int MIN_BLOCK_SIZE = 0;
	

	public static final int CHANNEL_POOL_SIZE = 20;
	public static final int DATACHANNEL_ACTIVE = 1;
	public static final int DATACHANNEL_WAIT = 2;
	public static final int DATACHANNEL_DEAD = 3;
	
}
