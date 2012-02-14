package edu.illinois.mitra.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import edu.illinois.mitra.RobotsActivity;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.Objects.positionList;

import android.util.Log;

public class GPSReceiver extends Thread {
	private static final String TAG = "GPSReceiver";
	private static final String ERR = "Critical Error";
	
	private positionList robotPositions = new positionList();
	private positionList waypointPositions = new positionList();

	private globalVarHolder gvh;

	private DatagramSocket mSocket;
	private InetAddress myLocalIP;
	
	private boolean received = false;

	public GPSReceiver(globalVarHolder gvh,String hostname, int port) {
		super();
		this.gvh = gvh;
		
		try {
			myLocalIP = getLocalAddress();
			mSocket = new DatagramSocket(port);
		} catch (IOException e) {}
		
		Log.i("GPSReceiver", "Listening to GPS host on port " + port);
	}

	@Override
	public void destroy() {
		mSocket.close();
		super.destroy();
	}
	
	@Override
	public synchronized void start() {
		super.start();
	}

	public void run() {
		gvh.setWaypointPositions(waypointPositions);
		gvh.setPositions(robotPositions);   		
    
    	try {
    		byte[] buf = new byte[2048]; 
    		
    		while(true) {
		    	// Receive a message
    			DatagramPacket packet = new DatagramPacket(buf, buf.length); 
				mSocket.receive(packet);
    			InetAddress remoteIP = packet.getAddress();
    			if(remoteIP.equals(myLocalIP))
    				continue;

    			String line = new String(packet.getData(), 0, packet.getLength());
    		
    			// Parse the received string
    			String [] parts = line.split("\n");
    			if(received == false) {
    				gvh.sendMainMsg(RobotsActivity.MESSAGE_LOCATION, 1);
    				received = true;
    			}    			
    			for(int i = 0; i < parts.length; i++) {
	    			switch(parts[i].charAt(0)) {
	    			case '@':
	    				waypointPositions.update(parts[i]);
	    				break;
	    			case '#':
	    				robotPositions.update(parts[i]);
	    				break;
	    			case 'G':
	    				gvh.sendMainMsg(3, parts[i]);
	    				break;
	    			case 'A':
	    				gvh.sendMainMsg(3, "ABORT");
	    				break;
	    			default:
	    				Log.e(ERR, "Unknown GPS message received: " + line);
	    				break;
	    			}
    			}
    			//gvh.setDebugInfo(robotPositions.toString() + "\nWaypoints: " + waypointPositions.getNumPositions());
	    	}
		} catch (IOException e) {
			Log.e(ERR, "Error receiving in GPSReceiver");
			gvh.sendMainMsg(1, 0);
		} 
	}
	
    private InetAddress getLocalAddress() throws IOException {
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
        try {
            mSocket.close();
        } catch (Exception e) {
            Log.e(ERR, "close of connect socket failed", e);
        }
    }
}