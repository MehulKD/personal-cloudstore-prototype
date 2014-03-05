package com.piasy.client.util;

import com.piasy.client.controller.Controller;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class NetworkUtil
{
	static Context context;
	static MyPhoneStateListener phoneStateListener;
	public static void setContext(Context cont)
	{
		context = cont;
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		phoneStateListener = new MyPhoneStateListener();
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}
	
	public static String getNetworkInfo()
	{
		StringBuffer sInfo = new StringBuffer();
		
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo mobNetInfo = connectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE); 
		if (mobNetInfo != null)
		{
			switch (mobNetInfo.getSubtype())
			{
			case TelephonyManager.NETWORK_TYPE_HSDPA:
				Controller.makeToast("联通3G");
				sInfo.append("联通3G ");
				break;
			case TelephonyManager.NETWORK_TYPE_EVDO_0:
			case TelephonyManager.NETWORK_TYPE_EVDO_A:
			case TelephonyManager.NETWORK_TYPE_EVDO_B:
				Controller.makeToast("电信3G");
				sInfo.append("电信3G ");
				break;
			case TelephonyManager.NETWORK_TYPE_GPRS:
				Controller.makeToast("移动2G");
				sInfo.append("移动2G ");
				break;
			case TelephonyManager.NETWORK_TYPE_EDGE:
				Controller.makeToast("联通2G");
				sInfo.append("联通2G ");
				break;
			case TelephonyManager.NETWORK_TYPE_CDMA:
				Controller.makeToast("电信2G");
				sInfo.append("电信2G ");
				break;
			default:
				Controller.makeToast("未知类型/网络关闭");
				sInfo.append("未知类型/网络关闭 ");
				System.out.println(mobNetInfo.getSubtype() + ", " + mobNetInfo.getSubtypeName());
				break;
			}
		}
		sInfo.append(phoneStateListener.getLastStrength() + "\n");
		System.out.println(phoneStateListener.getLastStrength());
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); 
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if (wifiInfo != null)
		{
			System.out.println("getBSSID " + wifiInfo.getBSSID());
			System.out.println("getSSID " + wifiInfo.getSSID());
			System.out.println("getIpAddress " + wifiInfo.getIpAddress());
			System.out.println("getMacAddress " + wifiInfo.getMacAddress());
			System.out.println("getNetworkId " + wifiInfo.getNetworkId());
			String units = WifiInfo.LINK_SPEED_UNITS;
			System.out.println("getLinkSpeed " + wifiInfo.getLinkSpeed() + " " + units);
			System.out.println("getRssi(信号强度，5级) " + WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5));
			
			sInfo.append("getBSSID " + wifiInfo.getBSSID() + "\n");
			sInfo.append("getSSID " + wifiInfo.getSSID() + "\n");
			sInfo.append("getIpAddress " + wifiInfo.getIpAddress() + "\n");
			sInfo.append("getMacAddress " + wifiInfo.getMacAddress() + "\n");
			sInfo.append("getNetworkId " + wifiInfo.getNetworkId() + "\n");
			sInfo.append("getLinkSpeed " + wifiInfo.getLinkSpeed() + " " + units + "\n");
			sInfo.append("getRssi(信号强度，5级) " + WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5) + "\n");
		}
		
//        StringBuffer sInfo = new StringBuffer();
//        
//
//        if (connectivity != null)  
//        { 
//
//    		NetworkInfo activeNetInfo = connectivity.getActiveNetworkInfo();  
//        	if (activeNetInfo != null)  
//        	{  
//        		System.out.println("XYZ_NetState_|TypeName:" + activeNetInfo.getTypeName()
//        				+ "|Type:"+activeNetInfo.getType() + "|State:" + activeNetInfo.getState()
//        				+ "|ExtraInfo:" + activeNetInfo.getExtraInfo() 
//        				+ "|Reason:"+activeNetInfo.getReason() + "|SubtypeName:" 
//        				+ activeNetInfo.getSubtypeName() + "|Subtype:" 
//        				+ activeNetInfo.getSubtype() + "|DetailedState:"
//        				+ activeNetInfo.getDetailedState());
//            }  
//
//        	if(mobNetInfo != null)  
//        	{  
//        		System.out.println("XYZ_NetState_|TypeName:" + mobNetInfo.getTypeName()
//        				+ "|Type:"+mobNetInfo.getType() + "|State:" + mobNetInfo.getState()
//        				+ "|ExtraInfo:" + mobNetInfo.getExtraInfo() 
//        				+ "|Reason:"+mobNetInfo.getReason() + "|SubtypeName:" 
//        				+ mobNetInfo.getSubtypeName() + "|Subtype:" 
//        				+ mobNetInfo.getSubtype() + "|DetailedState:"
//        				+ mobNetInfo.getDetailedState()); 
//        	}
//
//        	NetworkInfo[] info = connectivity.getAllNetworkInfo();   
//
//        	if (info != null) 
//        	{       
//        		for (int i = 0; i < info.length; i++) 
//        		{          
//        			if (info[i].getState() == NetworkInfo.State.CONNECTED) 
//        			{
//        				Log.d("", "XYZ_isconnect");
//        			}
//        			else
//        			{
//
//        				sInfo.append("\nDetailedState:"+info[i].getDetailedState());
//
//				       sInfo.append("\nState:"+info[i].getState());
//				
//				       sInfo.append("\nType:"+info[i].getType());
//				
//				       sInfo.append("\nTypeName:"+info[i].getTypeName());
//				
//				       sInfo.append("\nExtraInfo:"+info[i].getExtraInfo());
//				
//				       sInfo.append("\nReason:"+info[i].getReason());
//				
//				       sInfo.append("\nSubtype:"+info[i].getSubtype());
//				
//				       sInfo.append("\nSubtypeName:"+info[i].getSubtypeName());
//				
//				       sInfo.append("\n");
//
//        			}
//        		}    
//        	}
//
//        }  
//
//       System.out.println(sInfo.toString());

        return sInfo.toString();

	}
}
