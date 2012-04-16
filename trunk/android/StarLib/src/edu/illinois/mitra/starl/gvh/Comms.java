package edu.illinois.mitra.starl.gvh;

import java.util.HashMap;

import edu.illinois.mitra.starl.comms.CommsHandler;
import edu.illinois.mitra.starl.comms.MessageResult;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.interfaces.ComThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;

/**
 * Handles all inter-agent communication threads. The Comms class is only instantiated by a GlobalVarHolder.
 * 
 * @author Adam Zimmerman
 * @version 1.0
 * @see GlobalVarHolder
 */
public class Comms {
	private GlobalVarHolder gvh;
	private CommsHandler comms;
	private ComThread mConnectedThread;
	private HashMap<Integer,MessageListener> listeners = new HashMap<Integer, MessageListener>();
	private String name;
	
	public Comms(GlobalVarHolder gvh, ComThread mConnectedThread) {
		this.gvh = gvh;
		this.name = gvh.id.getName();
		this.mConnectedThread = mConnectedThread;
	}
	
	public void startComms() {	
		this.comms = new CommsHandler(gvh, mConnectedThread);
		comms.start();
		comms.clear();
	}
	
	public MessageResult addOutgoingMessage(RobotMessage msg) {
		if(comms != null) {
			// If the message is being sent to myself, add it to the in queue
			if(msg.getTo().equals(name)) {
				addIncomingMessage(msg);
				return new MessageResult(0);
			}
			
			// Create a new message result object
			int receivers = msg.getTo().equals("ALL") ? gvh.id.getParticipants().size()-1 : 1;
			MessageResult result = new MessageResult(receivers);
			
			// Add the message to the queue, link it to the message result object
			comms.addOutgoing(msg, result);
			
			// Return the message result object
			return result;
		} else {
			return null;
		}
	}

	// Message event code
	public void addMsgListener(int mid, MessageListener l) {
		synchronized(listeners) {
			if(listeners.containsKey(mid)) {
				throw new RuntimeException("Already have a listener for MID " + mid + ", " + listeners.get(mid).getClass().getSimpleName());
			}
			listeners.put(mid, l);
		}
	}
	
	public void removeMsgListener(int mid) {
		synchronized(listeners) {
			listeners.remove(mid);
		}
	}
	
	public void addIncomingMessage(RobotMessage m) {
		synchronized(listeners) {
			if(listeners.containsKey(m.getMID())) {
				listeners.get(m.getMID()).messageReceied(m);
			} else {
				gvh.log.e("Critical Error", "No handler for MID " + m.getMID());
			}
		}
	}

	
	public void stopComms() {
		synchronized(listeners) {
			listeners.clear();
		}
		comms.cancel();
		comms = null;
	}
	
	public void getCommStatistics() {
		comms.printStatistics();
	}
	
}
