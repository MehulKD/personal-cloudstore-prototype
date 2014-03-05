// filename: OpenFileDialog.java
package com.piasy.client;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class OpenFileDialog 
{
	public static String tag = "OpenFileDialog";
	static final public String sRoot = "/"; 
	static final public String sParent = "..";
	static final public String sFolder = ".";
	static final public String sEmpty = "";
	static final private String sOnErrorMsg = "No rights to access!";
	static Dialog dialog;
	
	public static Dialog createDialog(Context context, String title, 
			CallbackBundle callback, String suffix, Map<String, Integer> images)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(new FileSelectView(context, callback, suffix, images));
		dialog = builder.create();
		dialog.setTitle(title);
		return dialog;
	}
	
	static class FileSelectView extends ListView implements OnItemClickListener
	{
		
		
		private CallbackBundle callback = null;
		private String path = sRoot;
		private List<Map<String, Object>> list = null;
		
		private String suffix = null;
		
		private Map<String, Integer> imagemap = null;
		
		public FileSelectView(Context context, CallbackBundle callback,
				String suffix, Map<String, Integer> images) 
		{
			super(context);
			this.imagemap = images;
			this.suffix = (suffix == null) ? "" : suffix.toLowerCase(Locale.ENGLISH);
			this.callback = callback;
			this.setOnItemClickListener(this);
			refreshFileList();
		}
		
//		private String getSuffix(String filename)
//		{
//			int dix = filename.lastIndexOf('.');
//			if(dix < 0)
//			{
//				return "";
//			}
//			else
//			{
//				return filename.substring(dix + 1);
//			}
//		}
		
		private int getImageId(String s)
		{
			if(imagemap == null)
			{
				return 0;
			}
			else if(imagemap.containsKey(s))
			{
				return imagemap.get(s);
			}
			else if(imagemap.containsKey(sEmpty))
			{
				return imagemap.get(sEmpty);
			}
			else 
			{
				return 0;
			}
		}
		
		private int refreshFileList()
		{
			File[] files = null;
			try
			{
				files = new File(path).listFiles();
			}
			catch(Exception e)
			{
				files = null;
			}
			if(files==null)
			{
				Toast.makeText(getContext(), sOnErrorMsg,Toast.LENGTH_SHORT).show();
				return -1;
			}
			if(list != null)
			{
				list.clear();
			}
			else
			{
				list = new ArrayList<Map<String, Object>>(files.length);
			}
			
			ArrayList<Map<String, Object>> lfolders = new ArrayList<Map<String, Object>>();
			ArrayList<Map<String, Object>> lfiles = new ArrayList<Map<String, Object>>();
			
			//add root folder and parent folder
			if(!this.path.equals(sRoot))
			{
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("name", sRoot);
				map.put("path", sRoot);
				map.put("size", "Folder");
				map.put("img", getImageId(sRoot));
				list.add(map);
				
				map = new HashMap<String, Object>();
				map.put("name", sParent);
				map.put("path", path);
				map.put("size", "Folder");
				map.put("img", getImageId(sParent));
				list.add(map);
			}
			
			for(File file: files)
			{
				if(file.isDirectory() && file.listFiles() != null)
				{
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("name", file.getName());
					map.put("path", file.getPath());
					map.put("size", "Folder");
					map.put("img", getImageId(sFolder));
					lfolders.add(map);
				}
				else if(file.isFile())
				{
//					String sf = getSuffix(file.getName()).toLowerCase(Locale.ENGLISH);
//					if(suffix == null || suffix.length() == 0 
//							|| (sf.length () >0 && suffix.indexOf("." + sf + ";") >= 0))
//					{
//						Map<String, Object> map = new HashMap<String, Object>();
//						map.put("name", file.getName());
//						map.put("path", file.getPath());
//						map.put("img", getImageId(sf));
//						lfiles.add(map);
//					}
					if (suffix != null && suffix.equals(".*"))
					{
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("name", file.getName());
						map.put("path", file.getPath());
						map.put("size", getHLength(file.length()));
						map.put("img", getImageId("*"));
						lfiles.add(map);
					}
				}  
			}
			
			list.addAll(lfolders);
			list.addAll(lfiles);
			
			
			SimpleAdapter adapter = new SimpleAdapter(getContext(), 
					list, R.layout.filedialogitem, 
					new String[]{"img", "name", "size"}, 
					new int[]{R.id.filedialogitem_img, 
				R.id.filedialogitem_name, R.id.filedialogitem_path});
			this.setAdapter(adapter);
			return files.length;
		}
		
		protected String getHLength(long length)
		{
			if (length < 1024)
			{
				return "" + length + " B";
			}
			else if (length < 1024 * 1024)
			{
				float f1 = new BigDecimal((double) length / (double) 1024)
				.setScale(2, BigDecimal.ROUND_HALF_UP)
				.floatValue();
				return "" + f1 + " KB";
			}
			else if (length < 1024 * 1024 * 1024)
			{
				float f1 = new BigDecimal((double) length / (double) (1024 * 1024))
				.setScale(2, BigDecimal.ROUND_HALF_UP)
				.floatValue();
				return "" + f1 + " MB";
			}
			else
			{
				float f1 = new BigDecimal((double) length / (double) (1024 * 1024 * 1024))
				.setScale(2, BigDecimal.ROUND_HALF_UP)
				.floatValue();
				return "" + f1 + " GB";
			}
		}
		
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position, long id)
		{
			String pt = (String) list.get(position).get("path");
			String fn = (String) list.get(position).get("name");
			String size = (String) list.get(position).get("size");
			if(fn.equals(sRoot) || fn.equals(sParent))
			{
				File fl = new File(pt);
				String ppt = fl.getParent();
				if(ppt != null)
				{
					path = ppt;
				}
				else
				{
					path = sRoot;
				}
			}
			else
			{
				File fl = new File(pt);
				if(fl.isFile())
				{
					dialog.dismiss();
					Bundle bundle = new Bundle();
					bundle.putString("path", pt);
					bundle.putString("name", fn);
					bundle.putString("size", size);
					this.callback.callback(bundle);
					return;
				}
				else if(fl.isDirectory())
				{
					path = pt;
				}
			}
			this.refreshFileList();
		}
	}
}
