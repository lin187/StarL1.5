package edu.illinois.mitra.starl.functions;

import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.Common;

/**
 * Elects a leader. All agents broadcast a random integer, the robot with the largest number wins the election.
 * Ties are broken by comparing agent identifiers lexicographically (word of the day). To ensure proper operation,
 * robots should be synchronized before electing!
 * 
 * @author Adam Zimmerman
 * @version 1.0
 *
 */
public class RandomLeaderElection implements Callable<String>, LeaderElection, MessageListener {

	private static final String TAG = "RandomLeaderElection";
	private static final String ERR = "Critical Error";
	private GlobalVarHolder gvh = null;
	private int nodes = 0;
	private boolean error = false;
	private static int MAX_WAIT_TIME = 4000;
	
	private SortedMap<Integer,String> received;
	private String announcedLeader = null;
	private int largest = -1;
	
	private ExecutorService executor = new ScheduledThreadPoolExecutor(1);

	public RandomLeaderElection(GlobalVarHolder gvh) {
		this.gvh = gvh;
		nodes = gvh.id.getParticipants().size();
		received = new TreeMap<Integer,String>();
		received.clear();
		gvh.trace.traceEvent(TAG, "Created");
		registerListeners();
	}
			
	@Override
	public String call() throws Exception {
		gvh.trace.traceEvent(TAG, "Beginning Election");
		nodes = gvh.id.getParticipants().size();
		error = false;

		// Generate a random number
		Random rand = new Random();
		int myNum = rand.nextInt(1000);
		received.put(myNum, gvh.id.getName());

		gvh.trace.traceVariable(TAG, "myNum", myNum);
		gvh.log.i(TAG, "My number is " + myNum);
		
		// Broadcast
		RobotMessage bcast = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_RANDLEADERELECT, new MessageContents(myNum));
		gvh.comms.addOutgoingMessage(bcast);
		
		// Wait to receive MSG_LEADERELECT messages
		Long startWaitTime = System.currentTimeMillis();
		Long endTime = startWaitTime+MAX_WAIT_TIME;
		gvh.trace.traceEvent(TAG, "Waiting for MSG_LEADERELECT messages");
		while(!error && received.size() < nodes) {
			if(System.currentTimeMillis() > endTime) {
				gvh.trace.traceEvent(TAG, "Waited timed out");
				gvh.log.e(TAG, "Waited too long!");
				error = true;
			}
			Sleep(10);
		}
		
		// Determine the leader
		String leader = null;
		if(!error) {
			// Retrieve all names that submitted the largest random number, sort them
			SortedSet<String> leader_candidates = new TreeSet<String>(received.tailMap(Math.max(largest,myNum)).values());
			
			// The leader is the first in the sorted list
			leader = leader_candidates.first();
			gvh.trace.traceEvent(TAG, "Determined leader", leader);
			
			// Have determined a leader, broadcast the result
			RobotMessage bcast_leader = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_RANDLEADERELECT_ANNOUNCE, new MessageContents(leader));
			gvh.comms.addOutgoingMessage(bcast_leader);
			gvh.trace.traceEvent(TAG, "Notified all of leader");
		}
		if(error) {
			// Receive any MSG_LEADERELECT_ANNOUNCE messages, accept whoever they elect as leader
			startWaitTime = System.currentTimeMillis();
			endTime = startWaitTime+MAX_WAIT_TIME;
			gvh.trace.traceEvent(TAG, "Waiting for MSG_LEADERELECT_ANNOUNCE messages");
			while(announcedLeader == null) {
				if(System.currentTimeMillis() > startWaitTime + MAX_WAIT_TIME) {
					gvh.trace.traceEvent(TAG, "Waited timed out, leader election failed");
					gvh.log.e(ERR, "Leader election failed!");
					return "ERROR";
				}
			}
			leader = announcedLeader;
		}
		gvh.log.i(TAG, "Elected leader: " + leader);
		gvh.trace.traceEvent(TAG, "Elected leader", leader);
		return leader;
	}
	
	public void messageReceied(RobotMessage m) {
		gvh.log.i(TAG, "Received a message from " + m.getFrom() + ": " + m.getContents(0));
		switch(m.getMID()) {
		case Common.MSG_RANDLEADERELECT:
			String from = m.getFrom();
			if(received.containsValue(from)) {
				error = true;
				gvh.log.e(TAG, "Received from " + from + " twice!");
			} else {
				int val = Integer.parseInt(m.getContents(0));
				if(val > largest) {
					largest = val;
				}
				received.put(val, from);
				gvh.log.i(TAG, "Received " + received.size());
				if(received.size() == nodes) {
					gvh.log.i(TAG, "READY TO ELECT A LEADER!");
				}
			}
			gvh.trace.traceEvent(TAG, "Received MSG_RANDLEADERELECT message", m);
			break;
			
		case Common.MSG_RANDLEADERELECT_ANNOUNCE:
			announcedLeader = m.getContents(0);
			gvh.trace.traceEvent(TAG, "Received MSG_RANDLEADERELECT_ANNOUNCE message", announcedLeader);
			break;
		}
	}
	
	public String elect() {
		Future<String> elected = executor.submit(this);
		try {
			return elected.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return "ERROR";
	}

	private void registerListeners() {
		gvh.comms.addMsgListener(Common.MSG_RANDLEADERELECT, this);
		gvh.comms.addMsgListener(Common.MSG_RANDLEADERELECT_ANNOUNCE, this);
	}

	private void unregisterListeners() {
		gvh.comms.removeMsgListener(Common.MSG_RANDLEADERELECT);
		gvh.comms.removeMsgListener(Common.MSG_RANDLEADERELECT_ANNOUNCE);		
	}
	
	@Override
	public void cancel() {
		executor.shutdownNow();
		unregisterListeners();
		gvh.trace.traceEvent(TAG, "Cancelled");
	}
	
	private void Sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
