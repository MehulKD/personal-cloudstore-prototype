package model;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.Loger;
import controller.Controller;

/**
 * <p>This is the data channel class. The data channel 
 * transferring files. This file may be split into many
 * blocks, all the transferring of them will connect to
 * the same data channel in server. After one block's transfer
 * has finished, we will acknowledge it to client via controller.</p>
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
				socket.setSendBufferSize(Constant.BUFFER_SIZE);
				socket.setReceiveBufferSize(Constant.BUFFER_SIZE);
				socket.setTcpNoDelay(true);
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
	 * When the file transfer has finished, 
	 * this thread should be stopped
	 * */
	public void stopMe()
	{
		keepRunning = false;
	}
	
	/**
	 * Send ack to client.
	 * @param the stream tag of this block
	 * */
	protected void ack(long streamSeq)
	{
		//TO-DO ack via controller
		myController.ack(streamSeq);
	}

	/**
	 * The transfer thread of data channel.
	 * */
	class TransferThread extends Thread
	{
		Socket dataSocket = null;
		BufferedReader cis;
		PrintWriter cos;
		
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
				else if (type.equals("updateup"))
				{
					updateUp(transferInfo);
				}
				else if (type.equals("updateupfast"))
				{
					updateUpFast(transferInfo);
				}
				else if (type.equals("updatedown"))
				{
					
				}
				else if (type.equals("uploadfast"))
				{
					
				}
				else if (type.equals("downloadfast"))
				{
					
				}
				else if (type.equals("pureupload"))
				{
					pureUpload(transferInfo);
				}
				else if (type.equals("puredownload"))
				{
					pureDownload(transferInfo);
				}
			}
			catch (IOException | JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread run");
			}
		}
		
		protected boolean init()
		{
			boolean ret = false;
			try
			{
				dataSocket.setSoTimeout(Constant.SOCKET_READ_TIMEOUT);
				cis = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
				cos = new PrintWriter(dataSocket.getOutputStream());
				ret = true;
			}
			catch (IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread init");
			}
			return ret;
		}
			
		protected void pureDownload(JSONObject transferInfo)
		{
			try
			{
				JSONArray filesInfo = transferInfo.getJSONArray("files");
				JSONArray response = new JSONArray();
				int read;
				byte [] buf = new byte[Constant.BUFFER_SIZE];
				for (int i = 0; i < filesInfo.length(); i ++)
				{
					JSONObject info = new JSONObject();
					String filename = filesInfo.getJSONObject(i).getString("filename");
					File file = new File(filename);
					File orgFile = new File(file.getName());
					orgFile.createNewFile();
					File gzipFile = new File(file.getName() + ".gz");
					gzipFile.createNewFile();
					FileInputStream fin = new FileInputStream(orgFile);
					GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(gzipFile));
					while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
					{
						gos.write(buf, 0, read);
					}
					fin.close();
					gos.close();
					
					info.put("filename", filename);
					info.put("size", gzipFile.length());
					response.put(info);
				}
				cos.println(response.toString());
				cos.flush();
				
				for (int i = 0; i < filesInfo.length(); i ++)
				{
					String filename = filesInfo.getJSONObject(i).getString("filename");
					File file = new File(filename);
					DataOutputStream dos = new DataOutputStream(dataSocket.getOutputStream());
					FileInputStream fin = new FileInputStream(new File(file.getName() + ".gz"));
					while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
					{
						dos.write(buf, 0, read);
					}
					dos.flush();
					fin.close();
				}
				dataSocket.close();
			}
			catch (IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread pureUpload");
			}
			catch (JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread pureUpload");
			}
		}
		
		protected void pureUpload(JSONObject transferInfo)
		{
			try
			{
				JSONArray filesInfo = transferInfo.getJSONArray("files");
				byte [] buf = new byte[Constant.BUFFER_SIZE];
				
				for (int i = 0; i < filesInfo.length(); i ++)
				{
					String filename = filesInfo.getJSONObject(i).getString("filename");
					File file = new File(filename);
					File gzipFile = new File(file.getName() + ".gz");
					File orgFile = new File(file.getName());
					FileOutputStream fout = new FileOutputStream(gzipFile);
					long size = filesInfo.getJSONObject(i).getLong("size");
					long totalRead = 0;
					int read;
					DataInputStream dis = new DataInputStream(dataSocket.getInputStream());
					while (totalRead < size)
					{
						read = dis.read(buf, 0, Constant.BUFFER_SIZE);
						if (read == -1)
						{
							Loger.Log(Constant.LOG_LEVEL_DEBUG, "read -1 bytes, " + totalRead + "/" + size
								, "pure upload");
							continue;
						}
						fout.write(buf, 0, read);
						totalRead += read;
						Loger.Log(Constant.LOG_LEVEL_DEBUG, "read " + read + " bytes, " + totalRead + "/" + size
								, "pure upload");
					}
					fout.close();
					GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzipFile));
					fout = new FileOutputStream(orgFile);
					while ((read = gis.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
					{
						fout.write(buf, 0, read);
					}
					fout.close();
					gis.close();
				}
				dataSocket.close();
			}
			catch (IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread pureUpload");
			}
			catch (JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread pureUpload");
			}
		}
		
		/**
		 * Receive file from client, save it as the part file.
		 * */
		protected void upload(JSONObject transferInfo)
		{
			File partFile = null;
			try
			{
				HashMap<String, JSONArray> transferTasks = new HashMap<String, JSONArray>();
				JSONArray transferFiles = new JSONArray();
				JSONArray filesInfo = transferInfo.getJSONArray("files");
				for (int i = 0; i < filesInfo.length(); i ++)
				{
					myController.addFile(filesInfo.getJSONObject(i));
					String filename = filesInfo.getJSONObject(i).getString("filename");
					JSONArray blocks = filesInfo.getJSONObject(i).getJSONArray("blocks");
					JSONArray transferBlocks = new JSONArray();
					JSONArray responseBlocks = new JSONArray();
					for (int j = 0; j < blocks.length(); j ++)
					{
						JSONObject block = new JSONObject();
						block.put("seq", blocks.getJSONObject(j).getInt("seq"));	
						if (!myController.hasBlock(blocks.getJSONObject(j).getString("hash")))
						{
							block.put("action", "upload");
							transferBlocks.put(blocks.getJSONObject(j));
						}
						else
						{
							block.put("action", "success");
							myController.addBlockToFile(blocks.getJSONObject(j), filename);
						}
						responseBlocks.put(block);
					}
					JSONObject transferFile = new JSONObject();
					transferFile.put("filename", filename);
					transferFile.put("blocks", responseBlocks);
					transferFiles.put(transferFile);
					
					transferTasks.put(filename, transferBlocks);
				}
				cos.println(transferFiles.toString());
				cos.flush();
				
				Loger.Log(Constant.LOG_LEVEL_INFO, "send : " + transferFiles.toString(), "DataChannel upload");
				
				byte [] buf = new byte[Constant.BUFFER_SIZE];
				
				for (String filename : transferTasks.keySet())
				{
					JSONArray fileBlocks = transferTasks.get(filename);
					for (int i = 0; i < fileBlocks.length(); i ++)
					{
						//blockseq||size	||hash			||data
						//int, 		long, string(32 bytes), size bytes
						DataInputStream dis = new DataInputStream(dataSocket.getInputStream());
						int read = dis.read(buf, 0, 44);
						if (read == 44)
						{
							ByteBuffer byteBuffer = ByteBuffer.allocate(12);
							byteBuffer.put(buf, 0, 12);
							int blockSeq = byteBuffer.getInt(0);
							long size = byteBuffer.getLong(4);
							String hash = new String(buf, 12, 32);
							
							Loger.Log(Constant.LOG_LEVEL_INFO, "Data Transfer stream : blockSeq = " + blockSeq 
									+ ", size = " + size 
									+ ", hash = " + hash, "DataChannel TransferThread upload");
							
							partFile = new File(hash + ".gz");
							
							Loger.Log(Constant.LOG_LEVEL_INFO, "save file path = " + partFile.getAbsolutePath(), "DataChannel TransferThread upload");
							
							DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(partFile)));
							int totalRead = 0;
							while ((totalRead < size) && keepRunning)
							{
								read = dis.read(buf, 0, 
										(int) ((size - totalRead < Constant.BUFFER_SIZE) ? 
												(size - totalRead) : Constant.BUFFER_SIZE));
								dos.write(buf, 0, read);
								totalRead += read;
							}
							dos.close();
							
							File orgPartFile = new File(hash);
							dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(orgPartFile)));
							GZIPInputStream gis = new GZIPInputStream(new FileInputStream(partFile));
							while ((read = gis.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
							{
								dos.write(buf, 0, read);
							}
							gis.close();
							dos.close();
							
							if (hash.equals(IndexModel.getMD5(orgPartFile)))
							{
								ack(filename, blockSeq);
								JSONObject block = new JSONObject();
								block.put("seq", blockSeq);
								block.put("size", orgPartFile.length());
								block.put("hash", hash);
								myController.addBlock(block, filename);
								partFile.delete();
							}
							//this condition is the user exit the client app in force,
							//delete the part if its transfer hasn't finished(useless).
							else
							{
								partFile.delete();
								orgPartFile.delete();
							}
						}
					
					}
				}
				
				dataSocket.close();
			}
			catch (IOException | JSONException e)
			{
				//if exception(TCP connection has failed) occur, delete the useless part file.
				if ((partFile != null) && partFile.exists())
				{
					partFile.delete();
				}
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread upload");
			}
		}
		
		//TODO implement accumulate ack
		protected void ack(String filename, int blockSeq)
		{
			try
			{
				JSONObject ackInfo = new JSONObject();
				ackInfo.put("type", "ack");
				ackInfo.put("filename", filename);
				ackInfo.put("seq", blockSeq);
				
				cos.println(ackInfo.toString());
				cos.flush();
				
				Loger.Log(Constant.LOG_LEVEL_INFO, "send : " + ackInfo.toString(), "DataChannel ack");
			}
			catch (JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread ack");
			}
		}
		
		protected void updateUp(JSONObject transferInfo)
		{
			try
			{
				JSONArray files = transferInfo.getJSONArray("files");
				byte [] buf = new byte[Constant.BUFFER_SIZE];
				for (int i = 0; i < files.length(); i ++)
				{
					//merge to integral file
					String filename = files.getJSONObject(i).getString("filename");
					JSONObject fileInfo = myController.getFileInfo(filename);
					JSONArray blocksInfo = fileInfo.getJSONArray("blocks");
					File file = new File(filename);
					File integFile = new File(file.getName());
					FileOutputStream fout = new FileOutputStream(integFile);
					for (int j = 0; j < blocksInfo.length(); j ++)
					{
						FileInputStream fin = new FileInputStream(
								new File(blocksInfo.getJSONObject(j).getString("hash")));
						int read;
						while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
						{
							fout.write(buf, 0, read);
						}
						fin.close();
					}
					fout.flush();
					fout.close();
					
					//gen sig file and compress
					File sigFile = new File(file.getName() + ".sig");
					RsyncModel.genSignature(integFile.getAbsolutePath(), sigFile.getAbsolutePath());
					File sigGzipFile = new File(file.getName() + ".sig.gz");
					FileInputStream fin = new FileInputStream(sigFile);
					GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(sigGzipFile));
					int read;
					while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
					{
						gos.write(buf, 0, read);
					}
					fin.close();
					gos.flush();
					gos.finish();
					gos.close();
					
					//send sig info
					JSONObject sigInfo = new JSONObject();
					sigInfo.put("filename", filename);
					sigInfo.put("size", sigGzipFile.length());
					cos.println(sigInfo.toString());
					cos.flush();
					Thread.sleep(1000);
					Loger.Log(Constant.LOG_LEVEL_DEBUG, sigInfo.toString(), "update up sig");
					
					//send sig gzip file
					fin = new FileInputStream(sigGzipFile);
					DataOutputStream dos = new DataOutputStream(dataSocket.getOutputStream());
					while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
					{
						dos.write(buf, 0, read);
					}
					fin.close();
					
					//read delta info
					String deltaInfoStr = cis.readLine();
					Loger.Log(Constant.LOG_LEVEL_DEBUG, deltaInfoStr, "update up delta");
					JSONObject deltaInfo = new JSONObject(deltaInfoStr);
					if (deltaInfo.getString("filename").equals(filename))
					{
						//receive delta gzip file
						long size = deltaInfo.getLong("size");
						DataInputStream dis = new DataInputStream(dataSocket.getInputStream());
						File deltaGzipFile = new File(file.getName() + ".delta.gz");
						File deltaFile = new File(file.getName() + ".delta");
						fout = new FileOutputStream(deltaGzipFile);
						long totalRead = 0;
						while (totalRead < size)
						{
							read = dis.read(buf, 0, 
									(int) ((size - totalRead < Constant.BUFFER_SIZE) ? 
											(size - totalRead) : Constant.BUFFER_SIZE));
							Loger.Log(Constant.LOG_LEVEL_DEBUG, "read " + read + " bytes, " + totalRead + "/" + size, "update up delta");
							if (read == -1)
							{
								continue;
							}
							fout.write(buf, 0, read);
							totalRead += read;
						}
						fout.flush();
						fout.close();
						
						//uncompress delta gzip file
						GZIPInputStream gis = new GZIPInputStream(new FileInputStream(deltaGzipFile));
						fout = new FileOutputStream(deltaFile);
						while ((read = gis.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
						{
							fout.write(buf, 0, read);
						}
						gis.close();
						fout.flush();
						fout.close();
						
						//patch to new file
						File integFileNew = new File(file.getName() + ".new");
						RsyncModel.patch(integFile.getAbsolutePath(), deltaFile.getAbsolutePath(), 
								integFileNew.getAbsolutePath());
						
						//split it and update database
						ArrayList<Block> blocks = IndexModel.CDCSplit(integFileNew);
						myController.updateFile(blocks, filename);
						fin = new FileInputStream(integFileNew);
						RandomAccessFile raf = new RandomAccessFile(integFileNew, "r");
						for (Block block : blocks)
						{
							File blockFile = new File(block.hash);
							if (!blockFile.exists())
							{
								raf.seek(block.offset);
								fout = new FileOutputStream(new File(block.hash));
								totalRead = 0;
								while (totalRead < block.size)
								{
									read = raf.read(buf, 0, 
											(int) ((block.size - totalRead < Constant.BUFFER_SIZE) ? 
													(block.size - totalRead) : Constant.BUFFER_SIZE));
									fout.write(buf, 0, read);
									totalRead += read;
								}
								fout.close();
							}
						}
						raf.close();
						dataSocket.close();
					}
				}
			}
			catch (JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread updateUp");
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		protected void updateUpFast(JSONObject transferInfo)
		{
			try
			{
				JSONArray files = transferInfo.getJSONArray("files");
				byte [] buf = new byte[Constant.BUFFER_SIZE];
				for (int i = 0; i < files.length(); i ++)
				{
					//merge to integral file
					String filename = files.getJSONObject(i).getString("filename");
					JSONObject fileInfo = myController.getFileInfo(filename);
					JSONArray blocksInfo = fileInfo.getJSONArray("blocks");
					File file = new File(filename);
					File integFile = new File(file.getName());
					FileOutputStream fout = new FileOutputStream(integFile);
					for (int j = 0; j < blocksInfo.length(); j ++)
					{
						FileInputStream fin = new FileInputStream(
								new File(blocksInfo.getJSONObject(j).getString("hash")));
						int read;
						while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
						{
							fout.write(buf, 0, read);
						}
						fin.close();
					}
					fout.flush();
					fout.close();
					
					//read delta info
					String deltaInfoStr = cis.readLine();
					Loger.Log(Constant.LOG_LEVEL_DEBUG, deltaInfoStr, "update up fast delta");
					JSONObject deltaInfo = new JSONObject(deltaInfoStr);
					if (deltaInfo.getString("filename").equals(filename))
					{
						//receive delta gzip file
						long size = deltaInfo.getLong("size");
						DataInputStream dis = new DataInputStream(dataSocket.getInputStream());
						File deltaGzipFile = new File(file.getName() + ".delta.gz");
						File deltaFile = new File(file.getName() + ".delta");
						fout = new FileOutputStream(deltaGzipFile);
						long totalRead = 0;
						int read;
						while (totalRead < size)
						{
							read = dis.read(buf, 0, 
									(int) ((size - totalRead < Constant.BUFFER_SIZE) ? 
											(size - totalRead) : Constant.BUFFER_SIZE));
							Loger.Log(Constant.LOG_LEVEL_DEBUG, "read " + read + " bytes, " + totalRead + "/" + size, "update up fast delta");
							if (read == -1)
							{
								continue;
//								break;
							}
							fout.write(buf, 0, read);
							totalRead += read;
						}
						fout.flush();
						fout.close();
						Loger.Log(Constant.LOG_LEVEL_DEBUG, "read finish", "update up fast delta");
						
						//uncompress delta gzip file
						GZIPInputStream gis = new GZIPInputStream(new FileInputStream(deltaGzipFile));
						fout = new FileOutputStream(deltaFile);
						while ((read = gis.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
						{
							fout.write(buf, 0, read);
						}
						gis.close();
						fout.flush();
						fout.close();
						Loger.Log(Constant.LOG_LEVEL_DEBUG, "compress finish", "update up fast delta");
						
						//patch to new file
						File integFileNew = new File(file.getName() + ".new");
						RsyncModel.patch(integFile.getAbsolutePath(), deltaFile.getAbsolutePath(), 
								integFileNew.getAbsolutePath());

						Loger.Log(Constant.LOG_LEVEL_DEBUG, "patch finish", "update up fast delta");
						//split it and update database
						ArrayList<Block> blocks = IndexModel.CDCSplit(integFileNew);
						myController.updateFile(blocks, filename);
						RandomAccessFile raf = new RandomAccessFile(integFileNew, "r");
						for (Block block : blocks)
						{
							File blockFile = new File(block.hash);
							if (!blockFile.exists())
							{
								raf.seek(block.offset);
								fout = new FileOutputStream(new File(block.hash));
								totalRead = 0;
								while (totalRead < block.size)
								{
									read = raf.read(buf, 0, 
											(int) ((block.size - totalRead < Constant.BUFFER_SIZE) ? 
													(block.size - totalRead) : Constant.BUFFER_SIZE));
									fout.write(buf, 0, read);
									totalRead += read;
								}
								fout.close();
							}
						}
						raf.close();

						dataSocket.close();
					}
				}
			}
			catch (JSONException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread updateUp");
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
				
		/**
		 * Send file to client.
		 * */
		protected void download(JSONObject transferInfo)
		{
			try
			{
				JSONArray filesInfo = transferInfo.getJSONArray("files");
				JSONArray response = new JSONArray();
				for (int i = 0; i < filesInfo.length(); i ++)
				{
					String filename = filesInfo.getJSONObject(i).getString("filename");
					JSONObject fileInfo = myController.getFileInfo(filename);
					JSONArray blocksInfo = fileInfo.getJSONArray("blocks");
					for (int j = 0; j < blocksInfo.length(); j ++)
					{
						JSONObject blockInfo = blocksInfo.getJSONObject(j);
						File orgBlock = new File(blockInfo.getString("hash"));
						File gzipBlock = new File(blockInfo.getString("hash") + ".gz");
						FileInputStream fin = new FileInputStream(orgBlock);
						GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(gzipBlock));
						int read;
						byte[] buf = new byte[Constant.BUFFER_SIZE];
						while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
						{
							gos.write(buf, 0, read);
						}
						gos.flush();
						gos.finish();
						gos.close();
						fin.close();
//						System.out
//								.println("before " + blocksInfo.getJSONObject(j).getLong("size"));
						blocksInfo.getJSONObject(j).put("size", gzipBlock.length());
//						System.out
//						.println("after " + blocksInfo.getJSONObject(j).getLong("size"));
					}
					response.put(fileInfo);
				}
				
				cos.println(response.toString());
				cos.flush();
				Loger.Log(Constant.LOG_LEVEL_INFO, "send : " + response.toString(), "DataChannel download");
				
				byte[] buf = new byte[Constant.BUFFER_SIZE];
				DataOutputStream dos = new DataOutputStream(dataSocket.getOutputStream());
				while (keepRunning)
				{
					String rcvStr = cis.readLine();
					Loger.Log(Constant.LOG_LEVEL_INFO, "receive : " + rcvStr, "DataChannel download");
					
					JSONObject blockInfo = new JSONObject(rcvStr);
					String filename = blockInfo.getString("filename");
					int seq = blockInfo.getInt("seq");
					
					String hash = "";
					for (int i = 0; i < response.length(); i ++)
					{
						if (response.getJSONObject(i).getString("filename").equals(filename))
						{
							hash = response.getJSONObject(i).getJSONArray("blocks").
									getJSONObject(seq).getString("hash");
							break;
						}
					}
					
					int read;
					File gzipBlock = new File(hash + ".gz");
					DataInputStream dis = new DataInputStream(new FileInputStream(gzipBlock));
					while ((read = dis.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
					{
						dos.write(buf, 0, read);
					}
					dos.flush();
					dis.close();
					gzipBlock.delete();
				}
				dos.close();
				dataSocket.close();
			}
			catch (NullPointerException e)
			{
				Loger.Log(Constant.LOG_LEVEL_INFO, "file download finish", "DataChannel download");
			}
			catch (SocketTimeoutException e)
			{
				Loger.Log(Constant.LOG_LEVEL_DEBUG, e.getMessage(), "DataChannel TransferThread download");
			}
			catch (JSONException | IOException e)
			{
				Loger.Log(Constant.LOG_LEVEL_ERROR, e.getMessage(), "DataChannel TransferThread download");
			}
		}
	}
	
}