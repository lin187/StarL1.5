package edu.illinois.mitra.Objects;

import java.util.ArrayList;

import android.util.Log;

import edu.illinois.mitra.LogicThread;
import edu.illinois.mitra.comms.RobotMessage;

public class MutualExclusion extends Thread {
	private static final String TAG = "Mutex";
	private static final String ERR = "Critical Error";
	
	private int num_sections = 0;
	private globalVarHolder gvh;
	private String name;
	
	private ArrayList<ArrayList<String>> token_requestors;
	private String[] token_owners;
	private Boolean[] using_token;
	
	public MutualExclusion(int num_sections, globalVarHolder gvh, String leader) {
		this.num_sections = num_sections;
		this.gvh = gvh;
		this.name = gvh.getName();
	
		// Set leader as token owner for all tokens
		token_owners = new String[num_sections];
		using_token = new Boolean[num_sections];
		token_requestors = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < num_sections; i++) {
			token_owners[i] = new String(leader);
			using_token[i] = false;
			token_requestors.add(i, new ArrayList<String>());
		}
	}

	@Override
	public void run() {
		int msgcount = 0;
		String output = "";
		while(true) {
			// Receive token ownership broadcasts
			msgcount = gvh.getIncomingMessageCount(LogicThread.MSG_MUTEX_TOKEN_OWNER_BCAST);
			for(int i = 0; i < msgcount; i ++) {
				RobotMessage next = gvh.getIncomingMessage(LogicThread.MSG_MUTEX_TOKEN_OWNER_BCAST);
				String parts[] = next.getContents().split(",");
				int id = Integer.parseInt(parts[0]);
				token_owners[id] = parts[1];
				Log.i(TAG, "--> Token " + id + " is now owned by " + parts[1]);
			}
			
			// Receive token messages
			msgcount = gvh.getIncomingMessageCount(LogicThread.MSG_MUTEX_TOKEN);
			for(int i = 0; i < msgcount; i ++) {
				RobotMessage next = gvh.getIncomingMessage(LogicThread.MSG_MUTEX_TOKEN);
				String parts[] = next.getContents().split(",");
				int id = Integer.parseInt(parts[0]);
				token_owners[id] = name;
				using_token[id] = true;
				// Parse any attached requestors
				if(parts.length > 1) {
					Log.i(TAG, "Parsing attached requestors...");
					token_requestors.get(id).clear();
					for(int b = 1; b < parts.length; b++) {
						token_requestors.get(id).add(parts[b]);
					}
					Log.i(TAG, "Requestors: " + token_requestors.get(id).toString());
				}
				Log.e(TAG, "Received token " + id + "!");
			}
			
			// Receive token requests
			msgcount = gvh.getIncomingMessageCount(LogicThread.MSG_MUTEX_TOKEN_REQUEST);
			for(int i = 0; i < msgcount; i ++) {
				RobotMessage next = gvh.getIncomingMessage(LogicThread.MSG_MUTEX_TOKEN_REQUEST);
				String parts[] = next.getContents().split(",");
				int id = Integer.parseInt(parts[0]);
				// If we own the token being requested, enqueue the requestor
				if(token_owners[id].equals(name)) {
					token_requestors.get(id).add(next.getFrom());
				} else {
				// If we don't own the token, forward the request to the actual owner
					next.setTo(token_owners[id]);
					gvh.addOutgoingMessage(next);
				}
				Log.i(TAG, next.getFrom() + " has requested token " + id);
			}
			
			// Send any unused tokens on to the next requestor
			for(int i = 0; i < num_sections; i ++) {
				if(token_owners[i].equals(name) && !using_token[i] && !token_requestors.get(i).isEmpty()) {					
					// Pass the token. Include any additional requestors
					String to = token_requestors.get(i).remove(0);
					RobotMessage pass_token;
					Log.d(TAG, "Passing token " + i + " to requestor " + to);
					if(token_requestors.get(i).isEmpty()) {
						Log.d(TAG, "No additional requestors to include.");
						pass_token = new RobotMessage(to, name, LogicThread.MSG_MUTEX_TOKEN, Integer.toString(i));
					} else {
						Log.d(TAG, "Including list of requestors.");
						String reqs = token_requestors.get(i).toString().replaceAll("[\\[\\]\\s]", "");
						token_requestors.get(i).clear();
						pass_token = new RobotMessage(to, name, LogicThread.MSG_MUTEX_TOKEN, i + "," + reqs);
					}
					gvh.addOutgoingMessage(pass_token);
					token_owners[i] = to;
					
					// Broadcast the new token owner
					RobotMessage owner_broadcast = new RobotMessage("ALL", name, LogicThread.MSG_MUTEX_TOKEN_OWNER_BCAST, i + "," + to);
					gvh.addOutgoingMessage(owner_broadcast);
				}
			}
			
			// Show token owners on the debug
			output = "";
			for(int i = 0; i < num_sections; i++) {
				output = output + i + " " + token_owners[i] + "-" + token_requestors.get(i) + "\n";
			}
			gvh.setDebugInfo(new String(output));
		}
	}

	public synchronized void requestEntry(int id) {
		Log.d(TAG, "Requesting entry to section " + id);
		if(!token_owners[id].equals(name)) {
			// Send a token request message to the owner
			RobotMessage token_request = new RobotMessage(token_owners[id], name, LogicThread.MSG_MUTEX_TOKEN_REQUEST, Integer.toString(id));
			gvh.addOutgoingMessage(token_request);
			Log.d(TAG, "Sent token " + id + " request to owner " + token_owners[id]);
		} else {
			using_token[id] = true;
		}
	}
	
	public synchronized boolean clearToEnter(int id) {
		return using_token[id];
	}

	public synchronized void exit(int id) {
		Log.d(TAG, "Exiting section " + id);
		using_token[id] = false;
	}
	
	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		super.start();
	}
}
