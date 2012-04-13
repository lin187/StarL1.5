/**
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 *
 *  Copyright (c) 2009 by Harald Mueller and Sofia Lemons.
 */

package boeing.awt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import boeing.awt.R;
//import android.telephony.TelephonyManager;
import android.tether.system.CoreTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Connect extends Activity implements OnClickListener{

	public static final String TAG = "Ad Hoc NW";
	// a class implemented by Author of wifi-tehter, which allows us to execute
	//    command line demand in the app
	private CoreTask coretask;
	
	private Button buttonCreate;
	private Button buttonCheck;
	private Spinner spinner_ssid;
	private Spinner spinner_subnet;	
	private TextView textInfo;
	private WifiManager mWifi;	
	
	// Default ID
	//private int ID = 0;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect);

		// List all ssids defined in strings.xml
		spinner_ssid = (Spinner) findViewById(R.id.spinner_ssid);
		ArrayAdapter<CharSequence> adapter_ssid = ArrayAdapter.createFromResource(
				this, R.array.ssid_array, android.R.layout.simple_spinner_item);
		adapter_ssid.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_ssid.setAdapter(adapter_ssid);

		// List all subnets defined in strings.xml
		spinner_subnet = (Spinner) findViewById(R.id.spinner_subnet);
		ArrayAdapter<CharSequence> adapter_subnet = ArrayAdapter.createFromResource(
				this, R.array.subnet_array, android.R.layout.simple_spinner_item);
		adapter_subnet.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_subnet.setAdapter(adapter_subnet);
			
		this.coretask = new CoreTask();

		buttonCreate = (Button) findViewById(R.id.button_create);
		buttonCreate.setOnClickListener(this);

		buttonCheck = (Button) findViewById(R.id.button_check);
		buttonCheck.setOnClickListener(this);
		
		textInfo = (TextView) findViewById(R.id.Info);
		
		// Setup WiFi
		mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		// Turn off wifi if it's on
		if(this.mWifi.isWifiEnabled()){
			
			mWifi.setWifiEnabled(false);
			Log.d(TAG, "Wifi disabled!");
            // Waiting for interface-shutdown
            try {
                    Thread.sleep(1000);
            } catch (InterruptedException e) {
                    // nothing
            }
		}
				
		// Use Phone number (the last 2 digits) to generate ID
		//TelephonyManager tMgr =(TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		//String mPhoneNumber = tMgr.getLine1Number();
		
		//ID = Integer.parseInt(mPhoneNumber.substring(8).trim());	
		//ID = 101;
		//Log.d(TAG, "ID: " + ID);
	}

	private void connect(String SSID){
		
		this.coretask.setPath(this.getApplicationContext().getFilesDir().getParent());
		boolean a = false;
		a = this.coretask.runRootCommand("insmod /system/modules/bcm4329.ko");
		System.out.println(a);
		this.coretask.runRootCommand("iwconfig eth0 mode ad-hoc");
		String cmd = "iwconfig eth0 essid "+SSID;		
		Log.e(TAG, "cmd: " + cmd);
		this.coretask.runRootCommand(cmd);
		
		String IpAddress = spinner_subnet.getItemAtPosition(spinner_subnet.getSelectedItemPosition()).toString();
		//subnet = "192.168.1.";
		cmd = "ifconfig eth0 " + IpAddress;
		this.coretask.runRootCommand(cmd);		
		Toast.makeText(this, "Connect to " + SSID + " with new IP: " + IpAddress,
				Toast.LENGTH_LONG).show();
	}
	

	// Show the result of netcfg and iwconfig in the app
	// The following code is adapted from the following website:
	//   http://gimite.net/en/index.php?Run%20native%20executable%20in%20Android%20App
	private void check(){
		try {
			
			textInfo.setText("Info:\n#netcfg\n");
				
		    // Check configuration:
			// Change to the directory
			this.coretask.setPath("/system/bin/");
		    Process process = Runtime.getRuntime().exec("netcfg");
		    // Reads stdout.		    
		    BufferedReader reader = new BufferedReader(
		            new InputStreamReader(process.getInputStream()));
		    int read;
		    char[] buffer = new char[4096];
		    StringBuffer output = new StringBuffer();
		    while ((read = reader.read(buffer)) > 0) {
		        output.append(buffer, 0, read);
		    }
		    textInfo.append(output.toString());
		    textInfo.append("\n\n\n#iwconfig\n");
		    
		    
		    // Check interface:
			// Change to the directory
			this.coretask.setPath("/data/data/android.tether/bin/");		    
		    process = Runtime.getRuntime().exec("iwconfig eth0");
		    // NOTICE: you have to create a new buffered reader; otherwise, 
		    //   reader can't get new stream
		    // ToDo: some unidentified symbol
		    reader = new BufferedReader(
		            new InputStreamReader(process.getInputStream()));
		    output = new StringBuffer();
		    while ((read = reader.read(buffer)) > 0) {
		        output.append(buffer, 0, read);
		    }
		    textInfo.append(output.toString());
		    
		    reader.close();
		    
		    // Waits for the command to finish.
		    process.waitFor();		    
		    
		    
		} catch (IOException e) {
		    throw new RuntimeException(e);
		} catch (InterruptedException e) {
		    throw new RuntimeException(e);
		}
	}
	
	public void onClick(View view) {	

		switch (view.getId()) {

		// Create its own ad hoc network
		// ToDo: check if there is same ID in the network
		case R.id.button_create:		
			connect(spinner_ssid.getItemAtPosition(spinner_ssid.getSelectedItemPosition()).toString());
			break;
		case R.id.button_check:		
			check();
			break;
			
		}
	}    
}

