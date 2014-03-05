package com.piasy.client.model;

public class RsyncModel
{
	
	public static native int genSignature(String oldFileName, String sigFileName);
	
	public static native int genDelta(String sigFileName, String newFileName, String deltaFileName);
	
	public static native int patch(String oldFileName, String deltaFileName, String newFileName);
	
	static
	{
		System.loadLibrary("rsync_util");
	}
}
