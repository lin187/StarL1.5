package edu.mitra.bttest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;

public class Bt_testActivity extends Activity {
	
	public OutputStream out;
	public BluetoothSocket socket;
	public Handler handler;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        con();
        
        handler = new Handler();
        handler.post(new sendHello());
    }
    
    class sendHello implements Runnable {
		public void run() {
			try {
				out.write(Byte.parseByte("HELLO.\n"));
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			handler.postDelayed(this, 500);
		}
    }
    
    public void con() {
    	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    	if (adapter == null) {
    	    // Device does not support Bluetooth
    	    finish(); //exit
    	}

    	final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //UUID for serial connection
    	String mac = "00:1B:DC:0F:E5:98"; //desktop's MAC
    	BluetoothDevice device = adapter.getRemoteDevice(mac); //get remote device by mac, we assume these two devices are already paired


    	 // Get a BluetoothSocket to connect with the given BluetoothDevice
    	out = null;
    	try {
    	    socket = device.createRfcommSocketToServiceRecord(SERIAL_UUID); 
    	} catch (IOException e) {}

    	try {           
    	    socket.connect(); 
    	    out = socket.getOutputStream();
    	    //now you can use out to send output via out.write
    	} catch (IOException e) {}
    }
}