package edu.illinois.mitra.starl.comms;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.harness.DecoupledDelayQueue;
import edu.illinois.mitra.starl.harness.DelayedRunnable;
import edu.illinois.mitra.starl.interfaces.SmartComThread;

/**
 * Implements the simple acknowledgment protocol (SAP!) in a much more efficient manner than the deprecated CommsHandler class.
 * @author Adam Zimmerman
 * @version 1.0
 * @see RobotMessage
 * @see SmartComThread
 * @see CommsHandler
 */
public class SmartCommsHandler extends Thread {
	private static final String TAG = "SmartCommsHandler";
	private static final String ERR = "Critical Error";
	
	public static final int TIMEOUT = 250;
	public static final int MAX_RETRIES = 15;
	
	protected int seqNum = 0;
	protected String name;
	
    // Participant names and IP addresses
    protected HashMap<String,String> participants;
	
    // Connected threads and objects
    protected GlobalVarHolder gvh;
    protected SmartComThread mConnectedThread;
	
	private DecoupledDelayQueue<DelayedRunnable> dq = new DecoupledDelayQueue<DelayedRunnable>();
	private HashMap<String, HashSet<Integer>> sentMessages = new HashMap<String, HashSet<Integer>>(); // Map from recipient -> unACK'd MIDs
	private HashSet<UDPMessage> receivedMessages = new HashSet<UDPMessage>(); // Set of all received messages, thinned occasinally by removing old/outdated messages
	
	/**
	 * Create a new SCH using the provided SmartComThread.
	 * @param gvh
	 * @param mConnectedThread
	 */
	public SmartCommsHandler(GlobalVarHolder gvh, SmartComThread mConnectedThread) {
		super("SmartCommsHandler-"+gvh.id.getName());
		this.participants = gvh.id.getParticipantsIPs();
		this.name = gvh.id.getName();
		this.gvh = gvh;
		 
		seqNum = (new Random()).nextInt(10000);
		
		this.mConnectedThread = mConnectedThread;
		mConnectedThread.setCommsHandler(this);
		
		gvh.trace.traceEvent(TAG, "Created");
		
		for(String s : participants.keySet()) {
			if(!s.equals(name)) {
				sentMessages.put(s, new HashSet<Integer>());
			}
		}
	}

	/**
	 * Add an outgoing message. Messages are sent as soon as the SmartComThread is available. This function blocks until the message has been sent.
	 * @param msg the message to send
	 * @param result the MessageResult object to update with the results of the send
	 */
	public synchronized void addOutgoing(RobotMessage msg, MessageResult result) {
		final UDPMessage newMsg = new UDPMessage(seqNum, UDPMessage.MSG_SENT, msg);
		newMsg.setHandler(result);
		
		// Immediately send the outgoing message
		String ip = nameToIp(newMsg);
		
		if (ip == null)
			throw new RuntimeException("Could not find robot ip for outgoing message. " +
					"Destination name is '" + newMsg.getContents().getTo() + "'");
		
		mConnectedThread.write(newMsg, ip);

		// Increment the sequence number
		seqNum++;// = (seqNum + 1) % (Integer.MAX_VALUE-1);

		// Handle the sent message list
		Collection<String> recipients = new HashSet<String>();
		if(newMsg.isBroadcast()) {
			recipients.addAll(participants.keySet());
		} else {
			recipients.add(newMsg.getContents().getTo());
		}
		
		recipients.remove(name);

		// For each recipient, schedule a timer and add its UDPMessage to the list
		for(String s : recipients) {
			UDPMessage next = new UDPMessage(newMsg);
			next.getContents().setTo(s);
			sentMessages.get(s).add(newMsg.getSeqNum());
			
			// Schedule a resend timer
			dq.add(new DelayedRunnable(gvh, TIMEOUT, new ResendTimer(next)));
		}
		this.interrupt();
	}
	
