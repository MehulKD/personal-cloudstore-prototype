package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.json.JSONException;
import org.json.JSONObject;

import raptor.util.Config;
import raptor.util.Receiver;
import util.Loger;

/**
 * <p>A thread class which receive UDP packets form client and decode them.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		9:00 2014/5/5</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class ReceivingThread extends Thread
{
	
	DGSockPoolItem item;
	JSONObject conf;
	int blockseq;
	Socket ctrSocket;
	public ReceivingThread(DGSockPoolItem item, JSONObject conf, int blockseq, Socket ctrSocket)
	{
		this.item = item;
		this.conf = conf;
		this.blockseq = blockseq;
		this.ctrSocket = ctrSocket;
	}
	
	@Override
	public void run()
	{
		try
		{
			item.mixed = null;
			item.mixed = decodeOneBlock(item.dgsock, conf, blockseq);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		item.active = false;
	}
	

	/**
	 * Receive UDP packets form client and decode them
	 * */
	protected byte [] decodeOneBlock(DatagramSocket dgserver, JSONObject conf, int blockSeq) throws JSONException, IOException
	{
		ByteBuffer pktBf = ByteBuffer.allocate(Constant.PKT_SIZE);
		int K = conf.getInt("K");
		int Pr = (int) (1.25 * K);
		int SYMSIZE = conf.getInt("SYMSIZE");
		int bsize = conf.getInt("bsize");
		
		byte [][] data = new byte[Pr][SYMSIZE];
		boolean [] received = new boolean[Pr];
		for (int i = 0; i < Pr; i ++)
		{
			received[i] = false;
		}
		
		byte [] buf = new byte[Constant.PKT_SIZE];
		
		long start1 = System.currentTimeMillis();
		int need = (int) ((Constant.OVERHEAD + 1) * K);
		int count = 0;
		
		InetAddress clientAddr = null;
		while (count < need)
		{
			pktBf.clear();
			DatagramPacket pkt = new DatagramPacket(buf, Constant.PKT_SIZE);
			dgserver.receive(pkt);
			if (clientAddr == null)
			{
				clientAddr = pkt.getAddress();
			}
			//TODO client identify?
			
			int rcvlen = pkt.getLength();
			pktBf.put(pkt.getData(), 0, rcvlen);
			byte [] all = pktBf.array();
			int offset = 0;
			int bseq = pktBf.getInt(offset);
			offset += 4;
			if (bseq != blockSeq)
			{//just drop this packet
				continue;
			}
			int symNum = pktBf.getInt(offset);
			offset += 4;
			for (int i = 0; i < symNum; i ++)
			{
				int index = pktBf.getInt(offset);
				offset += 4;
				if (0 <= index && index < Pr)
				{
					System.arraycopy(all, offset, data[index], 0, SYMSIZE);
					received[index] = true;
				}
				offset += SYMSIZE;
			}
			count += symNum;
		}
			
		Loger.Log(Constant.LOG_LEVEL_INFO, "Receive time: " + 
				(System.currentTimeMillis() - start1) + " ms", "receiving thread");
		
		long start2 = System.currentTimeMillis();
		int [] index = new int[count];
		int [] index1 = new int[Pr];
		int pos = 0;
		for (int i = 0; i < Pr; i ++)
		{
			if (received[i])
			{
				index1[pos] = i;
				pos ++;
			}
		}
		for (int i = 0; i < count; i ++)
		{
			index[i] = index1[i];
		}
		
		byte [] raw = Receiver.decode(bsize, new Config(SYMSIZE, K, Constant.OVERHEAD), data, index);

		Loger.Log(Constant.LOG_LEVEL_INFO, "Decode time: " + 
				(System.currentTimeMillis() - start2) + " ms", "receiving thread");

		JSONObject reply = new JSONObject();
		reply.put("status", "success");
		
		PrintWriter writer = new PrintWriter(ctrSocket.getOutputStream());
		writer.println(reply.toString());
		writer.flush();
		
		Loger.Log(Constant.LOG_LEVEL_INFO, "send: " + 
				reply.toString(), "receiving thread");
		
		return raw;
	}
	
}
