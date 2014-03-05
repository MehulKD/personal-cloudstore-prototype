package com.piasy.client.model;


/**
 * <p>The class that encapsulates the access to file.
 * It is the basic unit transferred in data channel.
 * It records its own information, including tag, size, 
 * whether has been acknowledged.</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:54 2013/12/22</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class Block
{
	public long offset, size;
	public String filename, hash;
	public int blockSeq = -1;
	public boolean finished = false;
	
	public Block(String filename, long size, long offset, int blockSeq, String hash)
	{
		this.size = size;
		this.offset = offset;
		this.filename = filename;
		this.blockSeq = blockSeq;
		this.hash = hash;
	}
}
