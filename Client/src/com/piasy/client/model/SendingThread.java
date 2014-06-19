package com.piasy.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import raptor.util.EncodeResult;
import raptor.util.Sender;

/**
 * <p>A thread class which encodes data and sends UDP packets to server.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		9:00 2014/5/5</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class SendingThread extends Thread
{
	CodingRawData data;
	InetAddress addr;
	int datagramPort;
	int blockseq;
	Socket ctrSocket;
	public SendingThread(CodingRawData data, InetAddress addr, int datagramPort, int blockseq, Socket ctr)
	{
		this.data = data;
		this.addr = addr;
		this.datagramPort = datagramPort;
		this.blockseq = blockseq;
		ctrSocket = ctr;
	}
	
	int state = Constant.CODING_STATE_READY;
	/**
	 * Get the thread state
	 * */
	public int state()
	{
		return state;
	}
	
	EncodeResult result;
	long encodeTime;
	
	@Override
	public void run()
	{
		state = Constant.CODING_STATE_ENCODE_START;
		try
		{
			dgclient = new DatagramSocket();
			long start1 = System.currentTimeMillis();
			System.out.println("Encode start");
			result = Sender.encode(data.raw, 0, data.raw.length, data.conf);
			System.out.println("Encode end");
			encodeTime = System.currentTimeMillis() - start1;
			state = Constant.CODING_STATE_ENCODE_END;

			state = Constant.CODING_STATE_SEND_START;
			start1 = System.currentTimeMillis();
			System.out.println("send start");
			if (send(result, data.raw.length, blockseq))
			{
				//send success
				System.out.println("send end");
				long sendTime = System.currentTimeMillis() - start1;
				System.out.println("Encode time: " + encodeTime + ", send time: " + sendTime + ", wait time : " + waitTime);
			}
		}
		catch (SocketException e1)
		{
			e1.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		state = Constant.CODING_STATE_SEND_END;
	}
	
	
	long waitTime = 0;
	int pcount = 0;
	int count = 0;
	ByteBuffer pktBf = ByteBuffer.allocate(Constant.PKT_SIZE);
	DatagramSocket dgclient;
	/**
	 * Send a coding block
	 * */
	protected boolean send(final EncodeResult result, int blockSize, final int blockSeq) throws JSONException
	{
		boolean ret = false;
		try
		{
			count = 0;
			int need = (int) ((Constant.OVERHEAD + 1) * result.conf.K);
			final int Pr = (int) (1.25 * result.conf.K);
			
			while (count < need)
			{
				pktBf.clear();
				int len = 0;
				int symNum = (Constant.PKT_SIZE - 4) / (result.conf.SYMBOL_SIZE + 4);
				pktBf.putInt(blockSeq);
				len += 4;
				pktBf.putInt(symNum);
				len += 4;
				for (int i = 0; i < symNum; i ++)
				{
					pktBf.putInt(count);
					pktBf.put(result.data[count], 0, result.conf.SYMBOL_SIZE);
					count ++;
					len += 4 + result.conf.SYMBOL_SIZE;
				}
				
				DatagramPacket pkt = new DatagramPacket(pktBf.array(), len, addr, datagramPort);
				dgclient.send(pkt);
				try
				{
					Thread.sleep(Constant.UDP_FAST_INTERVAL);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			Timer timer = new Timer();
			TimerTask slowTask = new TimerTask()
			{
				
				@Override
				public void run()
				{
					if (count < Pr)
					{
						pktBf.clear();
						int len = 0;
						int symNum = (Constant.PKT_SIZE - 4) / (result.conf.SYMBOL_SIZE + 4);

						pktBf.putInt(blockSeq);
						len += 4;
						pktBf.putInt(symNum);
						len += 4;
						for (int i = 0; i < symNum; i ++)
						{
							pktBf.putInt(count);
							pktBf.put(result.data[count], 0, result.conf.SYMBOL_SIZE);
							count ++;
							len += 4 + result.conf.SYMBOL_SIZE;
						}
						
						DatagramPacket pkt = new DatagramPacket(pktBf.array(), len, addr, datagramPort);
						try
						{
							dgclient.send(pkt);
						}
						catch (IOException e1)
						{
							e1.printStackTrace();
						}
					}
					else
					{
						cancel();
					}
				}
			};
			
			long start2 = System.currentTimeMillis();
			timer.schedule(slowTask, Constant.UDP_SLOW_INTERVAL, Constant.UDP_SLOW_INTERVAL);
			
			ctrSocket.setSoTimeout(Constant.SO_SOCKET_TIMEOUT);
			BufferedReader cis = new BufferedReader(new InputStreamReader(ctrSocket.getInputStream()));
			String rcvStr = cis.readLine();
			System.out.println("sending thread Rcv : " + rcvStr);
			JSONObject respon = new JSONObject(rcvStr);
			if (respon.getString("status").equals("success"))
			{
				ret = true;
				timer.cancel();
				waitTime = System.currentTimeMillis() - start2;
			}
		}
		catch (SocketTimeoutException e)
		{
			e.printStackTrace();
		}
		catch (IOException e2)
		{
			e2.printStackTrace();
		}
		

		pcount ++;
		return ret;
	}
}
