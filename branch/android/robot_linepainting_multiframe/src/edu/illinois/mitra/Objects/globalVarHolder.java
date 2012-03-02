package edu.illinois.mitra.Objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import android.os.Handler;
import edu.illinois.mitra.RobotsActivity;
import edu.illinois.mitra.comms.CommsHandler;
import edu.illinois.mitra.comms.MessageResult;
import edu.illinois.mitra.comms.RobotMessage;


public class globalVarHolder {
	// Comms
	private Handler handler;
	private CommsHandler comms;
	private LinkedList<RobotMessage> incomingMessages = new LinkedList<RobotMessage>();
	private int[] incomingMIDs = new int[100];
	
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

	//Incoming messages
	//Return the next incoming message if it has a matching MID
	private synchronized RobotMessage getIncomingMessage(int mid_match) {
		if(incomingMIDs[mid_match] > 0) {
			for(int i=0; i<incomingMessages.size(); i++) {
				RobotMessage current = incomingMessages.get(i);
				if(current.getMID() == mid_match) {
					incomingMIDs[mid_match] --;
					incomingMessages.remove(i);
					return new RobotMessage(current);
				}
			}
		} else {
			// Throw an exception for checking a reserved MID?
		}
		return null;
	}
	// Get all matching messages
	public synchronized Collection<RobotMessage> getIncomingMessages(int mid_match) {
		HashSet<RobotMessage> retval = new HashSet<RobotMessage>();
		while(getIncomingMessageCount(mid_match) > 0) {
			retval.add(getIncomingMessage(mid_match));
		}
		return retval;
	}
	// Get the number of matching messages
	public synchronized int getIncomingMessageCount(int mid_match) {
		return incomingMIDs[mid_match];
	}
	// Add an incoming message
	public synchronized void addIncomingMessage(RobotMessage msg) {
		incomingMessages.add(msg);
		incomingMIDs[msg.getMID()] ++;
	}

	public void startComms() {
		for(int i = 0; i < 100; i ++) {
			incomingMIDs[i] = 0;
		}
		incomingMessages.clear();
		this.comms = new CommsHandler(participants, name, this);
		comms.start();
		comms.clear();
	}
	
	public void stopComms() {
		comms.cancel();
		comms = null;
	}
}
