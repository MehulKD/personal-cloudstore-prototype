package com.piasy.client.model;

/**
 * <p>The class that encapsulates CDC config.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		10:26 2014/5/5</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class Config
{
	public int magic, mask, min, max, step;
	public Config(int magic, int mask, int min, int max, int step)
	{
		this.magic = magic;
		this.mask = mask;
		this.min = min;
		this.max = max;
		this.step = step;
	}
}
