package dao;

/**
 * <p>The DAO entry class, the entry of table 'files'.
 * It has 4 field, id(primary key), owner, path, and digest.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		14:13 2013/12/15</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class FileEntry
{
	public long id;
	public String filename;
	public long size;
	public int blockNum;
	public String owner;
	
	/**
	 * Another public constructor.
	 * */
	public FileEntry(String filename, long size, int blockNum, String owner)
	{
		this.filename = filename;
		this.size = size;
		this.blockNum = blockNum;
		this.owner = owner;
	}
	
	/**
	 * Another public constructor.
	 * */
	public FileEntry(long id, String filename, long size, int blockNum, String owner)
	{
		this.id = id;
		this.filename = filename;
		this.size = size;
		this.blockNum = blockNum;
		this.owner = owner;
	}
}
