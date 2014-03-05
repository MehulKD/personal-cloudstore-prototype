package util;

public class Block
{
	public String filepath;
	public long offset;
	public long size;
	public String hash;
	public boolean redundant = false;
	
	public Block(String filepath, long offset, long size, String hash)
	{
		this.filepath = filepath;
		this.offset = offset;
		this.size = size;
		this.hash = hash;
	}
}
