package edu.illinois.mitra.starl.bluetooth;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.globalVarHolder;

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
	private BufferedInputStream bufInStream;
	
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
		if(mOutStream != null) {
			try {
				mOutStream.write(to_send);
			} catch (IOException e) {
				Log.e(ERR, "Bluetooth failed to send!");
			} catch (NullPointerException e) {
				Log.e(ERR, "Bluetooth write failed: mOutStream throws null pointer exception");
			}
		}
	}
	public int read() {
		int retval = -1;
		try {
			retval = bufInStream.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;
	}
	
	
	public byte[] readBuffer(int n_bytes) {
		byte[] buffer = new byte[n_bytes];
		for(int i = 0; i < n_bytes; i++) {
			try {
				buffer[i] = (byte) bufInStream.read();
			} catch (IOException e) {
				e.printStackTrace();
				return new byte[]{0};
			} catch (NullPointerException e) {
				e.printStackTrace();
				return new byte[]{0};
			}
		}

		return buffer;
//		for(int i = 0; i < n_bytes; i++) {
//			buffer[i] = (byte) 55;
//		}
//		
//		int bytesRead = -1;
//		try {
//			bytesRead = bufInStream.read(buffer, 0, n_bytes);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		if(bytesRead == -1) {
//			return new byte[]{-1};
//		}
//		Log.d(TAG, "Read from buffer: " + Arrays.toString(buffer));
//		return buffer;
	}
	
	public void disconnect() {
		Log.e(TAG, "Disconnecting from bluetooth!");
		if(mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				Log.e(ERR, "Bluetooth failed to disconnect!");
			}
		}
		mOutStream = null;
		gvh.sendMainMsg(common.MESSAGE_BLUETOOTH,0);
	}

	private class BluetoothConnectTask 	extends AsyncTask<BluetoothDevice, Void, Integer> {
		@Override
		protected Integer doInBackground(BluetoothDevice... params) {
			gvh.sendMainMsg(common.MESSAGE_BLUETOOTH,2);
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
					bufInStream = new BufferedInputStream(mSocket.getInputStream());
				} catch (IOException e) {
					e.printStackTrace();
					mOutStream = null;
					bufInStream = null;
					mSocket = null;
					Log.e(TAG, "Failed to connect!");
				}
			}
			
			if(mSocket == null) {
				Log.e(ERR, "Bluetooth failed to connect!");
			} else {
				send(ENABLE_CONTROL);
				send(PROGRAM_SONG);
				Log.i(TAG, "Connected to robot via Bluetooth!");
				Log.d(TAG, "Clearing input buffer...");
				// Clear the input buffer
				send(new byte[]{(byte) 142, (byte) 21});
				byte[] buffer = new byte[1024];
				int read = 0;
				try {
					read = bufInStream.read(buffer, 0, 1024);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d(TAG, "Read " + read + " byte(s) while clearing the inbuf");
				
				// Inform the GUI that bluetooth has been connected
				gvh.sendMainMsg(common.MESSAGE_BLUETOOTH,1);
			}
			return 0;
		}
	}
}