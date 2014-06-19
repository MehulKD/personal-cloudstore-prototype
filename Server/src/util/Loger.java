package util;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import model.Constant;

/**
 * <p>The logger utility class, log info with different level, and time stamp.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		14:13 2013/12/15</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class Loger
{
	static PrintStream out;
	static int level;
	public static void init(PrintStream output, int l)
	{
		out = output;
		level = l;
	}
	
	protected static int getLevel(String sl)
	{
		if (sl.equals(Constant.LOG_LEVEL_INFO))
		{
			return Constant.LEVEL_INFO;
		}
		else if (sl.equals(Constant.LOG_LEVEL_DEBUG))
		{
			return Constant.LEVEL_DEBUG;
		}
		else if (sl.equals(Constant.LOG_LEVEL_WARNING))
		{
			return Constant.LEVEL_WARNING;
		}
		else if (sl.equals(Constant.LOG_LEVEL_ERROR))
		{
			return Constant.LEVEL_ERROR;
		}
		else if (sl.equals(Constant.LOG_LEVEL_FATAL))
		{
			return Constant.LEVEL_FATAL;
		}
		else
		{
			return Constant.LEVEL_INFO;
		}
	}
	
	public static void Log(String sl, String message, String callee)
	{
		if (level <= getLevel(sl))
		{
			String timeString = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
			System.out.println(sl + " " + timeString + " " + System.currentTimeMillis() + 
						   " at `" + callee + "` log : " + message);
		}
	}
}
