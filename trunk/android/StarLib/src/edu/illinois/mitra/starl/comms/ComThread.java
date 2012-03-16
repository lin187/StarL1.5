package edu.illinois.mitra.starl.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import android.util.Log;
import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.globalVarHolder;

/**
 * This thread handles all incoming and outgoing transmissions.
 */    
class ComThread extends Thread implements Cancellable {
	private static final int BCAST_PORT = 2562;
	private static String TAG = "ComThread";
	private static String ERR = "Critical Error";
	
	private ArrayList<UDPMessage> ReceivedMsgList;
	
	private DatagramSocket mSocket = null;
	private InetAddress myLocalIP = null;
	private boolean running = true;
	private globalVarHolder gvh;
	
	public ComThread(ArrayList<UDPMessage> ReceivedMsgList, globalVarHolder gvh) {
		this.gvh = gvh;
		this.ReceivedMsgList = ReceivedMsgList;
		Boolean err = true;
		int retries = 0;

		while(err && retries < 15) {
			try {
				myLocalIP = common.getLocalAddress();
				if(mSocket == null) {
					mSocket = new DatagramSocket(BCAST_PORT);
					mSocket.setBroadcast(true);
					err = false;
				}
			} catch (IOException e) {
				Log.e(ERR, "Could not make socket", e);
				err = true;
				retries ++;
			}
		}
		gvh.traceEvent(TAG, "Created");
		running = true;
	}
    
    public void run() {
		try {
			byte[] buf = new byte[1024]; 
			
			//Listen on socket to receive messages 
			while(running) { 
    			DatagramPacket packet = new DatagramPacket(buf, buf.length); 
    			mSocket.receive(packet); 

    			InetAddress remoteIP = packet.getAddress();
    			if(remoteIP.equals(myLocalIP))
    				continue;

    			String s = new String(packet.getData(), 0, packet.getLength());  
    			UDPMessage recd = new UDPMessage(s, System.currentTimeMillis());

    			Log.d(TAG, "Received: " + s);
    			gvh.traceEvent(TAG, "Received", recd);
    			
    			// Add the message to the received queue
    			ReceivedMsgList.add(recd);
    		} 
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	/**
      * Write broadcast packet.
      */
    public void write(UDPMessage msg, String IP) {
        try {
        	String data = msg.toString();
            DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), InetAddress.getByName(IP), BCAST_PORT);
            mSocket.send(packet); 
            Log.i(TAG, "Sent: " + data + " to " + IP);
            gvh.traceEvent(TAG, "Sent", msg);
        } catch (Exception e) {
            Log.e(ERR, "Exception during write", e);
        }
    }
    
    @Override
    public void cancel() {
    	running = false;
        try {
            mSocket.close();
            mSocket = null;
        } catch (Exception e) {
            Log.e(ERR, "close of connect socket failed", e);
        }
        gvh.traceEvent(TAG, "Cancelled");
    }
} 