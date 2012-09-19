package edu.illinois.mitra.starl.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.ComThread;
import edu.illinois.mitra.starl.objects.Common;

/**
 * Hardware specific. This thread handles all incoming and outgoing UDP transmissions. Works in conjunction with CommsHandler.java
 * @deprecated Has been replaced by the new SmartUdpComThread class working in conjunction with SmartCommsHandler
 * @see SmartUdpComThread
 * @author Adam Zimmerman
 * @version 1.2
 */  
@Deprecated
public class UdpComThread extends Thread implements ComThread {
	private static final int BCAST_PORT = 2562;
	private static String TAG = "ComThread";
	private static String ERR = "Critical Error";
	
	private ArrayList<UDPMessage> ReceivedMsgList;
	
	private DatagramSocket mSocket = null;
	private InetAddress myLocalIP = null;
	private boolean running = true;
	private GlobalVarHolder gvh;
	
	public UdpComThread(GlobalVarHolder gvh) {
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
    			UDPMessage recd = new UDPMessage(s, gvh.time());

    			gvh.log.d(TAG, "Received: " + s);
    			gvh.trace.traceEvent(TAG, "Received", recd, gvh.time());
    			
    			// Add the message to the received queue
    			ReceivedMsgList.add(recd);
    		} 
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    @Override
	public synchronized void write(UDPMessage msg, String IP) {
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
	public void setMsgList(ArrayList<UDPMessage> ReceivedMessageList) {
		this.ReceivedMsgList = ReceivedMessageList;
	}
} 