package model;

public class RsyncModel
{

	static
	{
		try
		{
//			System.out.println(System.getProperty("java.library.path"));;
//			System.loadLibrary("rsync_util");
			System.load("/home/ubuntu/src/dropbox-like-app/Server/jni/librsync_util.so");
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
		
	}
	
	public static native int genSignature(String oldFileName, String sigFileName);
	
	public static native int genDelta(String sigFileName, String newFileName, String deltaFileName);
	
	public static native int patch(String oldFileName, String deltaFileName, String newFileName);
}
