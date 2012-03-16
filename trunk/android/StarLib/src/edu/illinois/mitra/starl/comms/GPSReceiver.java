package edu.illinois.mitra.starl.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.util.Log;
import edu.illinois.mitra.starl.exceptions.ItemFormattingException;
import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.globalVarHolder;
import edu.illinois.mitra.starl.objects.itemPosition;
import edu.illinois.mitra.starl.objects.positionList;

public class GPSReceiver extends Thread implements Cancellable {
	private static final String TAG = "GPSReceiver";
	private static final String ERR = "Critical Error";
	
	private positionList robotPositions;
	private positionList waypointPositions;

	private globalVarHolder gvh;

	private DatagramSocket mSocket;
	private InetAddress myLocalIP;
	private boolean running = true;
	
	private boolean received = false;

	public GPSReceiver(globalVarHolder gvh,String hostname, int port) {
		super();
		this.gvh = gvh;
		
		robotPositions = new positionList();
		waypointPositions = new positionList();
		
		try {
			myLocalIP = getLocalAddress();
			mSocket = new DatagramSocket(port);
		} catch (IOException e) {}
		
		Log.i("GPSReceiver", "Listening to GPS host on port " + port);
		gvh.traceEvent(TAG, "Created");
	}

	@Override
	public void destroy() {
		mSocket.close();
		super.destroy();
	}
	
	@Override
	public synchronized void start() {
		running = true;
		super.start();
	}

	public void run() {
		gvh.setWaypointPositions(waypointPositions);
		gvh.setPositions(robotPositions);   		
    
    	try {
    		byte[] buf = new byte[2048]; 
    		
    		while(running) {
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
    				gvh.sendMainMsg(common.MESSAGE_LOCATION, common.GPS_RECEIVING);
    				received = true;
    			}    			
    			for(int i = 0; i < parts.length; i++) {
    				if(parts[i].length() >= 2) {
		    			switch(parts[i].charAt(0)) {
		    			case '@':
		    				try {
		    					itemPosition newpos = new itemPosition(parts[i]);
		    					waypointPositions.update(newpos);
		    					gvh.traceEvent(TAG, "Received Waypoint Position", newpos);
		    				} catch(ItemFormattingException e){
		    					Log.e(TAG, "Invalid item formatting: " + e.getError());
		    				}
		    				break;
		    			case '#':
		    				try {
		    					itemPosition newpos = new itemPosition(parts[i]);
		    					robotPositions.update(newpos);
		    					gvh.traceEvent(TAG, "Received Robot Position", newpos);
		    				} catch(ItemFormattingException e){
		    					Log.e(TAG, "Invalid item formatting: " + e.getError());
		    				}
		    				break;
		    			case 'G':
		    				gvh.traceEvent(TAG, "Received launch command");
		    				gvh.sendMainMsg(common.MESSAGE_LAUNCH, parts[i].substring(3));
		    				break;
		    			case 'A':
		    				gvh.traceEvent(TAG, "Received abort command");
		    				gvh.sendMainMsg(common.MESSAGE_ABORT, null);
		    				break;
		    			default:
		    				Log.e(ERR, "Unknown GPS message received: " + line);
		    				break;
		    			}
    				}
    			}
	    	}
		} catch (IOException e) {
			gvh.sendMainMsg(common.MESSAGE_LOCATION, common.GPS_OFFLINE);
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
    
    @Override
    public void cancel() {
    	running = false;
    	gvh.sendMainMsg(common.MESSAGE_LOCATION, common.GPS_OFFLINE);
        try {
        	// Clear the list of waypoints
        	//robotPositions = new positionList();
        	//waypointPositions = new positionList();
        	//gvh.setWaypointPositions(waypointPositions);
        	//gvh.setPositions(robotPositions);
            mSocket.close();
        } catch (Exception e) {
            Log.e(ERR, "close of connect socket failed", e);
        }
		gvh.traceEvent(TAG, "Cancelled");
    }
}