package edu.illinois.mitra.starl.functions;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.StarLCallable;
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
public class RandomLeaderElection extends StarLCallable implements LeaderElection, MessageListener {

	private static final String TAG = "RandomLeaderElection";
	private static final String ERR = "Critical Error";
	private int nodes = 0;
	private static int MAX_WAIT_TIME = 5000;
	
	private SortedSet<Ballot> ballots;
	private Set<String> receivedFrom;
	
	private String announcedLeader;
	
	private ExecutorService executor = new ScheduledThreadPoolExecutor(1);

	private Future<List<Object>> elected;
	
	public RandomLeaderElection(GlobalVarHolder gvh) {
		super(gvh,"RandomLeaderElection");
		//results = new String[1];
		//nodes = gvh.id.getParticipants().size();
		//gvh.trace.traceEvent(TAG, "Created");
		registerListeners();
	}

	/**
	 * Executor implementing random leader election
	 */
	@Override
	public List<Object> callStarL() {
		// clear sets between successive calls (needs to be here instead of above in class declaration)
		ballots = new TreeSet<Ballot>();
		receivedFrom = new HashSet<String>();
		announcedLeader = null;
		
		results = new String[1];
		nodes = gvh.id.getParticipants().size();
		gvh.trace.traceEvent(TAG, "Created");
		
		gvh.trace.traceEvent(TAG, "Beginning Election");
		gvh.log.d(TAG, "Beginning election...");
		nodes = gvh.id.getParticipants().size();
		boolean error = false;

		// Generate a random number
		Random rand = new Random();
		int myNum = rand.nextInt(1000);
		receivedFrom.add(name);
		ballots.add(new Ballot(name, myNum));

		gvh.trace.traceVariable(TAG, "myNum", myNum);
		gvh.log.i(TAG, "My number is " + myNum);
		
		// Broadcast
		RobotMessage bcast = new RobotMessage("ALL", name, Common.MSG_RANDLEADERELECT, new MessageContents(myNum));
		gvh.comms.addOutgoingMessage(bcast);
		
		// Wait to receive MSG_LEADERELECT messages
		Long endTime = gvh.time()+MAX_WAIT_TIME;
		gvh.trace.traceEvent(TAG, "Waiting for MSG_LEADERELECT messages");
		while(!error && receivedFrom.size() < nodes) {
			if(gvh.time() >= endTime) {
				gvh.trace.traceEvent(TAG, "Waited timed out");
				gvh.log.e(TAG, "Waited too long!");
				
				Set<String> ptc = new HashSet<String>(gvh.id.getParticipants());
				ptc.removeAll(receivedFrom);
				System.out.println(name + " has waited too long to receive election messages. Have only received " + receivedFrom.size() + "\n\t\tWe're missing from " + ptc.toString());
				if(!receivedFrom.contains(name)) System.out.println("!!!!!" + name + " IS MISSING A VALUE FROM ITSELF??");
				error = true;
			}
			gvh.sleep(10);
		}
		
		gvh.log.d(TAG, "Received all numbers, determining leader.");
		// Determine the leader
		String leader = null;
		if(!error) {
			gvh.log.d(TAG, "No errors, determining leader now.");
			// Retrieve all names that submitted the largest random number, sort them
			leader = ballots.first().toString();
			gvh.trace.traceEvent(TAG, "Determined leader", leader);
			
			// Have determined a leader, broadcast the result
			RobotMessage bcast_leader = new RobotMessage("ALL", name, Common.MSG_RANDLEADERELECT_ANNOUNCE, new MessageContents(leader));
			gvh.comms.addOutgoingMessage(bcast_leader);
			gvh.trace.traceEvent(TAG, "Notified all of leader");
		}		
		if(error) {
			gvh.log.d(TAG, "An error occurred (waited too long?) must wait to receive announcement broadcasts.");
			// Receive any MSG_LEADERELECT_ANNOUNCE messages, accept whoever they elect as leader
			endTime = gvh.time()+MAX_WAIT_TIME;
			gvh.trace.traceEvent(TAG, "Waiting for MSG_LEADERELECT_ANNOUNCE messages");
			while(announcedLeader == null) {
				if(gvh.time() > endTime) {
					gvh.trace.traceEvent(TAG, "Waited timed out, leader election failed");
					gvh.log.e(TAG, "Leader election failed!");
					results[0] = "ERROR";
					return returnResults();
				}
				gvh.sleep(10);
			}
			leader = announcedLeader;
		}
		gvh.log.i(TAG, "Elected leader: " + leader);
		gvh.trace.traceEvent(TAG, "Elected leader", leader);
		results[0] = leader;
		return returnResults();
	}
	
	public void messageReceied(RobotMessage m) {
		gvh.log.i(TAG, "Received a message from " + m.getFrom() + ": " + m.getContents(0));
		switch(m.getMID()) {
		case Common.MSG_RANDLEADERELECT:
			String from = m.getFrom();
			if(receivedFrom.contains(from)) {
				gvh.log.e(TAG, "Received from " + from + " twice!");
			} else {
				int val = Integer.parseInt(m.getContents(0));				
				ballots.add(new Ballot(from, val));
				receivedFrom.add(from);
				gvh.log.i(TAG, "Received " + receivedFrom.size());
				if(receivedFrom.size() == nodes) {
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
	
	/**
	 * Call the callStarL executor and initate leader election
	 * 
	 */
	public void elect() {
		elected = executor.submit(this);
	}

	/**
	 * Return the name of the determined leader, if leader election is finished, or null if not
	 * 
	 */
	@Override
	public String getLeader() {
		if(elected.isDone()) {
			try {
				return (String) elected.get().get(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			return "ERROR";
		}
		return null;
	}
	
	private void registerListeners() {
		gvh.comms.addMsgListener(Common.MSG_RANDLEADERELECT, this);
		gvh.comms.addMsgListener(Common.MSG_RANDLEADERELECT_ANNOUNCE, this);
	}

	private void unregisterListeners() {
		gvh.comms.removeMsgListener(Common.MSG_RANDLEADERELECT);
		gvh.comms.removeMsgListener(Common.MSG_RANDLEADERELECT_ANNOUNCE);		
	}
	
	/**
	 * Comparable class used to order agent votes lexicographically using agent identifiers 
	 * 
	 * @author Adam Zimmerman
	 *
	 */
	private class Ballot implements Comparable<Ballot> {
		public String candidate;
		public int value;
		
		public Ballot(String candidate, int value) {
			this.candidate = candidate;
			this.value = value;
		}
		
		public String toString() {
			return candidate;
		}

		@Override
		public int compareTo(Ballot other) {
			// compare using agent ids if their vote values are equal
			if(other.value == this.value) {
				return candidate.compareTo(other.candidate);
			}
			return value - other.value;
		}
	}
	
	@Override
	public void cancel() {
		executor.shutdownNow();
		unregisterListeners();
		gvh.trace.traceEvent(TAG, "Cancelled");
	}
}
