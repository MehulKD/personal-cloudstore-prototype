package model;

import java.net.DatagramSocket;

/**
 * <p>This is datagram thread pool item class. It encapsulates the datagram socket
 * and decoded data.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		9:11 2014/5/5</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class DGSockPoolItem
{
	public DatagramSocket dgsock;
	public boolean active = false;
	public byte[] mixed = null;
	
	public DGSockPoolItem(DatagramSocket sock)
	{
		dgsock = sock;
	}
}
