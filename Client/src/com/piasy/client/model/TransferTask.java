package com.piasy.client.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piasy.client.controller.Controller;

import android.os.Looper;
import android.util.Log;

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
	ArrayList<String> files;
	Socket channel;
	BufferedReader cis;
	PrintWriter cos;
	DataInputStream dis;
	DataOutputStream dos;
	HashMap<String, ArrayList<Block>> splitBlocks = new HashMap<String, ArrayList<Block>>();
	HashMap<String, ArrayList<Block>> transferBlocks;
	boolean fastMode = false;
	boolean finished = false;
	int port;
	
	public TransferTask(int mode, ArrayList<String> files, boolean fastMode, int port)
	{
		this.mode = mode;
		this.files = files;
		this.fastMode = fastMode;
		this.port = port;
	}
	
	public boolean finished()
	{
		return finished;
	}
	
	@Override
	public void run() 
	{
		Looper.prepare();
		File myFile = new File(files.get(0));
		File logFile = new File(Constant.LOG_FILE_NAME);
		try
		{
			logFile.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
			String logStr = myFile.getAbsolutePath() + ",";
			bw.append(logStr, 0, logStr.length());
			
			if (!fastMode)
			{
				switch (mode)
				{
				case Constant.TRANSFER_MODE_DOWNLOAD:
				{
					try
					{
						long start = System.currentTimeMillis();
						
						logStr = "0,";
						bw.append(logStr, 0, logStr.length());
						
						init();
						transferBlocks = getDownloadBlocks();
						dis = new DataInputStream(channel.getInputStream());
						for (String filename : transferBlocks.keySet())
						{
							ArrayList<Block> fileBlocks = transferBlocks.get(filename);
							for (Block block : fileBlocks)
							{
								download(block);
							}
							
							boolean succed = true;
							for (Block block : fileBlocks)
							{
								Log.d(Constant.LOG_LEVEL_DEBUG, "block : " + block.blockSeq + ", " + block.finished);
								succed = succed && block.finished;
								if (!succed)
								{
									break;
								}
							}
							
							if (succed)
							{
								merge(filename);
							}
							else
							{
								//TODO re-transfer
							}
						}
						
						long time = System.currentTimeMillis() - start;
						Controller.makeToast("下载完成，耗时" + time + "毫秒");
						logStr = time + "\n";
						bw.append(logStr, 0, logStr.length());
					}
					catch (FileNotFoundException e)
					{
						e.printStackTrace();
					}
					catch (UnsupportedEncodingException e)
					{
						e.printStackTrace();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					break;
				}
				case Constant.TRANSFER_MODE_PURE_DOWNLOAD:
				{
					JSONObject request = new JSONObject();
					JSONArray filesInfo = new JSONArray();
					try
					{
						long start = System.currentTimeMillis();

						logStr = "0,";
						bw.append(logStr, 0, logStr.length());
						
						request.put("type", "puredownload");
						for (String filename : files)
						{
							JSONObject fileInfo = new JSONObject();
							fileInfo.put("filename", filename);
							filesInfo.put(fileInfo);
						}
						request.put("files", filesInfo);

						init();
						cos.println(request.toString());
						cos.flush();

						String rcvStr = cis.readLine();
						JSONArray response = new JSONArray(rcvStr);
						for (int i = 0; i < response.length(); i ++)
						{
							pureDownload(response.getJSONObject(i).getString("filename"),
									response.getJSONObject(i).getLong("size"));
							Controller.getController()
							.transferFileFinish(response.getJSONObject(i).getString("filename"), 
									mode);
						}
						long time = System.currentTimeMillis() - start;
						Controller.makeToast("纯下载完成，耗时" + time + "毫秒");
						logStr = time + "\n";
						bw.append(logStr, 0, logStr.length());
						
						finished = true;
//						close();
						Controller.getController().transferFinish();
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					break;
				}
				case Constant.TRANSFER_MODE_PURE_UPLOAD:
				{
					JSONObject request = new JSONObject();
					JSONArray filesInfo = new JSONArray();
					try
					{
						long start = System.currentTimeMillis();

						logStr = "0,";
						bw.append(logStr, 0, logStr.length());
						
						request.put("type", "pureupload");
						for (String filename : files)
						{
							JSONObject fileInfo = new JSONObject();
							File file = new File(filename);
							File gzipFile = new File(Constant.TMP_DIR + "/" + file.getName() + ".gz");
							GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(gzipFile));
							FileInputStream fin = new FileInputStream(file);
							byte [] buf = new byte[Constant.BUFFER_SIZE];
							int read;
							while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
							{
								gos.write(buf, 0, read);
							}
							fin.close();
							gos.close();
							
							fileInfo.put("filename", filename);
							fileInfo.put("size", gzipFile.length());
							filesInfo.put(fileInfo);
						}
						request.put("files", filesInfo);

						init();
						cos.println(request.toString());
						cos.flush();
						System.out.println("send " + request.toString());

						dos = new DataOutputStream(channel.getOutputStream());
						for (String filename : files)
						{
							pureUpload(filename);
							Controller.getController().transferFileFinish(filename, mode);
						}

						long time = System.currentTimeMillis() - start;
						Controller.makeToast("纯上传完成，耗时" + time + "毫秒");
						logStr = time + "\n";
						bw.append(logStr, 0, logStr.length());
						
						finished = true;
//						close();
						Controller.getController().transferFinish();
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					break;
				}
				case Constant.TRANSFER_MODE_UPLOAD:
					try
					{
						long start = System.currentTimeMillis();
						
						JSONObject request = new JSONObject();
						JSONArray filesInfo = new JSONArray();
						request.put("type", "upload");
						for (String filename : files)
						{
							JSONObject fileInfo = new JSONObject();
							File file = new File(filename);
							
							fileInfo.put("filename", filename);
							fileInfo.put("size", file.length());
							ArrayList<Block> fileBlocks = IndexModel.CDCSplit(file);
							JSONArray blocksInfo = new JSONArray();
							for (Block block : fileBlocks)
							{
								JSONObject blockInfo = new JSONObject();
								blockInfo.put("seq", block.blockSeq);
								blockInfo.put("size", block.size);
								blockInfo.put("hash", block.hash);
								blocksInfo.put(blockInfo);
							}
							fileInfo.put("blocks", blocksInfo);
							splitBlocks.put(filename, fileBlocks);
							filesInfo.put(fileInfo);
						}
						request.put("files", filesInfo);

						long time = System.currentTimeMillis() - start;
						Controller.makeToast("切块完成，耗时" + time + "毫秒");
						logStr = time + ",";
						bw.append(logStr, 0, logStr.length());
						start = System.currentTimeMillis();
						
						init();
						dos = new DataOutputStream(channel.getOutputStream());
						transferBlocks = getUploadBlocks(request);
						Thread ackListenThread = new Thread(ackListenRunnable);
						ackListenThread.start();
						for (String filename : transferBlocks.keySet())
						{
							ArrayList<Block> fileBlocks = transferBlocks.get(filename);
							for (int i = 0; i < fileBlocks.size(); i ++)
							{
								upload(fileBlocks.get(i));
							}
						}
						
						time = System.currentTimeMillis() - start;
						Controller.makeToast("上传完成，耗时" + time + "毫秒");
						logStr = time + "\n";
						bw.append(logStr, 0, logStr.length());
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
				case Constant.TRANSFER_MODE_UPDATE_DOWN:
					break;
				case Constant.TRANSFER_MODE_UPDATE_UP_FAST:
					try
					{
						bw.append("0,");
						long start = System.currentTimeMillis();
						
						JSONObject request = new JSONObject();
						JSONArray filesInfo = new JSONArray();
						request.put("type", "updateupfast");
						for (String filename : files)
						{
							JSONObject fileInfo = new JSONObject();
							fileInfo.put("filename", filename);
							filesInfo.put(fileInfo);
						}
						request.put("files", filesInfo);
						init();
						
						cos.println(request.toString());
						cos.flush();
						
						for (String filename : files)
						{
							updateUpFast(filename);
							Controller.getController().transferFileFinish(filename, mode);
						}
						
						long time = System.currentTimeMillis() - start;
						Controller.makeToast("向上快速同步完成，耗时" + time + "毫秒");
						logStr = time + "\n";
						bw.append(logStr);
						
						finished = true;
//						close();
						Controller.getController().transferFinish();
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
				case Constant.TRANSFER_MODE_UPDATE_UP:
					try
					{
						bw.append("0,");
						long start = System.currentTimeMillis();
						
						JSONObject request = new JSONObject();
						JSONArray filesInfo = new JSONArray();
						request.put("type", "updateup");
						for (String filename : files)
						{
							JSONObject fileInfo = new JSONObject();
							fileInfo.put("filename", filename);
							filesInfo.put(fileInfo);
						}
						request.put("files", filesInfo);
						init();
						
						cos.println(request.toString());
						cos.flush();
						
						for (String filename : files)
						{
							updateUp(filename);
							Controller.getController().transferFileFinish(filename, mode);
						}
						
						long time = System.currentTimeMillis() - start;
						Controller.makeToast("向上同步完成，耗时" + time + "毫秒");
						logStr = time + "\n";
						bw.append(logStr);
						
						finished = true;
//						close();
						Controller.getController().transferFinish();
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
				default:
					break;
				}
			}
			else
			{
				switch (mode)
				{
				case Constant.TRANSFER_MODE_DOWNLOAD:
					break;
				case Constant.TRANSFER_MODE_UPLOAD:
					break;
				case Constant.TRANSFER_MODE_UPDATE_DOWN:
					break;
				case Constant.TRANSFER_MODE_UPDATE_UP:
					break;
				default:
					break;
				}
			}
			
			bw.close();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		
		Looper.loop();
	}
	
	protected boolean init()
	{
		boolean ret = false;
		try
		{
			channel = new Socket(Setting.SERVER_IP, port);
			channel.setTcpNoDelay(true);
			channel.setKeepAlive(true);
			channel.setSendBufferSize(Constant.BUFFER_SIZE);
			channel.setReceiveBufferSize(Constant.BUFFER_SIZE);
			cis = new BufferedReader(new InputStreamReader(channel.getInputStream()));
			cos = new PrintWriter(channel.getOutputStream());
		}
		catch (UnknownHostException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask init: " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask init: UnknownHostException");
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask init: " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask init: IOException");
		}
		return ret;
	}
	
	protected HashMap<String, ArrayList<Block>> getUploadBlocks(JSONObject uploadRequest)
	{
		HashMap<String, ArrayList<Block>> blocks = new HashMap<String, ArrayList<Block>>();
		try
		{		
			cos.println(uploadRequest.toString());
			cos.flush();
			
			String rcvStr;
			synchronized (cis)
			{
				rcvStr = cis.readLine();
			}
			JSONArray filesResponse = new JSONArray(rcvStr);
			for (int i = 0; i < filesResponse.length(); i ++)
			{
				JSONObject fileResponse = filesResponse.getJSONObject(i);
				String filename = fileResponse.getString("filename");
				ArrayList<Block> uploadBlocks = new ArrayList<Block>();
				JSONArray blocksResponse = fileResponse.getJSONArray("blocks");
				for (int j = 0; j < blocksResponse.length(); j ++)
				{
					JSONObject blockResponse = blocksResponse.getJSONObject(j);
					int seq = blockResponse.getInt("seq");
					String action = blockResponse.getString("action");
					if (action.equals("upload"))
					{
						ArrayList<Block> fileBlocks = splitBlocks.get(filename);
						for (Block block : fileBlocks)
						{
							if (block.blockSeq == seq)
							{
								uploadBlocks.add(block);
							}
						}
					}
					else if (action.equals("success"))
					{
						ack(filename, seq, false);
					}
				}
				
				blocks.put(filename, uploadBlocks);
			}
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask getTransferBlocks: " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask getTransferBlocks: JSONException");
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask getTransferBlocks: " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask getTransferBlocks: IOException");
		}
		return blocks;
	}
	
	protected HashMap<String, ArrayList<Block>> getDownloadBlocks()
	{
		HashMap<String, ArrayList<Block>> blocks = new HashMap<String, ArrayList<Block>>();
		try
		{
			JSONObject request = new JSONObject();
			request.put("type", "download");
			JSONArray filesInfo = new JSONArray();
			for (int i = 0; i < files.size(); i ++)
			{
				JSONObject fileInfo = new JSONObject();
				fileInfo.put("filename", files.get(i));
				filesInfo.put(fileInfo);
			}
			request.put("files", filesInfo);
			
			cos.println(request.toString());
			cos.flush();
			
			String rcvStr = cis.readLine();
			filesInfo = new JSONArray(rcvStr);
			for (int i = 0; i < filesInfo.length(); i ++)
			{
				JSONObject fileInfo = filesInfo.getJSONObject(i);
				String filename = fileInfo.getString("filename");
				JSONArray blocksInfo = fileInfo.getJSONArray("blocks");
				ArrayList<Block> downloadBlocks = new ArrayList<Block>();				
				for (int j = 0 ; j < blocksInfo.length(); j ++)
				{
					//TODO filter blocks exist in local file system
					JSONObject blockInfo = blocksInfo.getJSONObject(j);
					downloadBlocks.add(new Block(filename, blockInfo.getLong("size"), 
							0, blockInfo.getInt("seq"), blockInfo.getString("hash")));
				}
				splitBlocks.put(filename, downloadBlocks);
				blocks.put(filename, downloadBlocks);
			}
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask download : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask download : JSONException");
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask download : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask download : IOException");
		}
		return blocks;
	}
	
	protected void ack(String filename, int seq, boolean accumulate)
	{
		ArrayList<Block> fileBlocks = splitBlocks.get(filename);
		if (accumulate)
		{
			ArrayList<Block> acked = new ArrayList<Block>();
			for (int i = 0; i < fileBlocks.size(); i ++)
			{
				if (fileBlocks.get(i).blockSeq <= seq)
				{
					acked.add(fileBlocks.get(i));
				}
				else
				{
					break;
				}
			}
			fileBlocks.removeAll(acked);
		}
		else
		{
			for (int i = 0; i < fileBlocks.size(); i ++)
			{
				if (fileBlocks.get(i).blockSeq == seq)
				{
					fileBlocks.remove(i);
					break;
				}	
			}						
		}
		
		if (fileBlocks.size() == 0)
		{
			splitBlocks.remove(filename);
			Controller.getController().transferFileFinish(filename, mode);
			if (splitBlocks.isEmpty())
			{
				finished = true;
				close();
				Controller.getController().transferFinish();
			}
		}
	}
	
	protected void merge(String filename)
	{
		ArrayList<Block> blocks = splitBlocks.get(filename);
		try
		{
			int index = 0;
			for (index = filename.length() - 1; 0 <= index; index --)
			{
				if (filename.charAt(index) == '/')
				{
					break;
				}
			}
			File file = new File(Constant.APP_BASE_DIR + "/" + filename.substring(index + 1));
			file.createNewFile();
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
			byte[] buf = new byte[Constant.BUFFER_SIZE];
			for (int i = 0; i < blocks.size(); i ++)
			{
				File partFile = new File(Constant.TMP_DIR + "/" + blocks.get(i).hash);
				DataInputStream dis = new DataInputStream(new FileInputStream(partFile));
				int read;
				while ((read = dis.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
				{
					dos.write(buf, 0, read);
				}
				dis.close();
				partFile.delete();
			}
			dos.close();
			Controller.getController().transferFileFinish(filename, mode);
			
			splitBlocks.remove(filename);
			if (splitBlocks.isEmpty())
			{
				finished = true;
				close();
				Controller.getController().transferFinish();
			}
		}
		catch (FileNotFoundException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask merge : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask merge : FileNotFoundException");
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask merge : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask merge : IOException");
		}
	}
	
	protected void updateUp(String filename)
	{
		try
		{
			//read sig file info
			String sigInfoStr = cis.readLine();
			JSONObject sigInfo = new JSONObject(sigInfoStr);
			File file = new File(filename);
			if (sigInfo.getString("filename").equals(filename))
			{
				//receive sig gzip file
				long size = sigInfo.getLong("size");
				byte [] buf = new byte[Constant.BUFFER_SIZE];
				dis = new DataInputStream(channel.getInputStream());
				File sigGzipFile = new File(Constant.TMP_DIR + "/" + file.getName() + ".sig.gz");
				FileOutputStream fout = new FileOutputStream(sigGzipFile);
				long totalRead = 0;
				int read;
				while (totalRead < size)
				{
					read = dis.read(buf, 0, 
							(int) ((size - totalRead < Constant.BUFFER_SIZE) ? 
									(size - totalRead) : Constant.BUFFER_SIZE));
					fout.write(buf, 0, read);
					System.out.println("read sig " + read + " bytes");
					if (read == -1)
					{
						continue;
					}
					totalRead += read;
				}
				fout.flush();
				fout.close();
				
				//uncompress sig file
				GZIPInputStream gis = new GZIPInputStream(new FileInputStream(sigGzipFile));
				File sigFile = new File(Constant.TMP_DIR + "/" + file.getName() + ".sig");
				fout = new FileOutputStream(sigFile);
				while ((read = gis.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
				{
					fout.write(buf, 0, read);
				}
				gis.close();
				fout.flush();
				fout.close();
				
				//gen delta file, and compress it
				File deltaFile = new File(Constant.TMP_DIR + "/" + file.getName() + ".delta");
				RsyncModel.genDelta(sigFile.getAbsolutePath(), filename, deltaFile.getAbsolutePath());
				File deltaGzipFile = new File(Constant.TMP_DIR + "/" + file.getName() + ".delta.gz");
				FileInputStream fin = new FileInputStream(deltaFile);
				GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(deltaGzipFile));
				while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
				{
					gos.write(buf, 0, read);
				}
				fin.close();
				gos.flush();
				gos.finish();
				gos.close();
				
				//send delta file info
				JSONObject deltaInfo = new JSONObject();
				deltaInfo.put("filename", filename);
				deltaInfo.put("size", deltaGzipFile.length());
				cos.println(deltaInfo.toString());
				cos.flush();
				Thread.sleep(1000);
				
				//send delta gzip file
				dos = new DataOutputStream(channel.getOutputStream());
				fin = new FileInputStream(deltaGzipFile);
				while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
				{
					dos.write(buf, 0, read);
					System.out.println("send " + read + " bytes");
				}
				dos.flush();
				fin.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	protected void updateUpFast(String filename)
	{
		try
		{
			File oldFile = new File(filename);
			File newFile = new File(Constant.TMP_DIR + "/" + oldFile.getName());
			//gen sig file
			File sigFile = new File(Constant.TMP_DIR + "/" + oldFile.getName() + ".sig");
			RsyncModel.genSignature(oldFile.getAbsolutePath(), sigFile.getAbsolutePath());
			
			//gen delta file, and compress it
			File deltaFile = new File(Constant.TMP_DIR + "/" + oldFile.getName() + ".delta");
			RsyncModel.genDelta(sigFile.getAbsolutePath(), newFile.getAbsolutePath(), 
					deltaFile.getAbsolutePath());
			File deltaGzipFile = new File(Constant.TMP_DIR + "/" + oldFile.getName() + ".delta.gz");
			FileInputStream fin = new FileInputStream(deltaFile);
			GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(deltaGzipFile));
			int read;
			byte [] buf= new byte[Constant.BUFFER_SIZE];
			while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
			{
				gos.write(buf, 0, read);
			}
			fin.close();
			gos.flush();
			gos.finish();
			gos.close();
			
			//send delta file info
			JSONObject deltaInfo = new JSONObject();
			deltaInfo.put("filename", filename);
			deltaInfo.put("size", deltaGzipFile.length());
			cos.println(deltaInfo.toString());
			cos.flush();
			
			Thread.sleep(1000);
			//send delta gzip file
			dos = new DataOutputStream(channel.getOutputStream());
			fin = new FileInputStream(deltaGzipFile);
			while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
			{
				dos.write(buf, 0, read);
				System.out.println("send " + read + " bytes");
				dos.flush();
			}
			dos.flush();
			fin.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	protected void updateDown(String filename)
	{
		
	}
	
	protected void pureDownload(String filename, long size)
	{
		try
		{
			//receive gzip file
			File tmp = new File(filename);
			DataInputStream dis = new DataInputStream(channel.getInputStream());
			File gzipFile = new File(Constant.APP_BASE_DIR + "/" + tmp.getName() + ".gz");
			FileOutputStream fout = new FileOutputStream(gzipFile);
			long totalRead = 0;
			int read;
			byte [] buf = new byte[Constant.BUFFER_SIZE];
			while (totalRead < size)
			{
				read = dis.read(buf, 0, Constant.BUFFER_SIZE);
				System.out.println("read " + read + " bytes");
				totalRead += read;
				fout.write(buf, 0, read);
			}
			fout.close();
			
			//uncompress it
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzipFile));
			File file = new File(Constant.APP_BASE_DIR + "/" + tmp.getName());
			fout = new FileOutputStream(file);
			while ((read = gis.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
			{
				fout.write(buf, 0, read);
			}
			gis.close();
			fout.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	protected void pureUpload(String filename)
	{
		try
		{
			File file = new File(filename);
			File gzipFile = new File(Constant.TMP_DIR + "/" + file.getName() + ".gz");
			FileInputStream fin = new FileInputStream(gzipFile);
			byte buf[] = new byte[Constant.BUFFER_SIZE];
			int read;
			int total = 0;
			long size = gzipFile.length();
			DataOutputStream dos = new DataOutputStream(channel.getOutputStream());
			while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
			{
				dos.write(buf, 0, read);
				dos.flush();
				total += read;
				System.out.println("send " + read + " bytes, " + total + "/" + size);
			}
			dos.flush();
			fin.close();
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask upload : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask upload : IOException");
		}
	}
	
	/**
	 * Upload method, it will create a new thread to upload this block to 
	 * server.
	 * */
	protected void upload(Block block)
	{
		try
		{
			RandomAccessFile dis = new RandomAccessFile(new File(block.filename), "r");
			dis.seek(block.offset);
			byte buf[] = new byte[Constant.BUFFER_SIZE];
			long totalRead = 0;
			int read;
			File gzipBlock = new File(Constant.TMP_DIR + "/" + block.hash + ".gz");
			GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(gzipBlock));
			while (totalRead < block.size)
			{
				read = dis.read(buf, 0, 
						(int) ((block.size - totalRead < Constant.BUFFER_SIZE) ? 
								(block.size - totalRead) : Constant.BUFFER_SIZE));
				totalRead += read;
				gos.write(buf, 0, read);
			}
			gos.flush();
			gos.close();
			dis.close();
			
			ByteBuffer byteBuffer = ByteBuffer.allocate(44);
			Log.d(Constant.LOG_LEVEL_DEBUG, "upload size = " + gzipBlock.length());
			byteBuffer.putInt(block.blockSeq);
			byteBuffer.putLong(gzipBlock.length());
			byteBuffer.put(block.hash.getBytes());
			dos = new DataOutputStream(channel.getOutputStream());
			dos.write(byteBuffer.array());
			dos.flush();
				
			FileInputStream fin = new FileInputStream(gzipBlock);
			while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
			{
				dos.write(buf, 0, read);
			}
			dos.flush();
			fin.close();
			gzipBlock.delete();
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask upload : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask upload : IOException");
		}
	}
	
	protected void download(Block block)
	{
		try
		{
			JSONObject header = new JSONObject();
			header.put("filename", block.filename);
			header.put("seq", block.blockSeq);
			
			cos.println(header.toString());
			cos.flush();
			Log.d(Constant.LOG_LEVEL_DEBUG, "send : " + header.toString());
			
			byte[] buf = new byte[Constant.BUFFER_SIZE];
			File gzipPartFile = new File(Constant.TMP_DIR + "/" + block.hash + ".gz");
			gzipPartFile.createNewFile();
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(gzipPartFile));
			long totalRead = 0;
			int read;
			System.out.println("filename " + block.filename + ", seq" + block.blockSeq + ", size " + block.size);
			while (totalRead < block.size)
			{
				read = dis.read(buf, 0, 
						(int) ((block.size - totalRead < Constant.BUFFER_SIZE) ? 
								(block.size - totalRead) : Constant.BUFFER_SIZE));
				System.out.println("read " + read);
				dos.write(buf, 0, read);
				totalRead += read;
			}
			dos.close();
			
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzipPartFile));
			File orgPartFile = new File(Constant.TMP_DIR + "/" + block.hash);
			FileOutputStream fout = new FileOutputStream(orgPartFile);
			while ((read = gis.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
			{
				fout.write(buf, 0, read);
			}
			fout.flush();
			gis.close();
			fout.close();
			gzipPartFile.delete();
			block.finished = true;
		}
		catch (JSONException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask download : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask download : JSONException");
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask download : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask download : IOException");
		}
	}
	
	protected void close()
	{
		try
		{
			cos.close();
			cis.close();
			channel.close();
		}
		catch (IOException e)
		{
			if (e.getMessage() != null)
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask close : " + e.getMessage());
			else
				Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask close : IOException");
		}
	}
	
	Runnable ackListenRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			while (!finished)
			{
				synchronized (cis)
				{
					try
					{
						String rcvStr = cis.readLine();
						
						Log.d(Constant.LOG_LEVEL_DEBUG, "receive : " + rcvStr);
						
						JSONObject info = new JSONObject(rcvStr);
						String type = info.getString("type");
						if (type.equals("ack"))
						{
							String filename = info.getString("filename");
							int seq = info.getInt("seq");
							ack(filename, seq, true);
						}
					}
					catch (IOException e)
					{
						if (e.getMessage() != null)
							Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask ackListenRunnable : " + e.getMessage());
						else
							Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask ackListenRunnable : IOException");
					}
					catch (JSONException e)
					{
						if (e.getMessage() != null)
							Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask ackListenRunnable : " + e.getMessage());
						else
							Log.e(Constant.LOG_LEVEL_ERROR, "at TransferTask ackListenRunnable : JSONException");
					}
				}
			}
		}
	};
}
