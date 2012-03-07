package edu.mitra.bttest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class Bt_testActivity extends Activity {
	public Handler handler;
	boolean running = true;
	boolean connected = false;

	private int sends = 0;
	private BluetoothSocket mSocket = null;
	private OutputStream mOutStream = null;	
	private TextView debug = null;
	private BluetoothAdapter mAdapter = null;
	private BluetoothDevice device = null;
	final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //UUID for serial connection
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        connected = false;
        
        debug = (TextView) findViewById(R.id.txtDebug);
        debug.setText("Connecting...");
        
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mAdapter.isDiscovering()) {
        	mAdapter.cancelDiscovery();
        }
        String mac = "CC:52:AF:E4:28:FD";
        device = mAdapter.getRemoteDevice(mac); //get remote device by mac, we assume these two devices are already paired

        //mSocket
        int retries = 0;
        while(!connected && retries < 10) {
	        try {
	        	mSocket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
	        	//Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
	            //mSocket = (BluetoothSocket) m.invoke(device, 1);
	        	mSocket.connect();
				mOutStream = mSocket.getOutputStream();
				connected = true;
			} catch (IOException e) {
				e.printStackTrace();
				debug.setText("Connect error!");
				connected = false;
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			catch (NoSuchMethodException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
	        retries ++;
	        debug.setText("Connecting... " + retries);
        }
        
        handler = new Handler();
        handler.post(new sendHello());
    }

    
    
	@Override
	protected void onStop() {
		running = false;
		super.onStop();
	}

	@Override
	protected void onResume() {
		running = true;
		super.onResume();
	}

	class sendHello implements Runnable {
		public void run() {
			if(connected) {
				try {
					mOutStream.write("HELLOTE.\n".getBytes());
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NullPointerException e) {
					Log.e("BT_TEST", "Null pointer exception!");
				}
			}
			sends ++;
			debug.setText("Connected: " + connected + " Sends: " + sends);
			if(running) handler.postDelayed(this, 500);
		}
    }
}