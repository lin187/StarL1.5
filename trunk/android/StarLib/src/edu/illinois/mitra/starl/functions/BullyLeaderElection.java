package edu.illinois.mitra.starl.functions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.StarLCallable;
import edu.illinois.mitra.starl.objects.Common;

/**
 * Elects a leader using the Bully algorithm. <b>Still slightly untested! Currently broken!</b>
 * You know what? Don't use this right now, it's a work in progress.
 * 
 * @author Adam Zimmerman
 * @version 1.2
 */
public class BullyLeaderElection extends StarLCallable implements MessageListener {
	private static final String TAG = "BullyElection";
	private static final String ERR = "Critical Error";
	
	private static final int TIMEOUT = 5000;
	private boolean elected = false;
	private boolean electing = false;
	private String leader = null;
	private String name = null;
	
	private timeoutTask ttask = new timeoutTask();
	private ScheduledThreadPoolExecutor timeout = new ScheduledThreadPoolExecutor(1);
	private ExecutorService executor = new ScheduledThreadPoolExecutor(1);
	
	// TODO: Fix this entire class!
	public BullyLeaderElection(GlobalVarHolder gvh) {
		super(gvh, "BullyLeaderElection");
		elected = false;
		electing = false;
		leader = null;
		
		name = gvh.id.getName();
		
		registerMessages();
		results = new String[1];
	}
	
	public String elect() {
		if(leader == null) {
			List<Object> electedLeader = new LinkedList<Object>(Arrays.asList(new String[]{"ERROR"}));
			try {
				electedLeader = executor.submit(this).get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			unregisterMessages();
			return (String) electedLeader.get(0);
		} else {
			return leader;
		}
	}

	public void cancel() {
		timeout.shutdownNow();
		executor.shutdownNow();
		unregisterMessages();
	}
	
	private void registerMessages() {
		gvh.comms.addMsgListener(this, Common.MSG_BULLYANSWER, Common.MSG_BULLYELECTION, Common.MSG_BULLYWINNER);
	}
	private void unregisterMessages() {
		gvh.comms.removeMsgListener(Common.MSG_BULLYANSWER);
		gvh.comms.removeMsgListener(Common.MSG_BULLYELECTION);
		gvh.comms.removeMsgListener(Common.MSG_BULLYWINNER);		
	}

	public void messageReceived(RobotMessage m) {
		switch(m.getMID()) {
		case Common.MSG_BULLYELECTION:
			// Reply immediately and start my own election
			RobotMessage reply = new RobotMessage(m.getFrom(), name, Common.MSG_BULLYANSWER, (MessageContents)null);
			gvh.comms.addOutgoingMessage(reply);
			if(!electing) {
				gvh.log.d(TAG,"Received a message from " + m.getFrom() + ", replying and starting my own election");
				leader = elect();
			} else {
				gvh.log.d(TAG,"Received an election start message from " + m.getFrom() + ". I'm already running an election though!");
			}
			break;
		case Common.MSG_BULLYANSWER:
			// Stop the timeout timer
			gvh.log.d(TAG,"Response received from " + m.getFrom() + " stopping the timeout timer.");
			timeout.remove(ttask);
			timeout.shutdownNow();
			break;
		case Common.MSG_BULLYWINNER:
			// Stop the timeout timer
			timeout.remove(ttask);
			timeout.shutdownNow();
			
			leader = m.getContents(0);
			gvh.log.i(TAG,"Received a leader announce message for " + leader);
			elected = true;
			break;
		}
	}

	@Override
	public List<Object> callStarL() {
		if(!elected) {
			electing = true;
			// Send an election start message to everyone with a higher ID
			RobotMessage start = new RobotMessage(null,name,Common.MSG_BULLYELECTION,(MessageContents)null);
			int sentTo = 0;
			for(String other : gvh.id.getParticipants()) {
				if(other.compareTo(name) > 0) {
					gvh.log.d(TAG,"Sending an election start message to " + other);
					start.setTo(other);
					gvh.comms.addOutgoingMessage(new RobotMessage(start));
					sentTo ++;
				}
			}
	
			gvh.log.d(TAG,"Starting a timeout timer");
			// Start a timeout timer
			timeout.schedule(ttask,TIMEOUT*Common.cap(sentTo, 1), TimeUnit.MILLISECONDS);
			while(!elected) {
				gvh.sleep(10);
			}
			electing = false;
		}
		return returnResults();
	}
		
	class timeoutTask implements Runnable {
		@Override
		public void run() {
			System.out.println("Timeout expired! I am the leader! " + name);
			gvh.log.e(TAG,"Timeout expired! I'm the leader!");
			elected = true;
			leader = name;
			RobotMessage winner = new RobotMessage("ALL",name,Common.MSG_BULLYWINNER, new MessageContents(name));
			gvh.comms.addOutgoingMessage(winner);
		}	
	}

	public String getLeader() {
		return null;
	}
}
