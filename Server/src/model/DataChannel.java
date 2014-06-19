package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import raptor.util.EncodeResult;
import raptor.util.Sender;
import util.Loger;
import controller.Controller;

/**
 * <p>This is the data channel class. The data channel 
 * transfer files. This file may be split into many
 * blocks, all the transferring of them will connect to
 * the same data channel in server.</p>
 * 
 * <p>author: 	Piasy Xu</p>
 * <p>date:		20:42 2013/12/22</p>
 * <p>email:	xz4215@gmail.com</p>
 * <p>github:	Piasy</p>
 * */
public class DataChannel extends Thread
{

	boolean keepRunning = true;
	ServerSocket dataServerSocket = null;
	Controller myController = null;
	
	/**
	 * Constructor, assign the server socket listening to client, 
	 * transfer mode, file name, controller's reference.
	 * */
	public DataChannel(ServerSocket dataServerSocket, Controller myController)
	{
		this.dataServerSocket = dataServerSocket;
		this.myController = myController;
	}
	
	/**
	 * The thread body of data channel.
	 * Create the target file(directory), listening to client.
	 * */
	@Override
	public void run()
	{
		while (keepRunning)
		{
			try
			{
				dataServerSocket.setSoTimeout(Constant.DATA_CHANNEL_ACCEPT_TIMEOUT);
				Socket socket = dataServerSocket.accept();
				TransferThread thread = new TransferThread(socket);
				thread.start();
				Loger.Log(Constant.LOG_LEVEL_INFO, "Accept a client data channel at port " + 
						  socket.getLocalPort(), "DataChannel accept");
			}
			catch (IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_DEBUG, e.getMessage(), "DataChannel accept");
			}
		}
		
