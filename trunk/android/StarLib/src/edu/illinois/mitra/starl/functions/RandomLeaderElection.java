package edu.illinois.mitra.starl.functions;

import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import android.util.Log;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.globalVarHolder;

public class RandomLeaderElection implements LeaderElection, MessageListener {

	private static final String TAG = "LeaderElection";
	private static final String ERR = "Critical Error";
	private globalVarHolder gvh = null;
	private int nodes = 0;
	private boolean error = false;
	private static int MAX_WAIT_TIME = 4000;
	
	private SortedMap<Integer,String> received;
	private String announcedLeader = null;
	private int largest = -1;
	
	public RandomLeaderElection(globalVarHolder gvh) {
		this.gvh = gvh;
		nodes = gvh.getParticipants().size();
		received = new TreeMap<Integer,String>();
		registerListeners();
	}
	
	// Elects one of the participants as leader, returns their name
	public String elect() {		
		nodes = gvh.getParticipants().size();
		error = false;

		// Generate a random number
		Random rand = new Random();
		int myNum = rand.nextInt(1000);
		received.put(myNum, gvh.getName());

		Log.i(TAG, "My number is " + myNum);
		
		// Broadcast
		RobotMessage bcast = new RobotMessage("ALL", gvh.getName(), common.MSG_RANDLEADERELECT, Integer.toString(myNum));
		gvh.addOutgoingMessage(bcast);
		
		// Wait to receive MSG_LEADERELECT messages
		Long startWaitTime = System.currentTimeMillis();
		Long endTime = startWaitTime+MAX_WAIT_TIME;
		while(!error && received.size() < nodes) {
			if(System.currentTimeMillis() > endTime) {
				Log.e(TAG, "Waited too long!");
				error = true;
			}
		}
		
		// Determine the leader
		String leader = null;
		if(!error) {
			// Retrieve all names that submitted the largest random number, sort them
			SortedSet<String> leader_candidates = new TreeSet<String>(received.tailMap(Math.max(largest,myNum)).values());
			
			// The leader is the first in the sorted list
			leader = leader_candidates.first();
			
			// Have determined a leader, broadcast the result
			RobotMessage bcast_leader = new RobotMessage("ALL", gvh.getName(), common.MSG_RANDLEADERELECT_ANNOUNCE, leader);
			gvh.addOutgoingMessage(bcast_leader);
		}
		if(error) {
			// Receive any MSG_LEADERELECT_ANNOUNCE messages, accept whoever they elect as leader
			startWaitTime = System.currentTimeMillis();
			endTime = startWaitTime+MAX_WAIT_TIME;
			while(announcedLeader == null) {
				if(System.currentTimeMillis() > startWaitTime + MAX_WAIT_TIME) {
					Log.e(ERR, "Leader election failed!");
					return "ERROR";
				}
			}
			leader = announcedLeader;
		}
		Log.i(TAG, "Elected leader: " + leader);

		return leader;
	}	
	
	public void cancel() {
		unregisterListeners();
	}
	
	private void registerListeners() {
		gvh.addMsgListener(common.MSG_RANDLEADERELECT, this);
		gvh.addMsgListener(common.MSG_RANDLEADERELECT_ANNOUNCE, this);
	}

	private void unregisterListeners() {
		gvh.removeMsgListener(common.MSG_RANDLEADERELECT);
		gvh.removeMsgListener(common.MSG_RANDLEADERELECT_ANNOUNCE);		
	}
	
	public void messageReceied(RobotMessage m) {
		switch(m.getMID()) {
		case common.MSG_RANDLEADERELECT:
			String from = m.getFrom();
			if(received.containsValue(from)) {
				error = true;
				Log.e(TAG, "Received from " + from + " twice!");
			} else {
				int val = Integer.parseInt(m.getContents());
				if(val > largest) {
					largest = val;
				}
				received.put(val, from);
			}
			break;
			
		case common.MSG_RANDLEADERELECT_ANNOUNCE:
			announcedLeader = m.getContents();
			break;
		}
	}
}
