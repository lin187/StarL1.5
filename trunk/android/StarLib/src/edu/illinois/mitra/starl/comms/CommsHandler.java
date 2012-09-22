 package edu.illinois.mitra.starl.comms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.interfaces.ComThread;

/**
 * Implements the simple acknowledgment protocol (SAP!)
 * @deprecated Has been replaced by the new SmartCommsHandler
 * @see SmartCommsHandler
 * @author Adam Zimmerman
 * @version 1.2
 */
@Deprecated
public class CommsHandler extends Thread implements Cancellable {
	public static final int TIMEOUT = 250;
	public static final int MSG_LIFESPAN = 30000;
	public static final int MAX_RETRIES = 15;
	private static final String TAG = "CommsHandler";
	private static final String ERR = "Critical Error";
	
	protected int seqNum = 0;
	protected String name;
	    
    // Incoming and Outgoing message lists
	protected ArrayList<UDPMessage> InMsgList;
	protected ArrayList<UDPMessage> OutMsgList;
	protected ArrayList<UDPMessage> ReceivedMsgList;
    
    // Participant names and IP addresses
    protected Map<String,String> participants;
	
    // Connected threads and objects
    protected GlobalVarHolder gvh;
    protected ComThread mConnectedThread;
	    
	// Statistics tracking
	private int stat_resendAcks = 0;
	private int stat_timeouts = 0;	
	private int stat_sends = 0;
	private int stat_receives = 0;
	
	public CommsHandler(GlobalVarHolder gvh, ComThread mConnectedThread) {
		super("CommsHandler-"+gvh.id.getName());
		this.participants = gvh.id.getParticipantsIPs();
		this.name = gvh.id.getName();
		this.gvh = gvh;
		
		InMsgList = new ArrayList<UDPMessage>();
		OutMsgList = new ArrayList<UDPMessage>();
		ReceivedMsgList = new ArrayList<UDPMessage>();
		 
		seqNum = (new Random()).nextInt(10000);
		
		this.mConnectedThread = mConnectedThread;
		this.mConnectedThread.setMsgList(ReceivedMsgList);
		
		gvh.trace.traceEvent(TAG, "Created", gvh.time());
	}
	
	public synchronized void addOutgoing(RobotMessage msg, MessageResult result) {
		UDPMessage newMsg = new UDPMessage(seqNum, UDPMessage.MSG_QUEUED, msg);
		newMsg.setHandler(result);
		OutMsgList.add(newMsg);
		seqNum = (seqNum + 1) % (Integer.MAX_VALUE-1);
		gvh.trace.traceEvent(TAG, "Sending", newMsg, gvh.time());
		gvh.log.i(TAG, "Sending " + newMsg);
		stat_sends ++;
	}
	
	public synchronized void clear() {
		InMsgList.clear();
		OutMsgList.clear();
		ReceivedMsgList.clear();
		gvh.trace.traceEvent(TAG, "Cleared", gvh.time());
		gvh.log.i(TAG, "Cleared");
	}
	
	@Override
	public synchronized void start() {
		mConnectedThread.start();
		super.start();
		gvh.trace.traceEvent(TAG, "Starting", gvh.time());
	}
	
    @Override
	public void run() {
    	super.run();
    	gvh.threadCreated(this);
    	while(true) {
    		gvh.sleep(1);

    		// Send any outgoing messages (queued or expired)
    		if(isPendingMessage()) {
    			UDPMessage toSend = nextPendingMessage();
    	        mConnectedThread.write(toSend, nameToIp(toSend));
    	        gvh.log.d(TAG, "PHYSICALLY sent " + toSend);
    		}
    		
    		// Send any outgoing ACKs
    		if(isPendingACK()) {
    			UDPMessage toSend = nextPendingACK();
    			mConnectedThread.write(toSend, nameToIp(toSend));
    			gvh.trace.traceEvent(TAG, "Sent ACK", toSend, gvh.time());
    			gvh.log.d(TAG, "PHYSICALLY ACK'd " + toSend);
    		}
    		
    		// Handle any newly received messages
    		if(ReceivedMsgList.size() > 0) {
    			handleIncoming();
    		}
    		
    		// Clean the list of received messages
    		cleanReceived();
    	}
	}

    
    // Remove any ACK'd messages that are older than MSG_LIFESPAN
    private void cleanReceived() {
    	int idx = 0;
    	long time = gvh.time();
    	
    	while(idx < InMsgList.size()) {
    		UDPMessage current = InMsgList.get(idx);

    		if(current.getState() == UDPMessage.MSG_ACK_SENT && (time - (current.getReceivedTime()) >= MSG_LIFESPAN)) {
    			InMsgList.remove(idx);
    		} else {
    			idx ++;
    		}
    	}
	}

