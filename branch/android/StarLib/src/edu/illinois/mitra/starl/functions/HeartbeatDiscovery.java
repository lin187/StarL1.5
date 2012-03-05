package edu.illinois.mitra.starl.functions;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.util.Log;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.NetworkDiscovery;
import edu.illinois.mitra.starl.interfaces.NetworkDiscoveryListener;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.globalVarHolder;

public class HeartbeatDiscovery extends Thread implements NetworkDiscovery, MessageListener {
	
	private static final int HEARTBEAT_PERIOD = 5000;
	private HashSet<String> remove;
	private HashMap<String,Long> participants;	
	private HashSet<NetworkDiscoveryListener> listeners = new HashSet<NetworkDiscoveryListener>();
	private globalVarHolder gvh = null;	
	
	private String name;
	private String myip;
	
	private RobotMessage discover;

	private boolean running = false;

	public HeartbeatDiscovery(globalVarHolder gvh) {
		this.gvh = gvh;
		participants = new HashMap<String,Long>();
		remove = new HashSet<String>();
		gvh.addMsgListener(common.MSG_NETWORK_DISCOVERY, this);
		
		name = gvh.getName();
		try {
			myip = common.getLocalAddress().getHostAddress();
		} catch (IOException e) {
			e.printStackTrace();
		}
		discover = new RobotMessage("DISCOVER", name, common.MSG_NETWORK_DISCOVERY, myip);
	}
	
	@Override
	public synchronized void start() {
		gvh.addOutgoingMessage(discover);
		running = true;
		
		// Add ourselves as a participant
		for(NetworkDiscoveryListener l : listeners) {
			l.neighborDiscoveredEvent(name, myip);
		}
		super.start();
	}
	
	@Override
	public void run() {
		while(running) {
			sleep(HEARTBEAT_PERIOD);
			gvh.addOutgoingMessage(discover);
			
			// TODO: This code currently throws a ConcurrentModificationException 
//			for(String neighbor : participants.keySet()) {
//				if((System.currentTimeMillis()-participants.get(neighbor)) > (4*HEARTBEAT_PERIOD)) {
//					Log.d(TAG, "I haven't heart from participant " + neighbor + " in too long.");
//					participants.remove(neighbor);
//					for(NetworkDiscoveryListener l : listeners) {
//						l.neighborLostEvent(neighbor);
//					}
//				}
//			}
		}
	}

	@Override
	public synchronized void addListener(NetworkDiscoveryListener l) {
		listeners.add(l);
	}
	
	@Override
	public synchronized void removeListener(NetworkDiscoveryListener l) {
		if(listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	@Override
	public synchronized void messageReceied(RobotMessage m) {
		String from = m.getFrom();

		if(!participants.containsKey(from)) {
			Log.i(TAG, "New neighbor: " + from + ": " + m.getContents());
			participants.put(m.getFrom(),System.currentTimeMillis());
			for(NetworkDiscoveryListener l : listeners) {
				l.neighborDiscoveredEvent(m.getFrom(), m.getContents());
			}
		}
	}

	@Override
	public void cancel() {
		running = false;
		gvh.removeMsgListener(common.MSG_NETWORK_DISCOVERY);
	}
	
	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}	
	}	
}
