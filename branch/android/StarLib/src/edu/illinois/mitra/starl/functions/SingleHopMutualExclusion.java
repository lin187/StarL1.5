package edu.illinois.mitra.starl.functions;

import java.util.ArrayList;
import java.util.Set;

import android.util.Log;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.globalVarHolder;

public class SingleHopMutualExclusion extends Thread implements MutualExclusion, MessageListener {
	private static final String TAG = "Mutex";
	private static final String ERR = "Critical Error";
	
	private int num_sections = 0;
	private globalVarHolder gvh;
	private String name;
	
	private ArrayList<ArrayList<String>> token_requesters;
	private String[] token_owners;
	private Boolean[] using_token;
	
	private boolean running = true;

	public SingleHopMutualExclusion(int num_sections, globalVarHolder gvh, String leader) {
		this.num_sections = num_sections;
		this.gvh = gvh;
		this.name = gvh.getName();
	
		// Set leader as token owner for all tokens
		token_owners = new String[num_sections];
		using_token = new Boolean[num_sections];
		token_requesters = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < num_sections; i++) {
			token_owners[i] = new String(leader);
			using_token[i] = false;
			token_requesters.add(i, new ArrayList<String>());
		}
	}

	/* (non-Javadoc)
	 * @see edu.illinois.mitra.Objects.MutualExclusionInterface#run()
	 */
	@Override
	public void run() {
		String output = "";
		while(running) {		
			// Send any unused tokens on to the next requester
			for(int i = 0; i < num_sections; i ++) {
				if(token_owners[i].equals(name) && !using_token[i] && !token_requesters.get(i).isEmpty()) {					
					// Pass the token. Include any additional requesters
					String to = token_requesters.get(i).remove(0);
					RobotMessage pass_token;
					Log.d(TAG, "Passing token " + i + " to requester " + to);
					if(token_requesters.get(i).isEmpty()) {
						pass_token = new RobotMessage(to, name, common.MSG_MUTEX_TOKEN, Integer.toString(i));
					} else {
						String reqs = token_requesters.get(i).toString().replaceAll("[\\[\\]\\s]", "");
						token_requesters.get(i).clear();
						pass_token = new RobotMessage(to, name, common.MSG_MUTEX_TOKEN, i + "," + reqs);
					}
					gvh.addOutgoingMessage(pass_token);
					token_owners[i] = to;
					
					// Broadcast the new token owner
					RobotMessage owner_broadcast = new RobotMessage("ALL", name, common.MSG_MUTEX_TOKEN_OWNER_BCAST, i + "," + to);
					gvh.addOutgoingMessage(owner_broadcast);
				}
			}
			
			// Show token owners on the debug
			output = "";
			for(int i = 0; i < num_sections; i++) {
				if(using_token[i]) {
					output = output + i + " " + token_owners[i] + "*-" + token_requesters.get(i) + "\n";
				} else {
					output = output + i + " " + token_owners[i] + "-" + token_requesters.get(i) + "\n";
				}
			}
			gvh.setDebugInfo(output);
		}
	}

	public synchronized void requestEntry(int id) {
		Log.d(TAG, "Requesting entry to section " + id);
		if(!token_owners[id].equals(name)) {
			// Send a token request message to the owner
			RobotMessage token_request = new RobotMessage(token_owners[id], name, common.MSG_MUTEX_TOKEN_REQUEST, Integer.toString(id));
			gvh.addOutgoingMessage(token_request);
			Log.d(TAG, "Sent token " + id + " request to owner " + token_owners[id]);
		} else {
			using_token[id] = true;
		}
	}

	public synchronized void requestEntry(Set<Integer> ids) {
		for(int id : ids) {
			requestEntry(id);
		}
	}

	public synchronized boolean clearToEnter(int id) {
		return using_token[id];
	}

	public synchronized boolean clearToEnter(Set<Integer> ids) {
		boolean retval = true;
		for(int id : ids) {
			retval &= clearToEnter(id);
		}
		return retval;
	}

	public synchronized void exit(int id) {
		if(using_token[id]) {
			Log.d(TAG, "Exiting section " + id);
			using_token[id] = false;
		}
	}

	public synchronized void exit(Set<Integer> ids) {
		for(int id : ids) {
			exit(id);
		}
	}

	public synchronized void exitAll() {
		for(int i = 0; i < num_sections; i++) {
			exit(i);
		}
	}

	@Override
	public synchronized void start() {
		super.start();
		running = true;
		
		// Register message listeners
		gvh.addMsgListener(common.MSG_MUTEX_TOKEN_OWNER_BCAST, this);
		gvh.addMsgListener(common.MSG_MUTEX_TOKEN, this);
		gvh.addMsgListener(common.MSG_MUTEX_TOKEN_REQUEST, this);
	}

	public void cancel() {
		Log.d(TAG, "CANCELLING MUTEX THREAD");
		running = false;
		
		// Unregister message listeners
		gvh.removeMsgListener(common.MSG_MUTEX_TOKEN_OWNER_BCAST);
		gvh.removeMsgListener(common.MSG_MUTEX_TOKEN);
		gvh.removeMsgListener(common.MSG_MUTEX_TOKEN_REQUEST);
	}

	public void messageReceied(RobotMessage m) {
		
		
		String[] msgparts = m.getContents().split(",");
		int id = Integer.parseInt(msgparts[0]);
		
		switch(m.getMID()) {
		case common.MSG_MUTEX_TOKEN_OWNER_BCAST:
			token_owners[id] = msgparts[1];
			Log.i(TAG, "--> Token " + id + " is now owned by " + msgparts[1]);
			break;
			
			
		case common.MSG_MUTEX_TOKEN:
			token_owners[id] = name;
			using_token[id] = true;
			// Parse any attached requesters
			if(msgparts.length > 1) {
				Log.i(TAG, "Parsing attached requesters...");
				token_requesters.get(id).clear();
				for(int b = 1; b < msgparts.length; b++) {
					token_requesters.get(id).add(msgparts[b]);
				}
				Log.i(TAG, "requesters: " + token_requesters.get(id).toString());
			}
			Log.e(TAG, "Received token " + id + "!");
			break;
		
			
		case common.MSG_MUTEX_TOKEN_REQUEST:
			// If we own the token being requested, enqueue the requester
			if(token_owners[id].equals(name)) {
				token_requesters.get(id).add(m.getFrom());
			} else {
			// If we don't own the token, forward the request to the actual owner
				m.setTo(token_owners[id]);
				gvh.addOutgoingMessage(m);
			}
			Log.i(TAG, m.getFrom() + " has requested token " + id);
			break;
		}
		
	}
}
