package com.piasy.client.model;

/**
 * <p>The class that encapsulates coding data and config.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		10:26 2014/5/5</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class CodingRawData
{
	public byte [] raw;
	public raptor.util.Config conf;
	
	public CodingRawData(byte [] data, raptor.util.Config conf)
	{
		this.raw = data;
		this.conf = conf;
	}
}
