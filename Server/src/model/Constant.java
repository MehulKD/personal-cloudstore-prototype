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
	public static final String LOG_LEVEL_INFO = "INFO";
	public static final String LOG_LEVEL_DEBUG = "DEBUG";
	public static final String LOG_LEVEL_WARNING = "WARNING";
	public static final String LOG_LEVEL_ERROR = "ERROR";
	public static final String LOG_LEVEL_FATAL = "FATAL";
	
	public static final String TABLE_COLUMN_ID = "id";
	public static final String TABLE_COLUMN_OWNER = "owner";
	public static final String TABLE_COLUMN_PATH = "path";
	public static final String TABLE_COLUMN_DIGEST = "digest";
	
	public static final int TRANSFER_MODE_NONE = -1;
	public static final int TRANSFER_MODE_UPLOAD = 1;
	public static final int TRANSFER_MODE_DOWNLOAD = 2;
	public static final int TRANSFER_MODE_UPDATE_UP = 3;
	public static final int TRANSFER_MODE_UPDATE_DOWN = 4;
	
	public static final int BUFFER_SIZE = 8 * 1024;
	
	public static final int DATA_CHANNEL_ACCEPT_TIMEOUT = 30 * 1000;
}
