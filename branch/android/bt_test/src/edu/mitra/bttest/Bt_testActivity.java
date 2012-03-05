package edu.mitra.bttest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

	private BluetoothSocket mSocket = null;
	private OutputStream mOutStream = null;	
	private TextView debug = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        connected = false;
        
        debug = (TextView) findViewById(R.id.txtDebug);
        
        debug.setText("Starting thread...");
        
        AcceptThread at = new AcceptThread();
        at.start();
        
        debug.setText("Waiting...");
        while(!at.hasSocket()) {}
        mSocket = at.getSocket();
        try {
			mOutStream = mSocket.getOutputStream();
			connected = true;
		} catch (IOException e) {
			e.printStackTrace();
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
					mOutStream.write("HELLO.\n".getBytes());
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
			debug.setText("Connected: " + connected);
			if(running) handler.postDelayed(this, 500);
		}
    }
}