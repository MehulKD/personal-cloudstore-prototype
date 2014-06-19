package com.piasy.client;

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
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.piasy.client.controller.Controller;

public class MainActivity extends Activity
{

	Controller myController = null;
	
	String name = "xjl";
	TextView hint = null;
	TextView netinfoText;
	Button codingUploadButton = null;
	Button uploadButton = null;
	Button validateButton = null;
	Button networkInfo = null;
	Button updateUp, updateValid, uploadModify, staticTest, splitButton;
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
		
		hint = (TextView) findViewById(R.id.hint);
		

		staticTest = (Button) findViewById(R.id.staticSplitTest);
		splitButton  = (Button) findViewById(R.id.splitButton);
		codingUploadButton = (Button) findViewById(R.id.codingUploadButton);
		uploadButton = (Button) findViewById(R.id.uploadButton);
		
		netinfoText = (TextView) findViewById(R.id.netinfoText);
		validateButton = (Button) findViewById(R.id.validateButton);
		updateUp = (Button) findViewById(R.id.updateUpButton);
		updateValid = (Button) findViewById(R.id.updateUpValidButton);
		uploadModify = (Button) findViewById(R.id.uploadModify);
		networkInfo = (Button) findViewById(R.id.networkInfo);
		
		myController = Controller.getController();
		myController.init(myHandler, name);
//		initUI();
	}

	protected void initUI()
	{
		hint.setText("连接服务器成功");
		System.out.println("controller init ok");
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
						try
						{
							params.put("filename", filename);
							myController.upload(params);

							hint.setText("开始上传");
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
//						raptor.util.Config conf = new raptor.util.Config((int) Math.ceil((double) (600 * 1024) / Constant.K_NUM), 
//								Constant.K_NUM, Constant.OVERHEAD);
//						long start = System.currentTimeMillis();
//						System.out.println("to Encode : ");
//						Sender.encode(new File(filename), 0, 600 * 1024, conf);
//						System.out.println("Encode : " + (System.currentTimeMillis() - start));
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
				System.out
						.println("MainActivity.myHandler.new Handler() {...}.handleMessage() " + (String) msg.obj);
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
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}		
	};
}
