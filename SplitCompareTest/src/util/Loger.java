package util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Loger
{
	public static void Log(String level, String message, String callee)
	{
		String timeString = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println(level + " " + timeString + " " + System.currentTimeMillis() + 
						   " at `" + callee + "` log : " + message);
	}
}
