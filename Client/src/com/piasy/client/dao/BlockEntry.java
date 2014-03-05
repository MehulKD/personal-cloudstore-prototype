package com.piasy.client.dao;

/**
 * <p>The DAO entry class, the entry of table 'blocks'.
 * It has 6 field, id(primary key), filename, status, tag,
 * offset, size.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:54 2013/12/24</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class BlockEntry
{
	public int id;
	public String filename;
	public long status;
	public long tag;
	public long offset;
	public long size;
	
	/**
	 * The default public constructor.
	 * */
	public BlockEntry()
	{
		
	}
	
	/**
	 * Another public constructor.
	 * */
	public BlockEntry(String filename, long status, long tag, long offset, long size)
	{
		this.filename = filename;
		this.status = status;
		this.tag = tag;
		this.offset = offset;
		this.size = size;
	}

	/**
	 * Another public constructor with id.
	 * */
	public BlockEntry(int id, String filename, long status, long tag, long offset, long size)
	{
		this.id = id;
		this.filename = filename;
		this.status = status;
		this.tag = tag;
		this.offset = offset;
		this.size = size;
	}
}
