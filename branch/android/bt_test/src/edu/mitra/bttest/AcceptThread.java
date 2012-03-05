package edu.mitra.bttest;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class AcceptThread extends Thread {
	private final BluetoothServerSocket sSocket;
	private BluetoothAdapter mAdapter = null;
	private BluetoothSocket sock = null;
	
	public AcceptThread() {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		
		BluetoothServerSocket tmp = null;
		try {
			tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord("btspp", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
		} catch (IOException e) {}
		sSocket = tmp;
		Log.d("BLUETOOTH", "Server is set up!");
	}
	
	@Override
	public synchronized void start() {
		super.start();
	}
	
	@Override
	public void run() {
		while(true) {
			Log.d("BLUETOOTH", "Running!");
			try {
				sock = sSocket.accept();
				Log.d("BLUETOOTH", "Accepted connection");
			} catch(IOException e) {Log.e("BLUETOOTH", "IOException");}
			
			if(sock != null) {
				try {
					sSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Log.d("BLUETOOTH", "Sock != null?");
				break;
			}
		}
	}
	
	public boolean hasSocket() {
		return (sock != null);
	}
	
	public BluetoothSocket getSocket() {
		return sock;
	}
}
