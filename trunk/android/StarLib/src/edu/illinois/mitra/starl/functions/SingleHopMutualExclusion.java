package edu.illinois.mitra.starl.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.objects.Common;

/**
 * A mutual exclusion implementation in which permission tokens are exchanged in
 * a single message "hop". To ensure that no entity is locked out, token
 * requesters are enqueued by the token holder and requestor queues are passed
 * along with the tokens. The number of mutex sections and the agent initially
 * holding all tokens must be known at the time of construction.
 * 
 * @author Adam Zimmerman
 * @version 1.0
 * 
 */
public class SingleHopMutualExclusion implements MutualExclusion, MessageListener {
	private static final String TAG = "SingleHopMutex";
	private static final String ERR = "Critical Error";

	private int num_sections = 0;
	private GlobalVarHolder gvh;
	private String name;

	private ArrayList<ArrayList<String>> token_requesters;
	private static String[] token_owners;
	private Boolean[] using_token;

	/**
	 * Create a new SingleHopMutualExclusion
	 * 
	 * @param num_sections
	 *            the number of mutex tokens to track
	 * @param gvh
	 *            the main GVH
	 * @param leader
	 *            the identifier of an agent who initially holds all tokens. All
	 *            entities must use the same value in order for mutex to
	 *            function properly
	 */
	public SingleHopMutualExclusion(int num_sections, GlobalVarHolder gvh, String leader) {
		this.num_sections = num_sections;
		this.gvh = gvh;
		this.name = gvh.id.getName();

		// Set leader as token owner for all tokens
		token_owners = new String[num_sections];
		using_token = new Boolean[num_sections];
		token_requesters = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < num_sections; i++) {
			token_owners[i] = new String(leader);
			using_token[i] = false;
			token_requesters.add(i, new ArrayList<String>());
		}
		gvh.trace.traceEvent(TAG, "Created", gvh.time());

		// Register message listeners
		gvh.comms.addMsgListener(this, Common.MSG_MUTEX_TOKEN_OWNER_BCAST, Common.MSG_MUTEX_TOKEN, Common.MSG_MUTEX_TOKEN_REQUEST);
	}

	/**
	 *  just a place holder, do nothing
	 *  TODO: let the group be the group interested in using the Mutex algorithm
	 */
	public void setGroup(List<String> party){
		
	}
	
	public synchronized void requestEntry(int id) {
		gvh.trace.traceEvent(TAG, "Requesting entry to section", id);
		gvh.log.d(TAG, "Requesting entry to section " + id);
		if(!token_owners[id].equals(name)) {
			// Send a token request message to the owner
			RobotMessage token_request = new RobotMessage(token_owners[id], name, Common.MSG_MUTEX_TOKEN_REQUEST, new MessageContents(id));
			gvh.comms.addOutgoingMessage(token_request);
			gvh.log.d(TAG, "Sent token " + id + " request to owner " + token_owners[id]);
			gvh.trace.traceEvent(TAG, "Requested token", id);
		} else {
			using_token[id] = true;
			gvh.trace.traceEvent(TAG, "Using token", id);
			gvh.trace.traceVariable(TAG, "Using " + id, true, gvh.time());
		}
	}

	public synchronized void requestEntry(Set<Integer> ids) {
		gvh.trace.traceEvent(TAG, "Requesting entry to set of sections", ids, gvh.time());
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
			gvh.trace.traceVariable(TAG, "Using " + id, false, gvh.time());
			gvh.log.d(TAG, "Exiting section " + id);
			using_token[id] = false;
			passIfUnused(id);
		}
		System.out.println(name+ " exiting");
	}

	private synchronized void passIfUnused(int id) {
		//System.out.println(name + " The token user is: " + token_owners[id]);
		// If this token has a requester, send it
		if(using_token[id] == false && token_owners[id].equals(name) && !token_requesters.get(id).isEmpty()) {
			// Pass the token. Include any additional requesters
			String to = token_requesters.get(id).remove(0);
			token_owners[id] = to;
			System.out.println(name + " The token user changed to: " + token_owners[id]);
			RobotMessage pass_token;
			gvh.log.d(TAG, "Passing token " + id + " to requester " + to);
			gvh.trace.traceEvent(TAG, "Passing token to new owner", id + " " + to, gvh.time());
			if(token_requesters.get(id).isEmpty()) {
				pass_token = new RobotMessage(to, name, Common.MSG_MUTEX_TOKEN, new MessageContents(id));
			} else {
				String reqs = token_requesters.get(id).toString().replaceAll("[\\[\\]\\s]", "");
				token_requesters.get(id).clear();
				pass_token = new RobotMessage(to, name, Common.MSG_MUTEX_TOKEN, new MessageContents(Integer.toString(id), reqs));
			}
			gvh.comms.addOutgoingMessage(pass_token);
			System.out.println("Token pass: " + id + name + "to " + to);
			// Broadcast the new token owner
			RobotMessage owner_broadcast = new RobotMessage("ALL", name, Common.MSG_MUTEX_TOKEN_OWNER_BCAST, new MessageContents(Integer.toString(id), to));
			gvh.comms.addOutgoingMessage(owner_broadcast);
			gvh.trace.traceVariable(TAG, "Owner " + id, to, gvh.time());
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
	public void cancel() {
		gvh.trace.traceEvent(TAG, "Cancelled", gvh.time());
		gvh.log.d(TAG, "CANCELLING MUTEX THREAD");

		// Unregister message listeners
		gvh.comms.removeMsgListener(Common.MSG_MUTEX_TOKEN_OWNER_BCAST);
		gvh.comms.removeMsgListener(Common.MSG_MUTEX_TOKEN);
		gvh.comms.removeMsgListener(Common.MSG_MUTEX_TOKEN_REQUEST);
	}

	public void messageReceived(RobotMessage m) {
		int id = Integer.parseInt(m.getContents(0));

		switch(m.getMID()) {
		case Common.MSG_MUTEX_TOKEN_OWNER_BCAST:
			String owner = m.getContents(1);

			gvh.trace.traceEvent(TAG, "Received token owner broadcast", id);
			gvh.trace.traceVariable(TAG, "Owner " + id, owner, gvh.time());
			token_owners[id] = owner;
			gvh.log.i(TAG, "--> Token " + id + " is now owned by " + owner);
			break;

		case Common.MSG_MUTEX_TOKEN:
			gvh.trace.traceEvent(TAG, "Received token", id);
			token_owners[id] = name;
			using_token[id] = true;
			gvh.trace.traceVariable(TAG, "Using " + id, true, gvh.time());
			// Parse any attached requesters
			if(m.getContentsList().size() == 2) {
				gvh.log.i(TAG, "Parsing attached requesters...");
				token_requesters.get(id).clear();
				for(String req : m.getContents(1).split(",")) {
					token_requesters.get(id).add(req);
				}
				gvh.log.i(TAG, "requesters: " + token_requesters.get(id).toString());
			}
			gvh.log.e(TAG, "Received token " + id + "!");
			break;

		case Common.MSG_MUTEX_TOKEN_REQUEST:
			gvh.trace.traceEvent(TAG, "Received token request", id + " " + m.getFrom(), gvh.time());
			// If we own the token being requested, enqueue the requester
			if(token_owners[id].equals(name)) {
				token_requesters.get(id).add(m.getFrom());

				// Pass the token if we're not using it
				passIfUnused(id);
			} else {
				// If we don't own the token, forward the request to the actual owner
				m.setTo(token_owners[id]);
				gvh.comms.addOutgoingMessage(m);
			}
			gvh.log.i(TAG, m.getFrom() + " has requested token " + id);
			break;
		}

	}
}
