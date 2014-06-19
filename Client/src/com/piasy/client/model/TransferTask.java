package com.piasy.client.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import raptor.util.Config;
import raptor.util.Receiver;

import com.piasy.client.controller.Controller;

/**
 * <p>The transfer task class, it takes charge of the transfer of
 * one file, including the splitting and acknowledgment</p>
 *  
 *  
 * <p>author: 	Piasy Xu</p>
 * <p>date:		19:54 2013/12/22</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class TransferTask extends Thread
{
	int mode = Constant.TRANSFER_MODE_NONE;
	String filename;
	Socket channel;
	BufferedReader cis;
	PrintWriter cos;
	boolean finished = false;
	int port;
	TransferTask instance;
	
	public TransferTask(int mode, String filename, int port)
	{
		this.mode = mode;
		this.filename = filename;
		this.port = port;
		instance = this;
	}

	DatagramSocket dgclient, dgserver;
	ArrayList<Block> transferBlocks;
	ArrayList<JSONObject> downloadBlocks = new ArrayList<JSONObject>();
	ArrayList<Block> splitBlocks;
	

	InetAddress addr = null;
	ByteBuffer pktBf = ByteBuffer.allocate(Constant.PKT_SIZE);
	
	long transferSize = 0;
	long totalSize = 0;
	
	long uploadSplitTime = 0, uploadEncodeTime = 0, uploadSendTime = 0;
	@Override
	public void run() 
	{
		try
		{
			addr = InetAddress.getByName(Setting.SERVER_IP);
			dgclient = new DatagramSocket();
			
			switch (mode)
			{
			case Constant.TRANSFER_MODE_UPLOAD:
				try
				{
					JSONObject request = new JSONObject();

					File file = new File(filename);
					request.put("type", "upload");
					request.put("filename", filename);
					request.put("size", file.length());
					
					Controller myController = Controller.getController();
					com.piasy.client.model.Config conf = myController.getConf(file.length());
					System.out.println("to split");
					long start1 = System.currentTimeMillis();
					splitBlocks = IndexModel.CDCSplit(file, conf.magic,
									conf.mask, conf.min, conf.max, conf.step);
					
					uploadSplitTime = (System.currentTimeMillis() - start1);
					start1 = System.currentTimeMillis();
					
					System.out.println("Split time: " + uploadSplitTime + " ms");
					System.out.println("Split finish! " + splitBlocks.size() + " blocks");
					JSONArray blocksInfo = new JSONArray();
					for (Block block : splitBlocks)
					{
						JSONObject blockInfo = new JSONObject();
						blockInfo.put("seq", block.blockSeq);
						blockInfo.put("size", block.size);
						blockInfo.put("hash", block.hash);
						blocksInfo.put(blockInfo);
					}
					
					request.put("blocks", blocksInfo);

					if (!init())
					{
						System.out.println("Init failed!");
						return;
					}
					
					transferBlocks = getUploadBlocks(request);
					
					upload();
					
					System.out.println("Total work, redund : " + ((double) (totalSize - transferSize) / totalSize) 
										+ ", split time : " + uploadSplitTime + ", encode & upload time : " 
										+ (System.currentTimeMillis() - start1));
				}
				catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				break;
			case Constant.TRANSFER_MODE_DOWNLOAD:
			{
				dgserver = new DatagramSocket();
				if (!init() && 0 < dgserver.getLocalPort())
				{
					System.out.println("Init failed!");
					return;
				}
				
				try
				{
					JSONObject request = new JSONObject();

					request.put("type", "download");
					request.put("filename", filename);
					request.put("port", dgserver.getLocalPort());
					
					transferBlocks = getDownloadBlocks(request);
					download();
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				
				
				break;
			}
			default:
				break;
			}
		}
		catch (UnknownHostException e1)
		{
			e1.printStackTrace();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}
	
	private ArrayList<Block> getUploadBlocks(JSONObject request) 
			throws IOException, JSONException
	{
		ArrayList<Block> uploadBlocks = new ArrayList<Block>();
		cos.println(request.toString());
		cos.flush();
		System.out.println("Send : " + request.toString());
		
		String rcvStr = cis.readLine();
		System.out.println("Rcv : " + rcvStr);
		JSONObject reply = new JSONObject(rcvStr);
		
		JSONArray blocks = reply.getJSONArray("blocks");
		for (int i = 0; i < blocks.length(); i ++)
		{
			JSONObject block = blocks.getJSONObject(i);
			int seq = block.getInt("seq");
			String action = block.getString("action");
			
			if (action.equals("success"))
			{
				
			}
			else if (action.equals("upload"))
			{
				uploadBlocks.add(splitBlocks.get(seq));
				transferSize += splitBlocks.get(seq).size;
			}
			totalSize += splitBlocks.get(seq).size;
		}
		
		return uploadBlocks;
	}
	
	JSONArray allDownloadBlocks = new JSONArray();
	private ArrayList<Block> getDownloadBlocks(JSONObject request) 
			throws IOException, JSONException
	{
		ArrayList<Block> _downloadBlocks = new ArrayList<Block>();
		
		cos.println(request.toString());
		cos.flush();
		System.out.println("Send : " + request.toString());
		
		String rcvStr = cis.readLine();
		System.out.println("Rcv : " + rcvStr);
		JSONObject reply = new JSONObject(rcvStr);
		
		JSONArray blocks = reply.getJSONArray("blocks");
		allDownloadBlocks = blocks;
		String filename = reply.getString("filename");
		for (int i = 0; i < blocks.length(); i ++)
		{
			JSONObject block = blocks.getJSONObject(i);
			int seq = block.getInt("seq");
			transferSize += block.getLong("size");
			
			_downloadBlocks.add(new Block(filename, block.getLong("size"), 0, seq, block.getString("hash")));
			downloadBlocks.add(block);
		}
		
		return _downloadBlocks;
	}

	/**
	 * Initialize the thread
	 * */
	protected boolean init() 
			throws UnknownHostException, IOException
	{
		boolean ret = false;
		channel = new Socket(Setting.SERVER_IP, port);
		channel.setSoTimeout(Constant.SO_SOCKET_TIMEOUT);
		channel.setTcpNoDelay(true);
		channel.setKeepAlive(true);
		cis = new BufferedReader(new InputStreamReader(channel.getInputStream()));
		cos = new PrintWriter(channel.getOutputStream());
		ret = true;
		
		return ret;
	}
	
	long waitTime = 0;
	/**
	 * Upload method, it will create a new thread to upload this block to 
	 * server.
	 * @throws JSONException 
	 * */
	protected void upload()
			throws IOException, JSONException
	{
		System.out.println("Upload start!");
		
		File file = new File(filename);
		RandomAccessFile dis = new RandomAccessFile(file, "r");
		byte [] buf = new byte[Constant.BUFFER_SIZE];
		ByteBuffer bf = ByteBuffer.allocate(Constant.BUFFER_SIZE);
		
		int blockNum = (int) Math.ceil((double) transferSize 
						/ (Constant.K_NUM * Constant.SYMBOL_SIZE));
		
		ArrayList<CodingRawData> codingBlocks = new ArrayList<CodingRawData>();
		int lo = 0, hi = 0;
		int sendSize = 0;
		for (int i = 0; i < blockNum; i ++)
		{
			int size = 4;
			bf.clear();
			if ((i == blockNum - 1) 
					|| (i == blockNum - 2 && transferSize - sendSize < Constant.K_NUM * Constant.MIN_SYMBOL_SIZE))
			{//if the last block is too small, merge it here
				hi = transferBlocks.size();
				bf.putInt(hi - lo);
				for (int j = lo; j < hi; j ++)
				{
					Block block = transferBlocks.get(j);
					bf.putInt(block.blockSeq);
					bf.putInt((int) block.size);
					dis.seek(block.offset);
					int read = dis.read(buf, 0, (int) block.size);
					
					if (read != block.size)
					{
						System.out.println("Split error! No enough data, upload fail!");
						dis.close();
						return;
					}
					

					bf.put(buf, 0, read);
					sendSize += read;
					size += read + 8;
				}

				raptor.util.Config conf = new raptor.util.Config((int) Math.ceil((double) size / Constant.K_NUM), 
						Constant.K_NUM, Constant.OVERHEAD);
				
				byte [] raw = new byte[size];
				System.arraycopy(bf.array(), 0, raw, 0, size);
				codingBlocks.add(new CodingRawData(raw, conf));
				lo = hi;
				break;
			}
			else
			{
				while (hi < transferBlocks.size() && size < Constant.K_NUM * Constant.SYMBOL_SIZE)
				{
					size += transferBlocks.get(hi).size + 8;
					hi ++;
				}
				hi --;
				size -= transferBlocks.get(hi).size + 8;
				
				bf.putInt(hi - lo);
				for (int j = lo; j < hi; j ++)
				{
					Block block = transferBlocks.get(j);
					bf.putInt(block.blockSeq);
					bf.putInt((int) block.size);
					dis.seek(block.offset);
					int read = dis.read(buf, 0, (int) block.size);
					
					if (read != block.size)
					{
						System.out.println("Split error! No enough data, upload fail!");
						dis.close();
						return;
					}
					
					bf.put(buf, 0, read);
					sendSize += read;
				}
				
				raptor.util.Config conf = new raptor.util.Config((int) Math.ceil((double) size / Constant.K_NUM), 
						Constant.K_NUM, Constant.OVERHEAD);
				
				byte [] raw = new byte[size];
				System.arraycopy(bf.array(), 0, raw, 0, size);
				codingBlocks.add(new CodingRawData(raw, conf));
				lo = hi;
			}
		}
		dis.close();
		
		codingSend(codingBlocks);
		System.out.println("Upload finish!");
	}

	protected void codingSend(ArrayList<CodingRawData> codingBlocks) throws JSONException, IOException
	{
		ArrayList<SendingThread> threads = new ArrayList<SendingThread>();
		int index = 0;
		while (index < codingBlocks.size())
		{
			int encodeThreadCount = 0;
			for (SendingThread thread : threads)
			{
				if (thread.state() <= Constant.CODING_STATE_ENCODE_START)
				{
					encodeThreadCount ++;
				}
			}
			
			if (encodeThreadCount < Constant.CODING_THREAD_COUNT)
			{
				JSONObject conf = new JSONObject();
				conf.put("K", codingBlocks.get(index).conf.K);
				conf.put("SYMSIZE", codingBlocks.get(index).conf.SYMBOL_SIZE);
				conf.put("bsize", codingBlocks.get(index).raw.length);
				
				cos.println(conf.toString());
				cos.flush();
				System.out.println("Send : " + conf.toString());
				String rcvStr = cis.readLine();
				System.out.println("Rcv : " + rcvStr);
				JSONObject reply = new JSONObject(rcvStr);
				if (reply.getString("action").equals("start"))
				{//server must reply start
					int dgport = reply.getInt("port");
					int ctrPort = reply.getInt("ctrport");
					Socket ctrSocket = new Socket(Setting.SERVER_IP, ctrPort);
					System.out.println("create ctr socket ok " + ctrSocket.getLocalPort() + " -> " + ctrPort);
					
					SendingThread thread = new SendingThread(codingBlocks.get(index), addr, dgport, index, ctrSocket);
					thread.start();
					threads.add(thread);
					index ++;
				}
			}
			else
			{
				try
				{
					System.out.println("Goto sleep " + Constant.CODING_THREAD_WAIT_TIME + " ms");
					Thread.sleep(Constant.CODING_THREAD_WAIT_TIME);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		for (SendingThread thread : threads)
		{
			try
			{
				thread.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	ByteBuffer blkBf = ByteBuffer.allocate(Constant.K_NUM * 
			(Constant.SYMBOL_SIZE + Constant.MIN_SYMBOL_SIZE));
	protected void download() 
			throws JSONException, FileNotFoundException, IOException
	{
		int bcount = 0;
		while (0 < downloadBlocks.size())
		{
			String rcvStr = cis.readLine();
			System.out.println("Rcv : " + rcvStr);
			JSONObject conf = new JSONObject(rcvStr);
			conf.put("action", "start");
			
			cos.println(conf.toString());
			cos.flush();
			System.out.println("Send : " + conf.toString());
			
			byte [] mixed = decodeOneBlock(dgserver, conf, bcount);
			if (mixed != null)
			{
				blkBf.clear();
				blkBf.put(mixed);
				
				int offset = 0;
				int blkNum = blkBf.getInt(offset);
				offset += 4;
				for (int i = 0; i < blkNum; i ++)
				{
					int index = blkBf.getInt(offset);
					offset += 4;
					int size = blkBf.getInt(offset);
					offset += 4;
					
					FileOutputStream fout = new FileOutputStream(
							new File(Constant.TMP_DIR + "/" + allDownloadBlocks.getJSONObject(index).getString("hash")));
					fout.write(mixed, offset, size);
					fout.close();
					
					downloadBlocks.remove(allDownloadBlocks.getJSONObject(index));
					
					offset += size;
				}
				
				JSONObject respon = new JSONObject();
				respon.put("status", "success");
				cos.println(respon.toString());
				cos.flush();
				System.out.println("Send : " + respon.toString());
			}
			else
			{
				//TODO decode fail
			}
		}
		
		merge(filename, allDownloadBlocks);
	}
	
	protected byte [] decodeOneBlock(DatagramSocket dgserver, JSONObject conf, int blockSeq) throws JSONException, IOException
	{
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
		System.out.println(need);
		int count = 0;
		
		while (count < need)
		{
			pktBf.clear();
			DatagramPacket pkt = new DatagramPacket(buf, Constant.PKT_SIZE);
			dgserver.receive(pkt);
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
			
		System.out.println("Receive time: " + (System.currentTimeMillis() - start1) + " ms");
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
		
		System.out.println("pos = " + pos + ", count = " + count + ", need = " + need);
		byte [] raw = Receiver.decode(bsize, new Config(SYMSIZE, K, Constant.OVERHEAD), data, index);
		
		System.out.println("Decode time: " + (System.currentTimeMillis() - start2) + " ms");
		return raw;
	}
	
	
	protected void merge(String filename, JSONArray blocks)
	{
		File file = new File(filename);
		file = new File(Constant.APP_BASE_DIR + "/" + file.getName());
		try
		{
			FileOutputStream fout = new FileOutputStream(file);
			byte [] buf = new byte[1024 * 1024];
			
			for (int i = 0; i < blocks.length(); i ++)
			{
				try
				{
					JSONObject block = blocks.getJSONObject(i);
					FileInputStream fin = new FileInputStream(new File(Constant.TMP_DIR + "/" + block.getString("hash")));
					
					int read = fin.read(buf, 0, (int) block.getLong("size"));
					fout.write(buf, 0, read);
					
					fin.close();
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			
			fout.close();
		}
		catch (FileNotFoundException e1)
		{
			e1.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
		
	public boolean finished()
	{
		return finished;
	}

}
