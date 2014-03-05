package com.piasy.client.util;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

public class MyPhoneStateListener extends PhoneStateListener
{
	private String lastStrength = "NONE";

	public void onSignalStrengthsChanged(SignalStrength signalStrength) 
	{
		super.onSignalStrengthsChanged(signalStrength);
//		lastStrength = signalStrength.getGsmSignalStrength();
		 if (signalStrength.isGsm()) 
		 {
			 if (signalStrength.getGsmSignalStrength() != 99)
			 {
				 lastStrength = "VALID GSM : " + (signalStrength.getGsmSignalStrength() * 2 - 113);
			 }
	         else
	         {
	        	 lastStrength = "INVALID GSM : " + signalStrength.getGsmSignalStrength();
	         }
	     }
		 else 
		 {
			 lastStrength = "CdmaDbm : " + signalStrength.getCdmaDbm();
	     }
	}

	public String getLastStrength() 
	{
		return lastStrength;
	}
}
