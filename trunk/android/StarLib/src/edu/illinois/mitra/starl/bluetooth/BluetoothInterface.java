package edu.illinois.mitra.starl.bluetooth;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.objects.Common;

public class BluetoothInterface {

	private static final String ERR = "Critical Error";
	private static final String TAG = "BluetoothInterface";
	private static byte[] ENABLE_CONTROL = { (byte) 128, (byte) 132 };
	private static byte[] PROGRAM_SONG = { (byte) 140, 0x00, 0x02, (byte) 79,
			(byte) 16, (byte) 84, (byte) 16, (byte) 140, 0x01, 0x01, (byte) 96,
			(byte) 10, (byte) 140, 0x02, 0x01, (byte) 84, (byte) 10 };
	// Standard serial port UUID (from Android documentation)
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private BluetoothDevice mDevice;
	private BluetoothSocket mSocket = null;
	private OutputStream mOutStream;
	private BufferedInputStream bufInStream;
	private BluetoothAdapter btAdapter;
	private GlobalVarHolder gvh;
	private String mac_address;
	
	private BluetoothConnectTask task;
	
	private boolean running = true;

	public BluetoothInterface(GlobalVarHolder gvh, String mac) {
		this.gvh = gvh;
		this.mac_address = mac;
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) throw new RuntimeException("NO BLUETOOTH ADAPTER FOUND!");
	
		connect();
	}

	public void connect() {
		gvh.log.i(TAG, "Connecting to " + mac_address);
		task = new BluetoothConnectTask();

		if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();

		// Acquire the remote device
		mDevice = btAdapter.getRemoteDevice(mac_address);

		// Attempt to connect
		task.execute(mDevice);
	}

	public void send(byte[] to_send) {
		if (mOutStream != null) {
			try {
				mOutStream.write(to_send);
			} catch (IOException e) {
				gvh.log.e(ERR, "Bluetooth failed to send!");
			} catch (NullPointerException e) {
				gvh.log.e(ERR, "Bluetooth write failed: mOutStream throws null pointer exception");
			}
		}
	}

	public byte[] readBuffer(int n_bytes) {
		byte[] buffer = new byte[n_bytes];
		try {
			int res = bufInStream.read(buffer);
		} catch (IOException e) {
			gvh.log.i(TAG, "Failed to read anything!");
		} catch (NullPointerException e) {
			gvh.log.i(TAG, "We're not connected yet! Go away!");
			return null;
		}
		
		return buffer;
	}

	public synchronized byte[] sendReceive(byte[] to_send, int expectedBytes) {
		send(to_send);
		return readBuffer(expectedBytes);
	}
	
	public void disconnect() {
		running = false;
		gvh.log.e(TAG, "Disconnecting from bluetooth!");
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				gvh.log.e(ERR, "Bluetooth failed to disconnect!");
			}
		}
		
		try {
			task.cancel(true);
		} catch(Exception e) {
			gvh.log.e(TAG, "Tried to stop BT connect task, failed.");
		}
		mOutStream = null;
		gvh.plat.sendMainMsg(Common.MESSAGE_BLUETOOTH, 0);
	}

	private class BluetoothConnectTask extends AsyncTask<BluetoothDevice, Void, Integer> {
		@Override
		protected Integer doInBackground(BluetoothDevice... params) {
			gvh.plat.sendMainMsg(Common.MESSAGE_BLUETOOTH, Common.BLUETOOTH_CONNECTING);
			
			BluetoothSocket tmp = null;
			try {
				tmp = params[0].createInsecureRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				gvh.log.e(TAG, "Couldn't create socket!");
			}
			mSocket = tmp;
			
			try {
				mSocket.connect();
				mOutStream = mSocket.getOutputStream();
				bufInStream = new BufferedInputStream(mSocket.getInputStream());
			} catch (IOException e) {
				mOutStream = null;
				bufInStream = null;
				gvh.log.e(TAG, "Failed to connect! Retrying...");
				if(running)	BluetoothInterface.this.connect();
				return 1;
			}

			gvh.log.i(TAG, "Connection established.");
			send(ENABLE_CONTROL);
			send(PROGRAM_SONG);
			
			// Inform the GUI that bluetooth has been connected
			gvh.plat.sendMainMsg(Common.MESSAGE_BLUETOOTH, Common.BLUETOOTH_CONNECTED);
			return 0;
		}
	}
}