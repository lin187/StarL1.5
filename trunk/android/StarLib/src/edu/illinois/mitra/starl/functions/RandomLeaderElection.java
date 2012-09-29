package edu.illinois.mitra.starl.functions;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.Common;

/**
 * Leader election algorithm which picks a random robot from all of the
 * participants. Calling 'elect' will begin the election process. Periodically
 * calling 'getLeader' is required to advance the election state machine.
 * 
 * @author Adam Zimmerman
 */
public class RandomLeaderElection implements LeaderElection, MessageListener {

	private static final int MAX_WAIT_TIME = 5000;
	private static final String ERROR_RETURN = "ERROR";
	private SortedSet<Ballot> ballots;
	private Set<String> receivedFrom;

	private GlobalVarHolder gvh;

	private static enum Stage {
		WAIT_FOR_BALLOTS, WAIT_FOR_ANNOUNCEMENT, ERROR, DONE
	};

	private String leader;
	private long startTime;
	private int candidates;
	private Stage stage;
	private static Random rand = new Random();

	public RandomLeaderElection(GlobalVarHolder gvh) {
		this.gvh = gvh;
		gvh.comms.addMsgListener(Common.MSG_RANDLEADERELECT, this);
		gvh.comms.addMsgListener(Common.MSG_RANDLEADERELECT_ANNOUNCE, this);
	}

	@Override
	public String getLeader() {
		switch(stage) {
		case WAIT_FOR_BALLOTS:
			// If we've received all of the ballots, elect a leader and announce it
			if(ballots.size() == candidates) {
				leader = ballots.first().candidate;
				stage = Stage.DONE;
				announceLeader();
				return leader;
			}

			// If we've timed out, wait for the announcement
			if(gvh.time() - startTime > MAX_WAIT_TIME) {
				stage = Stage.WAIT_FOR_ANNOUNCEMENT;
				startTime = gvh.time();
				break;
			}
			break;
		case WAIT_FOR_ANNOUNCEMENT:
			// If we've timed out again, go to the error state
			if(gvh.time() - startTime > MAX_WAIT_TIME)
				stage = Stage.ERROR;
			break;
		case ERROR:
			return ERROR_RETURN;
		case DONE:
			return leader;
		}
		return null;
	}

	@Override
	public void elect() {
		ballots = new TreeSet<Ballot>();
		receivedFrom = new HashSet<String>();
		stage = Stage.WAIT_FOR_BALLOTS;
		candidates = gvh.id.getParticipants().size();
		startTime = gvh.time();
		leader = null;

		int myNumber = rand.nextInt(1000);
		RobotMessage myBallot = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_RANDLEADERELECT, new MessageContents(myNumber));
		gvh.comms.addOutgoingMessage(myBallot);
		ballots.add(new Ballot(gvh.id.getName(), myNumber));
	}

	@Override
	public void messageReceived(RobotMessage m) {
		String from = m.getFrom();
		switch(m.getMID()) {
		case Common.MSG_RANDLEADERELECT:
			if(stage != Stage.WAIT_FOR_BALLOTS)
				return;

			if(!receivedFrom.contains(from)) {
				receivedFrom.add(from);
				ballots.add(new Ballot(from, Integer.parseInt(m.getContents(0))));
			}
			break;
		case Common.MSG_RANDLEADERELECT_ANNOUNCE:
			if(stage != Stage.WAIT_FOR_ANNOUNCEMENT)
				return;
			// TODO: Make sure a quorum announced the same leader, otherwise error
			leader = m.getContents(0);
			stage = Stage.DONE;
			break;
		}
	}

	private void announceLeader() {
		RobotMessage announce = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_RANDLEADERELECT_ANNOUNCE, leader);
		gvh.comms.addOutgoingMessage(announce);
	}

	@Override
	public void cancel() {
		gvh.comms.removeMsgListener(Common.MSG_RANDLEADERELECT);
		gvh.comms.removeMsgListener(Common.MSG_RANDLEADERELECT_ANNOUNCE);
	}

	/**
	 * Comparable class used to order agent votes lexicographically using agent
	 * identifiers
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
			if(other.value == this.value)
				return candidate.compareTo(other.candidate);
			return value - other.value;
		}
	}
}
