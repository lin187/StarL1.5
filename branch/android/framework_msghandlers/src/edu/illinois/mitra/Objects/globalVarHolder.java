package edu.illinois.mitra.Objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import android.os.Handler;
import android.util.Log;
import edu.illinois.mitra.RobotsActivity;
import edu.illinois.mitra.Objects.exceptions.MessageIDInUseException;
import edu.illinois.mitra.comms.CommsHandler;
import edu.illinois.mitra.comms.MessageResult;
import edu.illinois.mitra.comms.RobotMessage;
import edu.illinois.mitra.interfaces.MessageListener;


public class globalVarHolder {
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
	

	//Constructor
	public globalVarHolder(HashMap<String, String> participants, Handler handler) {
		this.participants = participants;
		this.handler = handler;
		this.listeners = new HashMap<Integer,MessageListener>();
	}
	
	public synchronized Set<String> getParticipants() {
		return participants.keySet();
	}

	public synchronized void setDebugInfo(String debugInfo) {
		sendMainMsg(RobotsActivity.MESSAGE_DEBUG, debugInfo);
	}
	
	public synchronized void sendMainToast(String debugInfo) {
		sendMainMsg(RobotsActivity.MESSAGE_TOAST, debugInfo);
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
	}

	// Message event code
	public synchronized void addMsgListener(int mid, MessageListener l) {
		if(listeners.containsKey(mid)) {
			// TODO: ALL KINDS OF PROBLEMS UP IN HERE
			//throw new MessageIDInUseException(mid);
			Log.e("Critical Error", "Already have a listener for MID " + mid);
			return;
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
			//Log.e("Critical Error", "No handler for MID " + m.getMID());
			// TODO: Do we care about this?
		}
	}
	
	
	public void startComms() {
		this.comms = new CommsHandler(participants, name, this);
		comms.start();
		comms.clear();
	}
	
	public void stopComms() {
		listeners.clear();
		comms.cancel();
		comms = null;
	}
}