	/**
	 * Handles a received message. <b>Do not call this function in your code!</b> This is only to be called from the SmartCommsHandler to which this is linked!
	 * @param msg the received UDPMessage
	 */
	public synchronized void handleReceived(UDPMessage msg) {
		// If it's an ACK
		if(msg.isACK()) {
			handleAck(msg);
		} else if(!msg.getContents().getFrom().equals(name)) {
			synchronized(receivedMessages) {
				// If it's data we haven't seen before
				if(!receivedMessages.contains(msg)) {
					// Trigger a reception event
					gvh.log.d(TAG, "Received data message: " + msg);
					gvh.comms.addIncomingMessage(new RobotMessage(msg.getContents()));
				}
				
				// Insert/overwrite it in the received message list
				receivedMessages.add(msg);
			}
			
			// Send an ACK
			sendAck(msg);
		}
		this.interrupt();
	}
	
    @Override
	public void run() {
    	gvh.threadCreated(this);
    	scheduleCleaner();    	
    	while(true) {
    		long sleepFor = dq.getMinimumDelay(TimeUnit.MILLISECONDS);
    		if(sleepFor > 0) gvh.sleep(sleepFor);    		
    		Collection<DelayedRunnable> toRun = dq.pollAll();
    		for(DelayedRunnable dr : toRun) {
    			dr.run();
    		}
    	}
	}
	
	private void scheduleCleaner() {
		dq.add(new DelayedRunnable(gvh, TIMEOUT*(MAX_RETRIES+2), new Runnable() {
			@Override
			public void run() {
				// Remove any received messages older than TIMEOUT*(MAX_RETRIES+1)
				int maxAge = TIMEOUT*(MAX_RETRIES+1);
				Collection<Integer> toRemove = new HashSet<Integer>();
				
				synchronized(receivedMessages) {
					for(UDPMessage u : receivedMessages) {
						if((gvh.time()-u.getReceivedTime()) >= maxAge) {
							toRemove.add(u.getSeqNum());
						}
					}

					receivedMessages.removeAll(toRemove);
				}
				
				dq.add(new DelayedRunnable(gvh, TIMEOUT*(MAX_RETRIES+1), this));
			}
		}));
	}

	protected synchronized void handleAck(UDPMessage Ack) {
		int seq = Ack.getSeqNum();
		String from = Ack.getContents().getFrom();
		sentMessages.get(from).remove(seq);
	}
	
	private void sendAck(UDPMessage msg) {
		msg.setState(UDPMessage.MSG_ACK_SENT);
		UDPMessage toSend = msg.getAck(name);
		gvh.log.d(TAG, "Sending ACK: " + toSend);
		mConnectedThread.write(toSend, nameToIp(toSend));
	}
	
    protected String nameToIp(String to) {
		if(to.equals("ALL") || to.equals("DISCOVER")) {
			return "192.168.1.255";
		} else {
			if(!participants.containsKey(to)) {
				gvh.log.e(ERR, "Can't find IP address for robot " + to);
				gvh.log.e(ERR, participants.toString());
			}
			return participants.get(to);
		}
	}
	
	protected String nameToIp(UDPMessage msg_to_send) {
		return nameToIp(msg_to_send.getContents().getTo());
	}
	
	private class ResendTimer implements Runnable {
		private UDPMessage msg;
		private String to;
		private int seq;
		public ResendTimer(UDPMessage msg) {
			this.msg = msg;
			to = msg.getContents().getTo();
			seq = msg.getSeqNum();
		}
		@Override
		public void run() {		
			synchronized(sentMessages) {
				// If the message is still in the received msg list (hasn't been ACK'd)
				if(sentMessages.get(to).contains(seq)) {
					// If we're under the resend limit, resend. Otherwise, mark as failed
					if(msg.getRetries() < MAX_RETRIES) {
						msg.retry();
						mConnectedThread.write(msg, nameToIp(msg));
						dq.add(new DelayedRunnable(gvh, TIMEOUT, this));
					} else {
						msg.getHandler().setFailed();
						sentMessages.remove(msg.getSeqNum());
					}
				} else {
					msg.getHandler().setReceived();
				}
			}
		}
	}
}
