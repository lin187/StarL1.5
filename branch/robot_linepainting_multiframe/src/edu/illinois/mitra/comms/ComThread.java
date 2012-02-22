package edu.illinois.mitra.comms;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import android.util.Log;
import edu.illinois.mitra.Objects.LogFile;

/**
 * This thread handles all incoming and outgoing transmissions.
 */    
class ComThread extends Thread {
	private static final int BCAST_PORT = 2562;
	private static String TAG = "ComThread";
	private static String ERR = "Critical Error";
	
	private ArrayList<UDPMessage> ReceivedMsgList;
	
	private DatagramSocket mSocket = null;
	private InetAddress myLocalIP = null;
	
	//TODO: ERASE THIS
	//private LogFile mlog;
	
	//TODO: Remove String name from this constructor
	public ComThread(ArrayList<UDPMessage> ReceivedMsgList, String name) {
		this.ReceivedMsgList = ReceivedMsgList;
		Boolean err = true;
		int retries = 0;

		while(err && retries < 15) {
			try {
				myLocalIP = getLocalAddress();
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
		
        //TODO: ERASE THIS LOGFILE ONCE THE COMMS SYSTEM HAS BEEN FIXED UP
        //TODO: ADD AN INTENT TO AUTO SHARE THE FILE VIA DROPBOX? THIS WONT WORK W/OUT INTERNET THOUGH
        //mlog = new LogFile(name + "_msg.csv");
	}
    
    public void run() {
		try {
			byte[] buf = new byte[1024]; 
			
			//Listen on socket to receive messages 
			while (true) { 
    			DatagramPacket packet = new DatagramPacket(buf, buf.length); 
    			mSocket.receive(packet); 

    			InetAddress remoteIP = packet.getAddress();
    			if(remoteIP.equals(myLocalIP))
    				continue;

    			String s = new String(packet.getData(), 0, packet.getLength());  
    			UDPMessage recd = new UDPMessage(s, System.currentTimeMillis());

    			Log.d(TAG, "Received: " + s);
    			//TODO: ERASE THIS ONCE LOGGING COMPLETED
    			//mlog.write(recd,false);
    			
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
            //TODO: ERASE THIS ONCE LOGGING COMPLETED
            //mlog.write(msg,true);
        } catch (Exception e) {
            Log.e(ERR, "Exception during write", e);
        }
    }
    
    private InetAddress getLocalAddress()throws IOException {
		try {
		    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
		        NetworkInterface intf = en.nextElement();
		        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
		            InetAddress inetAddress = enumIpAddr.nextElement();
		            if (!inetAddress.isLoopbackAddress()) {
		            	return inetAddress;
		            }
		        }
		    }
		} catch (SocketException ex) {
		    Log.e(TAG, ex.toString());
		}
		return null;
    }
    
    public void cancel() {
    	//TODO: ERASE THIS ONCE LOGGING COMPLETED
    	//mlog.close();
        try {
            mSocket.close();
            mSocket = null;
        } catch (Exception e) {
            Log.e(ERR, "close of connect socket failed", e);
        }
    }
} 