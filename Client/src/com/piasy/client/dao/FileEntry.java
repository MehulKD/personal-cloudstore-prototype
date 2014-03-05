package com.piasy.client.dao;

/**
 * <p>The DAO entry class, the entry of table 'files'.
 * It has 9 field, id(primary key), cloudPath, cloudDigest, localRecordTime,
 * localStatus, localPath, localModTime, localSize, and localDigest.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		21:25 2013/12/16</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class FileEntry
{
	public int id;
	public String cloudPath;
	public String cloudDigest;
	public long localRecordTime;
	public long localStatus;
	public String localPath;
	public long localModTime;
	public long localSize;
	public String localDigest;
	
	/**
	 * The default public constructor.
	 * */
	public FileEntry()
	{
		
	}
	
	/**
	 * Another public constructor.
	 * */
	public FileEntry(String cloudPath, String cloudDigest, long localRecordTime,
			long localStatus, String localPath, long localModTime,
			long localSize, String localDigest)
	{
		this.cloudPath = cloudPath;
		this.cloudDigest = cloudDigest;
		this.localRecordTime = localRecordTime;
		this.localStatus = localStatus;
		this.localPath = localPath;
		this.localModTime = localModTime;
		this.localSize = localSize;
		this.localDigest = localDigest;
	}

	/**
	 * Another public constructor with id.
	 * */
	public FileEntry(int id, String cloudPath, String cloudDigest, long localRecordTime,
			long localStatus, String localPath, long localModTime,
			long localSize, String localDigest)
	{
		this.id = id;
		this.cloudPath = cloudPath;
		this.cloudDigest = cloudDigest;
		this.localRecordTime = localRecordTime;
		this.localStatus = localStatus;
		this.localPath = localPath;
		this.localModTime = localModTime;
		this.localSize = localSize;
		this.localDigest = localDigest;
	}
}
