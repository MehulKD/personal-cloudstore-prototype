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
	private long id;
	private String owner;
	private String path;
	private String digest;
	
	/**
	 * The default public constructor.
	 * */
	public FileEntry()
	{
		
	}
	
	/**
	 * Another public constructor.
	 * */
	public FileEntry(String owner, String path, String digest)
	{
		this.owner = owner;
		this.path = path;
		this.digest = digest;
	}
	
	/**
	 * The public set method of id filed.
	 * @param the id
	 * */
	public void setId(long id)
	{
		this.id = id;
	}
	
	/**
	 * The public get method of id filed.
	 * @return the id
	 * */
	public long getId()
	{
		return this.id;
	}
	
	/**
	 * The public set method of owner filed.
	 * @param the owner
	 * */
	public void setOwner(String owner)
	{
		this.owner = owner;
	}
	
	/**
	 * The public get method of owner filed.
	 * @return the owner
	 * */
	public String getOwner()
	{
		return this.owner;
	}
	
	/**
	 * The public set method of path filed.
	 * @param the path
	 * */
	public void setPath(String path)
	{
		this.path = path;
	}
	
	/**
	 * The public get method of path filed.
	 * @return the path
	 * */
	public String getPath()
	{
		return this.path;
	}
	
	/**
	 * The public set method of digest filed.
	 * @param the digest
	 * */
	public void setDigest(String digest)
	{
		this.digest = digest;
	}
	
	/**
	 * The public get method digest id filed.
	 * @return the digest
	 * */
	public String getDigest()
	{
		return this.digest;
	}
}