	// Return the UDPMessage for the next ACK message to be sent
    protected UDPMessage nextPendingACK() {
		for(int i = 0; i < InMsgList.size(); i++) {
			UDPMessage current = InMsgList.get(i);
			if(current.getState() == UDPMessage.MSG_RECEIVED && !current.isACK() && !current.getContents().getFrom().equals(name)) {
				// This message has been received but not ACK'd, it isn't an ACK, and it's not from ourself
				current.setState(UDPMessage.MSG_ACK_SENT);
				InMsgList.set(i, current);
				
				// Create an ACK msg to the sender from me for the received sequence number
				return new UDPMessage(current.getSeqNum(), -1, new RobotMessage(current.getContents().getFrom(), name, 0, new MessageContents("ACK")));
			}
		}
		return null;
	}

    protected boolean isPendingACK() {
		for(UDPMessage current : InMsgList) {
			// If there's a message in the RECEIVED state that isn't an ACK and isn't from us, it must be ACK'd
			if(current.getState() == UDPMessage.MSG_RECEIVED && !current.isACK() && !current.getContents().getFrom().equals(name)) {
				return true;
			}
		}
		return false;
	}

	// Handle incoming messages 
	protected void handleIncoming() {
		int toHandle = ReceivedMsgList.size();
		for(int i = 0; i < toHandle; i++) {
		UDPMessage current = ReceivedMsgList.get(0);
		if(current != null) {
			// If the message is an ACK, handle it
			if(current.isACK()) {
				handleAck(current);
			// If the received message is a data message, handle it 
			} else {
				handleDataMsg(current);
			}
		}
		ReceivedMsgList.remove(current);
		}
	}

	protected void handleDataMsg(UDPMessage current) {
		// If we've received a message from the same sender with the same contents and sequence number, this is a duplicate.
		int msg_idx = InMsgList.indexOf(current);
		
		// If it's a new message, add it to the InMsgList
		if(msg_idx == -1) {
			// Discovery messages are not ACK'd and aren't added to InMsgList
			if(!current.isDiscovery()) {
				current.setState(UDPMessage.MSG_RECEIVED);
				InMsgList.add(current);
			}
			
			// If it's a broadcast from ourself, disregard it
			if(current.isBroadcast() && current.getContents().getFrom().equals(name)) {
				return;
			}
			
			gvh.trace.traceEvent(TAG, "Received data message", current, gvh.time());
			gvh.log.i(TAG, "Incoming data message " + current);
			stat_receives ++;
			
			// Add it to the gvh in queue
			gvh.comms.addIncomingMessage(new RobotMessage(current.getContents()));
		} else {
		// If we've received it before, flag its duplicate for re-sending an ACK and reset it's received time
			gvh.log.e(TAG, "--> Received duplicate message: " + current + "\n--> Flagging for re-ACKing");
			gvh.trace.traceEvent(TAG, "Duplicate message", current, gvh.time());
			UDPMessage duplicate = InMsgList.get(msg_idx);
			stat_resendAcks ++;
			duplicate.setState(UDPMessage.MSG_RECEIVED);
			duplicate.setReceivedTime(gvh.time());
			InMsgList.set(msg_idx, duplicate);
		}
	}

