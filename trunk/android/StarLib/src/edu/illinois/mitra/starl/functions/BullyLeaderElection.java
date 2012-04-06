package edu.illinois.mitra.starl.functions;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.Common;

public class BullyLeaderElection implements Callable<String>, MessageListener, LeaderElection {
	private static final String TAG = "BullyElection";
	private static final String ERR = "Critical Error";
	
	private static final int TIMEOUT = 5000;
	private boolean elected = false;
	private boolean electing = false;
	private String leader = null;
	private GlobalVarHolder gvh = null;	
	private String name = null;
	
	private timeoutTask ttask = new timeoutTask();
	private ScheduledThreadPoolExecutor timeout = new ScheduledThreadPoolExecutor(1);
	private ExecutorService executor = new ScheduledThreadPoolExecutor(1);
	
	public BullyLeaderElection(GlobalVarHolder gvh) {
		elected = false;
		electing = false;
		leader = null;
		
		this.gvh = gvh;
		name = gvh.id.getName();
		
		registerMessages();
	}
	
	@Override
	public String elect() {
		if(leader == null) {
			String electedLeader = "ERROR";
			try {
				electedLeader = executor.submit(this).get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			unregisterMessages();
			return electedLeader;
		} else {
			return leader;
		}
	}

	@Override
	public void cancel() {
		timeout.shutdownNow();
		executor.shutdownNow();
		unregisterMessages();
	}
	
	private void registerMessages() {
		gvh.comms.addMsgListener(Common.MSG_BULLYANSWER, this);
		gvh.comms.addMsgListener(Common.MSG_BULLYELECTION, this);
		gvh.comms.addMsgListener(Common.MSG_BULLYWINNER, this);
	}
	private void unregisterMessages() {
		gvh.comms.removeMsgListener(Common.MSG_BULLYANSWER);
		gvh.comms.removeMsgListener(Common.MSG_BULLYELECTION);
		gvh.comms.removeMsgListener(Common.MSG_BULLYWINNER);		
	}

	@Override
	public void messageReceied(RobotMessage m) {
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
	public String call() throws Exception {
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
				Thread.sleep(10);
			}
			electing = false;
		}
		return leader;
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
}
