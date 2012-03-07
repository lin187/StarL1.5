package edu.illinois.mitra.starl.objects;

import java.util.HashMap;
import java.util.Set;

import android.os.Handler;
import android.util.Log;
import edu.illinois.mitra.starl.comms.CommsHandler;
import edu.illinois.mitra.starl.comms.MessageResult;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.HeartbeatDiscovery;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.NetworkDiscovery;
import edu.illinois.mitra.starl.interfaces.NetworkDiscoveryListener;

public class globalVarHolder implements NetworkDiscoveryListener {
	// Comms
	private Handler handler;
	private CommsHandler comms;
	private HashMap<Integer,MessageListener> listeners;
	
	// GPS
	private positionList robot_positions;
	private positionList waypoint_positions;

	// Identification
	private HashMap<String, String> participants = null;
	private String name = null;
	
	// Discovery
	private NetworkDiscovery discover = null;
	private boolean useDiscovery = false;
	
	// Log file
	private TraceWriter trace = null;

	//Constructor
	// For hard-coded participants
	public globalVarHolder(HashMap<String, String> participants, Handler handler) {
		this.participants = participants;
		this.handler = handler;
		this.listeners = new HashMap<Integer,MessageListener>();
		useDiscovery = false;
	}
	
	// For participant discovery
	public globalVarHolder(Handler handler) {
		this.handler = handler;
		this.listeners = new HashMap<Integer,MessageListener>();
		participants = new HashMap<String, String>();
		useDiscovery = true;
	}
	
	public synchronized Set<String> getParticipants() {
		return participants.keySet();
	}

	public synchronized void setDebugInfo(String debugInfo) {
		sendMainMsg(common.MESSAGE_DEBUG, debugInfo);
	}
	
	public synchronized void sendMainToast(String debugInfo) {
		sendMainMsg(common.MESSAGE_TOAST, debugInfo);
	}
	
	public synchronized void sendMainMsg(int type, Object data) {
		handler.obtainMessage(type, -1, -1, data).sendToTarget();
	}
	
	//Robot positions
	public synchronized void setPositions(positionList new_position) {
		//SET POSITIONS MAY ONLY BE CALLED BY THE GPSRECEIVER!
		robot_positions = new_position;
	}
	public synchronized positionList getPositions() {
		return robot_positions;
	}
	public synchronized itemPosition getPosition(String robot_name) {
		return robot_positions.getPosition(robot_name);
	}
	public synchronized itemPosition getMyPosition() {
		return robot_positions.getPosition(name);
	}
	
	//Waypoint positions
	public synchronized void setWaypointPositions(positionList new_positions) {
		//SET POSITIONS MAY ONLY BE CALLED BY THE GPSRECEIVER!
		waypoint_positions = new_positions;
	}
	public synchronized positionList getWaypointPositions() {
		return waypoint_positions;
	}
	public synchronized itemPosition getWaypointPosition(String waypoint_name) {
		return waypoint_positions.getPosition(waypoint_name);
	}
		
	//Name
	public synchronized void setName(String new_name) {
		name = new String(new_name);
	}
	public synchronized String getName() {
		return name;
	}

	//Outgoing messages
	public synchronized MessageResult addOutgoingMessage(RobotMessage msg) {
		if(comms != null) {
			// If the message is being sent to myself, add it to the in queue
			if(msg.getTo().equals(name)) {
				addIncomingMessage(msg);
				return new MessageResult(0);
			}
			
			// Create a new message result object
			int receivers = msg.getTo().equals("ALL") ? participants.size()-1 : 1;
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
	public synchronized void addMsgListener(int mid, MessageListener l) {
		if(listeners.containsKey(mid)) {
			throw new RuntimeException("Already have a listener for MID " + mid);
		}
		listeners.put(mid, l);
	}
	public synchronized void removeMsgListener(int mid) {
		listeners.remove(mid);
	}
	
	public synchronized void addIncomingMessage(RobotMessage m) {
		try {
			listeners.get(m.getMID()).messageReceied(m);
		} catch(NullPointerException e) {
			Log.e("Critical Error", "No handler for MID " + m.getMID());
		}
	}
	
	public void startComms() {
		this.comms = new CommsHandler(participants, name, this);
		comms.start();
		comms.clear();
		
		if(useDiscovery) {
			// Create a new network discovery thread and register this as a listener
			discover = new HeartbeatDiscovery(this);
			discover.addListener(this);
			discover.start();
		}
	}
	
	public void stopComms() {
		if(useDiscovery) {
			discover.cancel();
			discover = null;
		}
		
		listeners.clear();
		comms.cancel();
		comms = null;
	}
	
	@Override
	public void neighborDiscoveredEvent(String name, String IP) {
		participants.put(name, IP);
	}

	@Override
	public void neighborLostEvent(String name) {
		participants.remove(name);
	}
	
	public void event(String thread, String data) {
		// TODO: Implement me!
	}
}
