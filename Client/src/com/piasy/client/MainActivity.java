package com.piasy.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.piasy.client.controller.Controller;
import com.piasy.client.dao.DBManager;
import com.piasy.client.model.Constant;
import com.piasy.client.model.IndexModel;
import com.piasy.client.util.NetworkUtil;

public class MainActivity extends Activity
{

	Controller myController = null;
	DBManager dbManager = null;
	
	String name = "xjl";
	TextView hint = null;
	TextView netinfoText;
	Button pureUploadButton = null;
	Button uploadButton = null;
	Button validateButton = null;
	Button networkInfo = null;
	Button updateUp, updateValid, uploadModify;
	long filesize = 0;
	String validateFile = "";
	JSONObject validateParams = new JSONObject();
	String pureValidateFile = "";
	JSONObject pureValidateParams = new JSONObject();
	
	boolean download = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Controller.setContext(getApplicationContext());
		NetworkUtil.setContext(getApplicationContext());
		
		dbManager = new DBManager(getApplicationContext());
		
		hint = (TextView) findViewById(R.id.hint);
		netinfoText = (TextView) findViewById(R.id.netinfoText);
		pureUploadButton = (Button) findViewById(R.id.pureUploadButton);
		uploadButton = (Button) findViewById(R.id.uploadButton);
		validateButton = (Button) findViewById(R.id.validateButton);
		updateUp = (Button) findViewById(R.id.updateUpButton);
		updateValid = (Button) findViewById(R.id.updateUpValidButton);
		uploadModify = (Button) findViewById(R.id.uploadModify);
		networkInfo = (Button) findViewById(R.id.networkInfo);
		networkInfo.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				netinfoText.setText(NetworkUtil.getNetworkInfo());
				
//				RsyncModel.genSignature(Environment.getExternalStorageDirectory().getPath() + "/RsyncTest/old/1.txt", 
//						Environment.getExternalStorageDirectory().getPath() + "/RsyncTest/tmp/1.txt.sig");
//				RsyncModel.genDelta(Environment.getExternalStorageDirectory().getPath() + "/RsyncTest/tmp/1.txt.sig",
//						Environment.getExternalStorageDirectory().getPath() + "/RsyncTest/new/1.txt", 
//						Environment.getExternalStorageDirectory().getPath() + "/RsyncTest/tmp/1.txt.delta");
//				RsyncModel.patch(Environment.getExternalStorageDirectory().getPath() + "/RsyncTest/old/1.txt", 
//						Environment.getExternalStorageDirectory().getPath() + "/RsyncTest/tmp/1.txt.delta", 
//						Environment.getExternalStorageDirectory().getPath() + "/RsyncTest/tmp/1.txt.new");
			}
		});
		
		myController = Controller.getController();
		myController.init(myHandler, dbManager, name);
		
	}

	protected void initUI()
	{
		hint.setText("连接服务器成功");
		
		Log.d(Constant.LOG_LEVEL_DEBUG, "controller init ok");
		pureUploadButton.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				Map<String, Integer> images = new HashMap<String, Integer>();
				images.put(OpenFileDialog.sRoot, R.drawable.filedialog_root);
				images.put(OpenFileDialog.sParent, R.drawable.filedialog_folder_up);
				images.put(OpenFileDialog.sFolder, R.drawable.filedialog_folder);
				images.put("*", R.drawable.filedialog_file);
				images.put(OpenFileDialog.sEmpty, R.drawable.filedialog_root);
				Dialog dialog = OpenFileDialog.createDialog(MainActivity.this, "Chose FIle", new CallbackBundle() 
				{
					@Override
					public void callback(Bundle bundle) 
					{
						String filename = bundle.getString("path");
						
						JSONObject params = new JSONObject();
						JSONArray files = new JSONArray();
						JSONObject file = new JSONObject();
						try
						{
							file.put("filename", filename);
							files.put(file);
							params.put("files", files);
							myController.pureUpload(params);
							
							pureValidateFile = filename;
							pureValidateParams = params;
							
							hint.setText("开始纯上传");
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
				}, 
				".*",
				images);
				
				dialog.show();
			}
		});
				
		uploadButton.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				Map<String, Integer> images = new HashMap<String, Integer>();
				images.put(OpenFileDialog.sRoot, R.drawable.filedialog_root);
				images.put(OpenFileDialog.sParent, R.drawable.filedialog_folder_up);
				images.put(OpenFileDialog.sFolder, R.drawable.filedialog_folder);
				images.put("*", R.drawable.filedialog_file);
				images.put(OpenFileDialog.sEmpty, R.drawable.filedialog_root);
				Dialog dialog = OpenFileDialog.createDialog(MainActivity.this, "Chose FIle", new CallbackBundle() 
				{
					@Override
					public void callback(Bundle bundle) 
					{
						String filename = bundle.getString("path");
						
						JSONObject params = new JSONObject();
						JSONArray files = new JSONArray();
						JSONObject file = new JSONObject();
						try
						{
							file.put("filename", filename);
							files.put(file);
							params.put("files", files);
							myController.upload(params);

							hint.setText("开始上传");
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
				}, 
				".*",
				images);
				
				dialog.show();
			}
		});
		
		updateUp.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				Map<String, Integer> images = new HashMap<String, Integer>();
				images.put(OpenFileDialog.sRoot, R.drawable.filedialog_root);
				images.put(OpenFileDialog.sParent, R.drawable.filedialog_folder_up);
				images.put(OpenFileDialog.sFolder, R.drawable.filedialog_folder);
				images.put("*", R.drawable.filedialog_file);
				images.put(OpenFileDialog.sEmpty, R.drawable.filedialog_root);
				Dialog dialog = OpenFileDialog.createDialog(MainActivity.this, "Chose FIle", new CallbackBundle() 
				{
					@Override
					public void callback(Bundle bundle) 
					{
						String filename = bundle.getString("path");
						
						JSONObject params = new JSONObject();
						JSONArray files = new JSONArray();
						JSONObject file = new JSONObject();
						try
						{
							file.put("filename", filename);
							files.put(file);
							params.put("files", files);
							myController.updateUp(params);

							validateParams = params;
							hint.setText("开始增量同步上传");
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
				}, 
				".*",
				images);
				
				dialog.show();
			}
		});
		
		validateButton.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				Map<String, Integer> images = new HashMap<String, Integer>();
				images.put(OpenFileDialog.sRoot, R.drawable.filedialog_root);
				images.put(OpenFileDialog.sParent, R.drawable.filedialog_folder_up);
				images.put(OpenFileDialog.sFolder, R.drawable.filedialog_folder);
				images.put("*", R.drawable.filedialog_file);
				images.put(OpenFileDialog.sEmpty, R.drawable.filedialog_root);
				Dialog dialog = OpenFileDialog.createDialog(MainActivity.this, "Chose FIle", new CallbackBundle() 
				{
					@Override
					public void callback(Bundle bundle) 
					{
						String filename = bundle.getString("path");
						validateFile = filename;
						
						JSONObject params = new JSONObject();
						JSONArray files = new JSONArray();
						JSONObject file = new JSONObject();
						try
						{
							file.put("filename", filename);
							files.put(file);
							params.put("files", files);
							myController.upload(params);
							
							validateParams = params;

							hint.setText("开始验证（上传）");
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
				}, 
				".*",
				images);
				
				dialog.show();
			}
		});
		
		updateValid.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				myController.download(validateParams);
				hint.setText("开始验证（下载）");
				try
				{
					validateFile = validateParams.getJSONArray("files").getJSONObject(0).getString("filename");
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		});
		
		uploadModify.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				Map<String, Integer> images = new HashMap<String, Integer>();
				images.put(OpenFileDialog.sRoot, R.drawable.filedialog_root);
				images.put(OpenFileDialog.sParent, R.drawable.filedialog_folder_up);
				images.put(OpenFileDialog.sFolder, R.drawable.filedialog_folder);
				images.put("*", R.drawable.filedialog_file);
				images.put(OpenFileDialog.sEmpty, R.drawable.filedialog_root);
				Dialog dialog = OpenFileDialog.createDialog(MainActivity.this, "Chose FIle", new CallbackBundle() 
				{
					@Override
					public void callback(Bundle bundle) 
					{
						String filename = bundle.getString("path");
						
						JSONObject params = new JSONObject();
						JSONArray files = new JSONArray();
						JSONObject file = new JSONObject();
						try
						{
							file.put("filename", filename);
							files.put(file);
							params.put("files", files);
							
							File org = new File(filename);
							File modify = new File(Constant.TMP_DIR + "/" + org.getName());
							FileInputStream fin = new FileInputStream(org);
							FileOutputStream fout = new FileOutputStream(modify);
							byte [] buf = new byte[Constant.BUFFER_SIZE];
							int read;
							while ((read = fin.read(buf, 0, Constant.BUFFER_SIZE)) != -1)
							{
								double rand = Math.random();
								if (rand < Constant.MODIFY_P)
								{
									int offset = (int) (rand * read);
									fout.write(buf, offset, read - offset);
								}
								else if (1 - Constant.MODIFY_P < rand)
								{
									fout.write(("" + System.currentTimeMillis()).getBytes());
									fout.write(buf, 0, read);
								}
								else
								{
									fout.write(buf, 0, read);
								}
								
							}
							fin.close();
							fout.close();
							
							myController.updateUpFast(params);
							
							validateParams = params;
							
							hint.setText("开始同步上传（修改后）");
						}
						catch (JSONException e)
						{
							e.printStackTrace();
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
				}, 
				".*",
				images);
				
				dialog.show();
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		myController.exit();
	}
	
	@SuppressLint("HandlerLeak")
	Handler myHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			try
			{
				Log.d(Constant.LOG_LEVEL_DEBUG, (String) msg.obj);
				JSONObject info = new JSONObject((String) msg.obj);
				String type = info.getString("type");
				if (type.equals("init"))
				{
					if (info.getString("status").equals("success"))
					{
						initUI();
					}
					else
					{
						hint.setText("连接服务器失败");
					}
				}
				else if (type.equals("query"))
				{
//					hint.setText(info.toString());
					JSONArray content = info.getJSONArray("content");
					if (0 < content.length())
					{
						JSONObject item = content.getJSONObject(0);
						filesize = item.getLong("size");
					}
					
				}
				else if (type.equals("upload"))
				{
					if (info.getString("status").equals("success"))
					{
						Toast.makeText(getApplicationContext(), "Upload success", Toast.LENGTH_LONG).show();
						if (info.getString("filename").equals(validateFile))
						{
							try
							{
								Thread.sleep(10 * 1000);
							}
							catch (InterruptedException e)
							{
								e.printStackTrace();
							}
							myController.download(validateParams);
							hint.setText("开始验证（下载）");
						}
						else if (info.getString("filename").equals(pureValidateFile))
						{
							try
							{
								Thread.sleep(10 * 1000);
							}
							catch (InterruptedException e)
							{
								e.printStackTrace();
							}
							myController.pureDownload(pureValidateParams);
							hint.setText("开始验证（纯下载）");
						}
						else
						{
							hint.setText("上传成功");
						}
//						myController.query();
						
					}
					else
					{
						hint.setText("上传失败");
						Toast.makeText(getApplicationContext(), info.getString("status"), Toast.LENGTH_LONG).show();
					}
				}
				else if (type.equals("download"))
				{
					if (info.getString("status").equals("success"))
					{
						if (info.getString("filename").equals(validateFile))
						{
							File origin = new File(validateFile);
							File passed = new File(Constant.APP_BASE_DIR + "/" + origin.getName());
							if (IndexModel.getMD5(origin).equals(IndexModel.getMD5(passed)))
							{
								hint.setText("验证成功");
								Toast.makeText(getApplicationContext(), 
										"Validate success", Toast.LENGTH_LONG).show();
							}
							else
							{
								Toast.makeText(getApplicationContext(), 
										"Validate fail", Toast.LENGTH_LONG).show();
								hint.setText("验证失败");
							}
						}
						else if (info.getString("filename").equals(pureValidateFile))
						{
							File origin = new File(pureValidateFile);
							File passed = new File(Constant.APP_BASE_DIR + "/" + origin.getName());
							String oldDigest = IndexModel.getMD5(origin);
							System.out
									.println(origin.getAbsolutePath());
							String newDigest = IndexModel.getMD5(passed);
							if (oldDigest != null 
									&& newDigest != null 
									&& oldDigest.equals(newDigest))
							{
								hint.setText("验证成功");
								Toast.makeText(getApplicationContext(), 
										"Validate success", Toast.LENGTH_LONG).show();
							}
							else
							{
								Toast.makeText(getApplicationContext(), 
										"Validate fail", Toast.LENGTH_LONG).show();
								hint.setText("验证失败");
							}
						}
						else
						{
							hint.setText("下载成功");
						}
					}
					else
					{
						hint.setText("下载失败");
						Toast.makeText(getApplicationContext(), 
								info.getString("status"), Toast.LENGTH_LONG).show();
					}
				}
			}
			catch (JSONException e)
			{
				if (e.getMessage() != null)
					Log.e(Constant.LOG_LEVEL_ERROR, "at MainActivity Handler : " + e.getMessage());
				else
					Log.e(Constant.LOG_LEVEL_ERROR, "at MainActivity Handler : JSONException");
			}
		}		
	};
}
