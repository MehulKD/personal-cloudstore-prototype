package model;

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
	/**
	 * Constant for logger
	 * */
	public static final String LOG_LEVEL_DEBUG = "DEBUG";
	public static final String LOG_LEVEL_INFO = "INFO";
	public static final String LOG_LEVEL_WARNING = "WARNING";
	public static final String LOG_LEVEL_ERROR = "ERROR";
	public static final String LOG_LEVEL_FATAL = "FATAL";
	public static final int LEVEL_DEBUG = 1;
	public static final int LEVEL_INFO = 2;
	public static final int LEVEL_WARNING = 3;
	public static final int LEVEL_ERROR = 4;
	public static final int LEVEL_FATAL = 5;
	
	/**
	 * Constant for raptor code
	 * */
	public static final int K_NUM = 600;
	public static final int MIN_SYMBOL_SIZE = 128;
	public static final int SYMBOL_SIZE = 1024;
	public static final double OVERHEAD = 0.02;
	public static final int UDP_FAST_INTERVAL = 20;	//ms
	public static final int UDP_SLOW_INTERVAL = 100;	//ms
	public static final int PKT_SIZE = 1500;	//byte
	public static final int DGSOCK_POOL_SIZE = 5;

	/**
	 * Constant for DBManager
	 * */
	public static final String TABLE_COLUMN_ID = "id";
	public static final String TABLE_COLUMN_OWNER = "owner";
	public static final String TABLE_COLUMN_PATH = "path";
	public static final String TABLE_COLUMN_DIGEST = "digest";
	public static final int STRING = 1;
	public static final int TABLENAME = 2;
	public static final long DB_CON_KEEPALIVE_INTERVAL = 60 * 60 * 1000;

	/**
	 * Constant for data transfer
	 * */
	public static final int REQUEST_TYPE_QUERY = 1;
	public static final int REQUEST_TYPE_UPLOAD = 2;
	public static final int REQUEST_TYPE_DOWNLOAD = 3;
	public static final int REQUEST_TYPE_ACK = 6;
	public static final int TRANSFER_MODE_NONE = -1;
	public static final int TRANSFER_MODE_UPLOAD = 1;
	public static final int TRANSFER_MODE_DOWNLOAD = 2;
	public static final int SO_SOCKET_TIMEOUT = 5 * 60 * 1000;
	public static final int DATA_CHANNEL_ACCEPT_TIMEOUT = 30 * 1000;

	/**
	 * Constant for CDC(content defined chunking)
	 * */
	public static final int CDC_WINDOW_SIZE = 48;
	public static final int SLID_WINDOW_STEP = 8;
	public static final int CDC_MAGIC = 0x00000847;
	public static final int CDC_MASK = 0x00000fff;
	public static final int CDC_BUFFER_SIZE = CDC_WINDOW_SIZE * 30;
	public static final int BUFFER_SIZE = 4 * 1024 * 1024;
	public static final int MAX_BLOCK_SIZE = 4 * 1024 * 1024;
	public static final int MIN_BLOCK_SIZE = 128 * 1024;
	
}
