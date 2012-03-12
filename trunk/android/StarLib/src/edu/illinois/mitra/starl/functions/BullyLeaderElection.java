package edu.illinois.mitra.starl.functions;

import android.os.Handler;
import android.util.Log;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.globalVarHolder;

public class BullyLeaderElection implements LeaderElection, MessageListener {
	private static final String TAG = "BullyElection";
	private static final String ERR = "Critical Error";
	
	private static final int TIMEOUT = 5000;
	private boolean elected = false;
	private boolean electing = false;
	private String leader = null;
	private globalVarHolder gvh = null;	
	private String name = null;
	private Handler timeout = null;
	
	public BullyLeaderElection(globalVarHolder gvh) {
		elected = false;
		electing = false;
		this.gvh = gvh;
		name = gvh.getName();
		timeout = new Handler();
		registerMessages();
		Log.e(TAG, "I am " + name);
	}
	
	@Override
	public String elect() {
		if(!elected) {
			electing = true;
			// Send an election start message to everyone with a higher ID
			RobotMessage start = new RobotMessage(null,name,common.MSG_BULLYELECTION,null);
			int sentTo = 0;
			for(String other : gvh.getParticipants()) {
				if(other.compareTo(name) > 0) {
					Log.d(TAG,"Sending an election start message to " + other);
					start.setTo(other);
					gvh.addOutgoingMessage(new RobotMessage(start));
					sentTo ++;
				}
			}
	
			Log.d(TAG,"Starting a timeout timer");
			// Start a timeout timer
			timeout.postDelayed(new Runnable() {
				@Override
				public void run() {
					Log.e(TAG,"Timeout expired! I'm the leader!");
					elected = true;
					leader = name;
					RobotMessage winner = new RobotMessage("ALL",name,common.MSG_BULLYWINNER,name);
					gvh.addOutgoingMessage(winner);
				}			
			},TIMEOUT*common.cap(sentTo, 1));
			
			// TODO:
			// This prevents the messageReceived function from executing somehow!!
			while(!elected) {}
			electing = false;
		}
		//while(!electing) {}
		return leader;
	}

	@Override
	public void cancel() {
		unregisterMessages();
	}

	@Override
	public void messageReceied(RobotMessage m) {
		switch(m.getMID()) {
		case common.MSG_BULLYELECTION:
			// Reply immediately and start my own election
			RobotMessage reply = new RobotMessage(m.getFrom(), name, common.MSG_BULLYANSWER, null);
			gvh.addOutgoingMessage(reply);
			if(!electing) {
				Log.d(TAG,"Received a message from " + m.getFrom() + ", replying and starting my own election");
				leader = elect();
			} else {
				Log.d(TAG,"Received an election start message from " + m.getFrom() + ". I'm already running an election though!");
			}
			break;
		case common.MSG_BULLYANSWER:
			// Stop the timeout timer
			Log.d(TAG,"Response received from " + m.getFrom() + " stopping the timeout timer.");
			timeout.removeCallbacksAndMessages(null);
			break;
		case common.MSG_BULLYWINNER:
			// Stop the timeout timer
			timeout.removeCallbacksAndMessages(null);
			leader = m.getContents();
			Log.i(TAG,"Received a leader announce message for " + leader);
			elected = true;
			break;
		}
		
	}
	
	private void registerMessages() {
		gvh.addMsgListener(common.MSG_BULLYANSWER, this);
		gvh.addMsgListener(common.MSG_BULLYELECTION, this);
		gvh.addMsgListener(common.MSG_BULLYWINNER, this);
	}
	private void unregisterMessages() {
		gvh.removeMsgListener(common.MSG_BULLYANSWER);
		gvh.removeMsgListener(common.MSG_BULLYELECTION);
		gvh.removeMsgListener(common.MSG_BULLYWINNER);		
	}

}
