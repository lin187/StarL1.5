package edu.illinois.mitra.bluetooth;

import java.io.IOException;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.illinois.mitra.Objects.globalVarHolder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.os.AsyncTask;
import android.util.Log;

public class BluetoothInterface {
	
	private static final String ERR = "Critical Error";
	private static final String TAG = "Bluetooth";
	private static byte[] ENABLE_CONTROL = {(byte) 128,(byte) 132};
	//									  SONG 0 															| SONG 1 									  | SONG 2
	private static byte[] PROGRAM_SONG = {(byte) 140, 0x00, 0x02, (byte) 79, (byte) 16, (byte) 84, (byte) 16, (byte) 140, 0x01, 0x01, (byte) 96, (byte) 10,(byte) 140, 0x02, 0x01, (byte) 84, (byte) 10};
	private static int MAX_RETRIES = 10;

	private BluetoothDevice mDevice;
	private BluetoothSocket mSocket = null;
	private OutputStream mOutStream;
	private BluetoothAdapter btAdapter;
	private int retries = 0;
	private globalVarHolder gvh;
	
	public BluetoothInterface(globalVarHolder gvh) {
		this.gvh = gvh;
		btAdapter = BluetoothAdapter.getDefaultAdapter();
	    if (btAdapter == null) {
	    	Log.e(TAG,"NO BLUETOOTH ADAPTER FOUND!");
	    }
	}
	
	public void connect(String mac_address) {
		Log.i(TAG, "Connecting to " + mac_address);
		BluetoothConnectTask task = new BluetoothConnectTask();
		
		// Cancel Bluetooth discovery
		if(btAdapter.isDiscovering()) {
			btAdapter.cancelDiscovery();
		}
		
		// Acquire the remote device
		mDevice = btAdapter.getRemoteDevice(mac_address);
		
		// Attempt to connect until the maximum number of retries is reached
		task.execute(mDevice);			
	}
	
	public void send(byte[] to_send) {
		try {
			mOutStream.write(to_send);
		} catch (IOException e) {
			Log.e(ERR, "Bluetooth failed to send!");
		} catch (NullPointerException e) {
			Log.e(ERR, "Bluetooth write failed: mOutStream throws null pointer exception");
		}
	}
	public void disconnect() {
		if(mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				Log.e(ERR, "Bluetooth failed to disconnect!");
			}
		}
		gvh.sendMainMsg(2,0);
	}

	private class BluetoothConnectTask 	extends AsyncTask<BluetoothDevice, Void, Integer> {
		@Override
		protected Integer doInBackground(BluetoothDevice... params) {
			gvh.sendMainMsg(2,2);
			while(mSocket == null && retries < MAX_RETRIES) {
				retries ++;
				try {
					Method m;
					try {
						m = mDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
						mSocket = (BluetoothSocket) m.invoke(mDevice, 1);
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
			        
					mSocket.connect();
					mOutStream = mSocket.getOutputStream();
				} catch (IOException e) {
					e.printStackTrace();
					mOutStream = null;
					mSocket = null;
					Log.e(TAG, "Failed to connect!");
				}
			}
			
			if(mSocket == null) {
				Log.e(ERR, "Bluetooth failed to connect!");
			} else {
				gvh.sendMainMsg(2,1);
				send(ENABLE_CONTROL);
				send(PROGRAM_SONG);
				Log.i(TAG, "Connected to robot via Bluetooth!");
			}
			return 0;
		}
	}
}