	// Send a pending (queued or expired) outgoing message
	// If the message is a broadcast, delete it and add one outgoing sent message for each recipient
	// This ensures that repeated transmissions won't be sent to everyone again
	protected UDPMessage nextPendingMessage() {
		for(int i = 0; i < OutMsgList.size(); i++) {
			UDPMessage current = new UDPMessage(OutMsgList.get(i));
			
			if(current.getState() == UDPMessage.MSG_QUEUED) {
				current.setState(UDPMessage.MSG_SENT);
				current.setSentTime(gvh.time());
				
				if(current.isBroadcast() || current.isDiscovery()) {
					OutMsgList.remove(i);
					if(current.isBroadcast()) {
						for(String next : participants.keySet()) {
							if(!next.equals(name)) {
								RobotMessage current_msg = new RobotMessage(current.getContents());
								current_msg.setTo(next);
								UDPMessage udp_new = new UDPMessage(current.getSeqNum(), UDPMessage.MSG_SENT, current_msg);
								udp_new.setSentTime(gvh.time());
								udp_new.setHandler(current.getHandler());
								OutMsgList.add(udp_new);
							}
						}
					}
				} else {
					OutMsgList.set(i, current);
				}
				
				return current;
			}
		}
		return null;
	}

	// Check for queued or expired outgoing messages
	protected boolean isPendingMessage() {
		long time = gvh.time();
		for(int i = 0; i < OutMsgList.size(); i++) {
			UDPMessage current = OutMsgList.get(i);
			try {
			if(current.getState() == UDPMessage.MSG_SENT && (time - current.getSentTime()) >= TIMEOUT) {
				// If the message has been retried too many times, signal failure
				if(current.getRetries() > MAX_RETRIES) {
					current.getHandler().setFailed();
					OutMsgList.remove(i);
					i --;
				} else {
					// Otherwise, mark the message for resending
					gvh.log.i(TAG, "Found expired unacked outgoing message: " + current);
					gvh.log.i(TAG, "\t Message age: " + (time - current.getSentTime()) + " ms");
					stat_timeouts ++;
					current.setState(UDPMessage.MSG_QUEUED);
					current.retry();
					OutMsgList.set(i, current);
					return true;
				}
			}
			
			if(current.getState() == UDPMessage.MSG_QUEUED) {
				return true;
			}
			} catch(NullPointerException e) {
				gvh.log.e(TAG, "CAUGHT A NULL POINTER EXCEPTION IN isPendingMessage()");
			}
		}
		return false;
	}

	// Handle an ACK message by marking the associated sent message as ACK'd
	protected synchronized void handleAck(UDPMessage ReceivedAck) {
		gvh.log.i(TAG, "Handling ACK " + ReceivedAck);
		gvh.trace.traceEvent(TAG, "Received ACK", ReceivedAck, gvh.time());
		
		// Check the outgoing list for a sent message matching this ACK
		for(int i = 0; i < OutMsgList.size(); i++) {
			UDPMessage current = OutMsgList.get(i);
			
			if(current.getState() == UDPMessage.MSG_SENT && ackMatchesMessage(ReceivedAck, current)) {
				//Report that receipt was successful
				current.getHandler().setReceived();
				OutMsgList.remove(i);
				return;
			}
		}
		
		gvh.log.e(TAG, "Received an unexpected ACK: " + ReceivedAck);
	}
	
	protected boolean ackMatchesMessage(UDPMessage ack, UDPMessage msg) {
    	return (ack.getSeqNum() == msg.getSeqNum()) 
    			&& (ack.getContents().getFrom().equals(msg.getContents().getTo()))
    			&& (ack.getContents().getTo().equals(msg.getContents().getFrom()));
	}

    public void cancel() {
    	gvh.log.i(TAG, "Cancelling comms handler");
        mConnectedThread.cancel();
		gvh.trace.traceEvent(TAG, "Cancelled", gvh.time());
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
	
	public void printStatistics() {
		System.out.println("Sends: " + stat_sends);
		System.out.println("Receives: " + stat_receives);
		
		System.out.println("Resends: " + stat_resendAcks);
		System.out.println("Timeouts: " + stat_timeouts);
	}
}
