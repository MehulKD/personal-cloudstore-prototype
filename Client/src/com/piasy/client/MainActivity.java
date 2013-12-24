package com.piasy.client;

import org.json.JSONException;
import org.json.JSONObject;

import com.piasy.client.controller.Controller;
import com.piasy.client.dao.DBManager;
import com.piasy.client.model.Constant;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{

	Controller myController = null;
	DBManager dbManager = null;
	
	String name = "xjl";
	TextView hint = null;
	Button queryButton = null;
	Button uploadButton = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		dbManager = new DBManager(getApplicationContext());
		
		hint = (TextView) findViewById(R.id.hint);
		queryButton = (Button) findViewById(R.id.queryButton);
		uploadButton = (Button) findViewById(R.id.uploadButton);
		
		myController = Controller.getController();
		if (myController.init(myHandler, dbManager, name))
		{
			Log.d(Constant.LOG_LEVEL_DEBUG, "controller init ok");
			queryButton.setOnClickListener(new View.OnClickListener()
			{
				
				@Override
				public void onClick(View v)
				{
					Log.d(Constant.LOG_LEVEL_DEBUG, "click query button");
					myController.query();
				}
			});
			
			
			uploadButton.setOnClickListener(new View.OnClickListener()
			{
				
				@Override
				public void onClick(View v)
				{
					String filename = Environment.getExternalStorageDirectory().getPath() + "/360.apk";
					//String filename = Environment.getExternalStorageDirectory().getPath() + "/PlanTravelTrace/oldnew.txt";
					//TODO get file name here
					Log.d(Constant.LOG_LEVEL_DEBUG, "click upload button");
					myController.upload(filename, 0);
				}
			});
			
		}
		else
		{
			Log.d(Constant.LOG_LEVEL_DEBUG, "controller init fail");
		}
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
				JSONObject info = new JSONObject((String) msg.obj);
				String type = info.getString("type");
				if (type.equals("query"))
				{
					hint.setText(info.toString());
				}
				else
				{
					if (type.equals("upload"))
					{
						if (info.getString("status").equals("success"))
						{
							Toast.makeText(getApplicationContext(), "Upload success", Toast.LENGTH_LONG).show();
							myController.query();
						}
						else
						{
							Toast.makeText(getApplicationContext(), info.getString("status"), Toast.LENGTH_LONG).show();
						}
					}
					else
					{
						
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
