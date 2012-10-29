package edu.illinois.mitra.starl.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.SmartComThread;
import edu.illinois.mitra.starl.objects.Common;

/**
 * Hardware specific implementation of SmartComThread. Requires Android Internet privileges
 * @author Adam Zimmerman
 * @version 1.0
 */
public class SmartUdpComThread extends Thread implements SmartComThread {
	private static final int BCAST_PORT = 2562;
	private static String TAG = "ComThread";
	private static String ERR = "Critical Error";
	
	private SmartCommsHandler commsHandler;
	
	private DatagramSocket mSocket = null;
	private InetAddress myLocalIP = null;
	private boolean running = true;
	private GlobalVarHolder gvh;
	
	public SmartUdpComThread(GlobalVarHolder gvh) {
		this.gvh = gvh;
		Boolean err = true;
		int retries = 0;

		while(err && retries < 15) {
			try {
				myLocalIP = Common.getLocalAddress();
				if(mSocket == null) {
					mSocket = new DatagramSocket(BCAST_PORT);
					mSocket.setBroadcast(true);
					err = false;
				}
			} catch (IOException e) {
				gvh.log.e(ERR, "Could not make socket" + e);
				err = true;
				retries ++;
			}
		}
		gvh.trace.traceEvent(TAG, "Created", gvh.time());
		running = true;
		this.start();
	}
    
    @Override
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
    			UdpMessage recd = new UdpMessage(s, gvh.time());

    			gvh.log.d(TAG, "Received: " + s);
    			gvh.trace.traceEvent(TAG, "Received", recd, gvh.time());
    			
    			// Pass the received message to the comms handler
    			if(commsHandler != null) {
    				commsHandler.handleReceived(recd);
    			} else {
    				gvh.log.e(TAG, "Lost a message because comms handler thread hasn't been specified!");
    			}
    		} 
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    @Override
	public synchronized void write(UdpMessage msg, String IP) {
    	if(mSocket != null) {
	        try {
	        	String data = msg.toString();
	            DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), InetAddress.getByName(IP), BCAST_PORT);
	            mSocket.send(packet); 
	            gvh.log.i(TAG, "Sent: " + data + " to " + IP);
	            gvh.trace.traceEvent(TAG, "Sent", msg, gvh.time());
	        } catch (Exception e) {
	            gvh.log.e(ERR, "Exception during write" + e);
	        }
    	}
    }
    
    @Override
    public void cancel() {
    	running = false;
        try {
            mSocket.close();
            mSocket = null;
        } catch (Exception e) {
            gvh.log.e(ERR, "close of connect socket failed" + e);
        }
        gvh.log.i(TAG, "Cancelled UDP com thread");
        gvh.trace.traceEvent(TAG, "Cancelled", gvh.time());
    }

	@Override
	public void setCommsHandler(SmartCommsHandler sch) {
		commsHandler = sch;
	}

}