		try
		{
			dataServerSocket.close();
		}
		catch (IOException e)
		{
			Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel run close");
		}
		Loger.Log(Constant.LOG_LEVEL_INFO, "Data channel at " + dataServerSocket.getLocalPort() + " closed.", "DataChannel run");
	}

	/**
	 * The transfer thread of data channel.
	 * */
	class TransferThread extends Thread
	{
		Socket dataSocket = null;
		BufferedReader cis;
		PrintWriter cos;

		ArrayList<DGSockPoolItem> socksPool = new ArrayList<DGSockPoolItem>();
		
		/**
		 * Assign data socket and transfer mode.
		 * */
		public TransferThread(Socket dataSocket)
		{
			this.dataSocket = dataSocket;
		}
		
		/**
		 * Thread body, transfer file.
		 * */

		InetAddress addr = null;
		
		@Override
		public void run()
		{
			init();
			
			try
			{
				String rcvStr = cis.readLine();
				Loger.Log(Constant.LOG_LEVEL_INFO, "receive : " + rcvStr, "DataChannel run");
				
				JSONObject transferInfo = new JSONObject(rcvStr);
				String type = transferInfo.getString("type");
				
				if (type.equals("upload"))
				{
					upload(transferInfo);
				}
				else if (type.equals("download"))
				{
					download(transferInfo);
				}
			}
			catch (IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread run");
			}
			catch (JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread run");
			}
		}

		ByteBuffer blkBf = ByteBuffer.allocate(Constant.K_NUM * 
				(Constant.SYMBOL_SIZE + Constant.MIN_SYMBOL_SIZE));
		ByteBuffer pktBf = ByteBuffer.allocate(Constant.PKT_SIZE);

		/**
		 * Receive file from client, save it as the part file.
		 * */
		protected void upload(JSONObject transferInfo)
		{
			try
			{
				myController.addFile(transferInfo);
				String filename = transferInfo.getString("filename");
				JSONArray blocks = transferInfo.getJSONArray("blocks");
				
				ArrayList<JSONObject> transferBlocks = new ArrayList<JSONObject>();
				JSONArray responseBlocks = new JSONArray();
				for (int j = 0; j < blocks.length(); j ++)
				{
					JSONObject block = new JSONObject();
					block.put("seq", blocks.getJSONObject(j).getInt("seq"));	
					if (!myController.hasBlock(blocks.getJSONObject(j).getString("hash")))
					{
						block.put("action", "upload");
						transferBlocks.add(blocks.getJSONObject(j));
					}
					else
					{
						block.put("action", "success");
						myController.addBlockToFile(blocks.getJSONObject(j), filename);
					}
					responseBlocks.put(block);
				}
				
				JSONObject reply = new JSONObject();
				reply.put("blocks", responseBlocks);
				cos.println(reply.toString());
				cos.flush();
				System.out.println("Send : " + reply.toString());
				
				ArrayList<ReceivingThread> threads = new ArrayList<ReceivingThread>();
				int bcount = 0;
				while (0 < transferBlocks.size())
				{

					for (int i = 0; i < threads.size(); i ++)
					{
						ReceivingThread thread2 = threads.get(i);
						if (!thread2.item.active)
						{
							byte [] mixed = thread2.item.mixed;
							if (mixed != null)
							{
								blkBf.clear();
								blkBf.put(mixed);
								
								int offset = 0;
								int blkNum = blkBf.getInt(offset);
								offset += 4;
								for (int j = 0; j < blkNum; j ++)
								{
									int index = blkBf.getInt(offset);
									offset += 4;
									int size = blkBf.getInt(offset);
									offset += 4;
									
									FileOutputStream fout = new FileOutputStream(
											new File(blocks.getJSONObject(index).getString("hash")));
									fout.write(mixed, offset, size);
									fout.close();
									
									myController.addBlock(blocks.getJSONObject(index), filename);
									transferBlocks.remove(blocks.getJSONObject(index));
									
									offset += size;
								}
							}
							else
							{
								//TODO decode fail
							}
							threads.remove(i);
							break;
						}
					}
					
					String rcvStr = cis.readLine();
					System.out.println("Rcv : " + rcvStr);
					
					ServerSocket ctrSocket = new ServerSocket(0);
					if (ctrSocket.getLocalPort() <= 0)
					{
						System.out
								.println("Error when create ctr socket");
					}
					
					DatagramSocket dgserver = new DatagramSocket();
					JSONObject conf = new JSONObject(rcvStr);
					conf.put("action", "start");
					conf.put("port", dgserver.getLocalPort());
					conf.put("ctrport", ctrSocket.getLocalPort());

					cos.println(conf.toString());
					cos.flush();
					System.out.println("Send : " + conf.toString());
					
					Socket ctr = ctrSocket.accept();
					ReceivingThread thread = new ReceivingThread(new DGSockPoolItem(dgserver), conf, bcount, ctr);
					thread.item.active = true;
					thread.start();
					threads.add(thread);
					bcount ++;
					ctrSocket.close();
				}
//				merge(filename, blocks);
			}
			catch (SocketTimeoutException e)
			{
				e.printStackTrace();
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread upload");
				try
				{
					dataSocket.close();
				}
				catch (IOException e1)
				{
					Loger.Log(Constant.LOG_LEVEL_ERROR, e1.getMessage(), "DataChannel TransferThread upload");
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
			catch (SocketException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		/**
		 * Merge blocks into one integral file.
		 * */
		protected void merge(String filename, JSONArray blocks)
		{
			File file = new File(filename);
			file = new File(file.getName());
			try
			{
				FileOutputStream fout = new FileOutputStream(file);
				byte [] buf = new byte[1024 * 1024];
				
				for (int i = 0; i < blocks.length(); i ++)
				{
					try
					{
						JSONObject block = blocks.getJSONObject(i);
						FileInputStream fin = new FileInputStream(new File(block.getString("hash")));
						
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
		
		int datagramPort = 0;
		DatagramSocket dgclient;
		/**
		 * Send file to client.
		 * */
		protected void download(JSONObject transferInfo)
		{
			try
			{
				datagramPort = transferInfo.getInt("port");
				dgclient = new DatagramSocket();
				String filename = transferInfo.getString("filename");
				JSONObject reply = myController.getFileInfo(filename);
				JSONArray blocks = reply.getJSONArray("blocks");
				ArrayList<Block> transferBlocks = new ArrayList<Block>();
				for (int i = 0; i < blocks.length(); i ++)
				{
					JSONObject block = blocks.getJSONObject(i);
					transferBlocks.add(new Block(filename, block.getLong("size"), 0, block.getInt("seq"), block.getString("hash")));
				}
				
				long transferSize = reply.getLong("size");
				cos.println(reply.toString());
				cos.flush();
				Loger.Log(Constant.LOG_LEVEL_INFO, "send : " + reply.toString(), "DataChannel download");
				
				byte [] buf = new byte[Constant.BUFFER_SIZE];
				ByteBuffer bf = ByteBuffer.allocate(Constant.BUFFER_SIZE);
				
				int blockNum = (int) Math.ceil((double) transferSize 
								/ (Constant.K_NUM * Constant.SYMBOL_SIZE));
				
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
							FileInputStream fin = new FileInputStream(new File(block.hash));
							int read = fin.read(buf, 0, (int) block.size);
							
							if (read != block.size)
							{
								System.out.println("Split error! No enough data, upload fail!");
								fin.close();
								return;
							}
							
							bf.put(buf, 0, read);
							sendSize += read;
							size += read + 8;
							fin.close();
						}

						long start1 = System.currentTimeMillis();
						raptor.util.Config conf = new raptor.util.Config((int) Math.ceil((double) size / Constant.K_NUM), 
								Constant.K_NUM, Constant.OVERHEAD);
						EncodeResult result = Sender.encode(bf.array(), 0, size, conf);
						System.out.println("Encode time: " + (System.currentTimeMillis() - start1) + " ms");
						send(result, size, i);
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
							FileInputStream fin = new FileInputStream(new File(block.hash));
							int read = fin.read(buf, 0, (int) block.size);
							
							if (read != block.size)
							{
								System.out.println("Split error! No enough data, upload fail!");
								fin.close();
								return;
							}
							
							bf.put(buf, 0, read);
							sendSize += read;
							fin.close();
						}
						
						long start1 = System.currentTimeMillis();
						raptor.util.Config conf = new raptor.util.Config((int) Math.ceil((double) size / Constant.K_NUM), 
								Constant.K_NUM, Constant.OVERHEAD);
						EncodeResult result = Sender.encode(bf.array(), 0, size, conf);
						System.out.println("Encode time: " + (System.currentTimeMillis() - start1) + " ms");
						send(result, size, i);
						lo = hi;
					}
				}
				System.out.println("Download finish!");
				dataSocket.close();
			}
			catch (NullPointerException e)
			{
				Loger.Log(Constant.LOG_LEVEL_INFO, e.getMessage(), "DataChannel download");
			}
			catch (SocketTimeoutException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread download");
				try
				{
					dataSocket.close();
				}
				catch (IOException e1)
				{
					Loger.Log(Constant.LOG_LEVEL_ERROR, e1.getMessage(), "DataChannel TransferThread download");
				}
			}
			catch (IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread download");
			}
			catch (JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread download");
			}
		}
		
		int pcount = 0;
		int count = 0;
		/**
		 * send one coding block
		 * */
		protected boolean send(final EncodeResult result, int blockSize, final int blockSeq) throws JSONException
		{
			System.out.println("Send coding Block " + blockSeq);
			long start1 = System.currentTimeMillis();
			boolean ret = false;
			JSONObject conf = new JSONObject();
			conf.put("K", result.conf.K);
			conf.put("SYMSIZE", result.conf.SYMBOL_SIZE);
			conf.put("bsize", blockSize);
			
			cos.println(conf.toString());
			cos.flush();
			System.out.println("Send : " + conf.toString());
			String rcvStr;
			try
			{
				rcvStr = cis.readLine();
				System.out.println("Rcv : " + rcvStr);
				JSONObject reply = new JSONObject(rcvStr);
				
				if (reply.getString("action").equals("start"))
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
					
					timer.schedule(slowTask, Constant.UDP_SLOW_INTERVAL, Constant.UDP_SLOW_INTERVAL);
					
					rcvStr = cis.readLine();
					System.out.println("Rcv : " + rcvStr);
					JSONObject respon = new JSONObject(rcvStr);
					if (respon.getString("status").equals("success"))
					{
						ret = true;
						timer.cancel();
						System.out.println("Send time: " + (System.currentTimeMillis() - start1) + " ms");
					}
				}
			}
			catch (IOException e2)
			{
				e2.printStackTrace();
			}
			

			pcount ++;
			return ret;
		}
		
		/**
		 * Initialize the transfer thread
		 * */
		protected boolean init()
		{
			boolean ret = false;
			try
			{
				dataSocket.setSoTimeout(Constant.SO_SOCKET_TIMEOUT);
				dataSocket.setTcpNoDelay(true);
				cis = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
				cos = new PrintWriter(dataSocket.getOutputStream());
				
				addr = dataSocket.getInetAddress();
				dgclient = new DatagramSocket();
				
				while (socksPool.size() < Constant.DGSOCK_POOL_SIZE)
				{
					DatagramSocket socket = new DatagramSocket();
					if (0 < socket.getLocalPort())
					{
						socksPool.add(new DGSockPoolItem(socket));
					}
					else
					{
						System.out.println("Warning: new dgsock fail");
					}
				}
				
				ret = true;
			}
			catch (IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread init");
			}
			return ret;
		}

	}
}