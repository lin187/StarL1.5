package edu.illinois.mitra.Objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import android.util.Log;

import edu.illinois.mitra.LogicThread;
import edu.illinois.mitra.comms.RobotMessage;

public class LeaderElection {

	private static final String TAG = "LeaderElection";
	private static final String ERR = "Critical Error";
	private globalVarHolder gvh;
	private int nodes = 0;
	private boolean error = false;
	private static int MAX_WAIT_TIME = 4000;
	
	public LeaderElection(globalVarHolder gvh) {
		this.gvh = gvh;
	}
	
	// Elects one of the participants as leader, returns their name
	public String elect() {		
		nodes = gvh.getParticipants().size();

		// Generate a random number
		Random rand = new Random();
		int myNum = rand.nextInt(1000);
		Log.i(TAG, "My number is " + myNum);
		
		// Broadcast
		RobotMessage bcast = new RobotMessage("ALL", gvh.getName(), LogicThread.MSG_LEADERELECT, Integer.toString(myNum));
		gvh.addOutgoingMessage(bcast);
		
		// Wait to receive MID=MSG_LEADERELECT messages
		Log.d(TAG, "Waiting to receive " + (nodes-1) + " MID=LogicThread.MSG_LEADERELECT messages.");
		int msgcount = 0;
		Long startWaitTime = System.currentTimeMillis();
		while(msgcount < (nodes-1)) {
			msgcount = gvh.getIncomingMessageCount(LogicThread.MSG_LEADERELECT);
			//gvh.setDebugInfo("Waiting... have " + msgcount);
			if(System.currentTimeMillis() > startWaitTime+MAX_WAIT_TIME) {
				//gvh.setDebugInfo("Waited too long!");
				error = true;
				break;
			}
		}
		
		// Determine the leader
		int max_val = -1;
		ArrayList<String> receivedFrom = new ArrayList<String>();
		String leader = null;
		if(!error) {
			Iterator<RobotMessage> iter = gvh.getIncomingMessages(LogicThread.MSG_LEADERELECT).iterator();
			for(int i = 0; i < nodes-1; i ++) {
				RobotMessage next = iter.next();
				String from = next.getFrom();
				String contents = next.getContents();
				// Make sure we haven't received multiple values from the same robot
				if(!receivedFrom.contains(from)) {
					receivedFrom.add(from);
				} else {
					Log.e(TAG, "Received from " + from + " twice!");
					error = true;
					break;
				}
				int next_val = Integer.parseInt(contents);
				if(next_val > max_val) {
					max_val = next_val;
					leader = new String(from);
				} else if(next_val == max_val) {
					leader = (leader.compareTo(from) > 0) ? leader : new String(from);
				}
			}
		}
		if(!error) {
			if(myNum > max_val) {
				leader = gvh.getName();
				max_val = myNum;
			} else if(myNum == max_val) {
				leader = (leader.compareTo(gvh.getName()) > 0) ? leader : gvh.getName();
			}
			
			// Have determined a leader, broadcast the result
			RobotMessage bcast_leader = new RobotMessage("ALL", gvh.getName(), LogicThread.MSG_LEADERELECT_ANNOUNCE, leader);
			gvh.addOutgoingMessage(bcast_leader);
		}
	
		if(error) {
			// Receive any MSG_LEADERELECT_ANNOUNCE messages, accept whoever they elect as leader
			msgcount = 0;
			startWaitTime = System.currentTimeMillis();
			while(msgcount == 0) {
				msgcount = gvh.getIncomingMessageCount(LogicThread.MSG_LEADERELECT_ANNOUNCE);
				if(System.currentTimeMillis() > startWaitTime + MAX_WAIT_TIME) {
					Log.e(ERR, "Leader election failed!");
					return "ERROR!";
				}
			}
			leader = gvh.getIncomingMessages(LogicThread.MSG_LEADERELECT_ANNOUNCE).iterator().next().getContents();
		}
		
		Log.i(TAG, "Elected leader: " + leader);
		
		clear_msgbuffer(LogicThread.MSG_LEADERELECT);
		clear_msgbuffer(LogicThread.MSG_LEADERELECT_ANNOUNCE);
		
		return leader;
	}	
	
	
	//Clear any received messages
	private void clear_msgbuffer(int MID) {
		int toClear = gvh.getIncomingMessageCount(MID);
		Log.d(TAG, "Clearing " + toClear + " MID = " + MID + " messages from the buffer");
		@SuppressWarnings("unused")
		Collection<RobotMessage> clearbuf = gvh.getIncomingMessages(LogicThread.MSG_LEADERELECT);
	}
}
