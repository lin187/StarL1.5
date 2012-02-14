 package edu.illinois.mitra.comms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import android.util.Log;
import edu.illinois.mitra.Objects.globalVarHolder;

public class CommsHandler extends Thread {
	public static final int TIMEOUT = 250;
	public static final int MSG_LIFESPAN = 30000;
	public static final int MAX_RETRIES = 15;
	private static final String TAG = "ProtocolThread";
	private static final String ERR = "Critical Error";
	
	private int seqNum = 0;
	private String name;
	    
    // Incoming and Outgoing message lists
    private ArrayList<UDPMessage> InMsgList = new ArrayList<UDPMessage>();
    private ArrayList<UDPMessage> OutMsgList = new ArrayList<UDPMessage>();
    private ArrayList<UDPMessage> ReceivedMsgList = new ArrayList<UDPMessage>();
    
    // Participant names and IP addresses
    private HashMap<String,String> participants;
	
    // Connected threads and objects
    private globalVarHolder gvh;
	private ComThread mConnectedThread;
	    
	public CommsHandler(HashMap<String,String> participants, String name, globalVarHolder gvh) {
		this.participants = participants;
		this.name = name;
		this.gvh = gvh;
		
		Random rand = new Random();
		seqNum = rand.nextInt(10000);
		
        mConnectedThread = new ComThread(ReceivedMsgList, name);
	}
	
	public synchronized void addOutgoing(RobotMessage msg, MessageResult result) {
		UDPMessage newMsg = new UDPMessage(seqNum, UDPMessage.MSG_QUEUED, msg);
		newMsg.setHandler(result);
		OutMsgList.add(newMsg);
		seqNum = (seqNum + 1) % 9999;
	}
	
	public synchronized void clear() {
		InMsgList.clear();
		OutMsgList.clear();
		ReceivedMsgList.clear();
	}
	
	@Override
	public synchronized void start() {
		mConnectedThread.start();
		Log.e(TAG, "Starting protocol thread...");
		super.start();
	}
	
    @Override
	public void run() {
    	super.run();
    	Log.i(TAG, "protocol thread running...");
    	while(true) {
    		// Send any outgoing messages (queued or expired)
    		if(isPendingMessage()) {
    			UDPMessage toSend = nextPendingMessage();
    	        mConnectedThread.write(toSend, nameToIP(toSend));
    		}
    		
    		// Send any outgoing ACKs
    		if(isPendingACK()) {
    			UDPMessage toSend = nextPendingACK();
    			mConnectedThread.write(toSend, nameToIP(toSend));
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
    	long time = System.currentTimeMillis();
    	
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
    private UDPMessage nextPendingACK() {
		for(int i = 0; i < InMsgList.size(); i++) {
			UDPMessage current = InMsgList.get(i);
			if(current.getState() == UDPMessage.MSG_RECEIVED && !current.isACK()) {
				current.setState(UDPMessage.MSG_ACK_SENT);
				InMsgList.set(i, current);
				
				// Create an ACK msg to the sender from me for the received sequence number
				RobotMessage ackmsg = new RobotMessage(current.getContents().getFrom(), name, 0, "ACK");
				return new UDPMessage(current.getSeqNum(), -1, ackmsg);
			}
		}
		return null;
	}

	private boolean isPendingACK() {
		for(int i = 0; i < InMsgList.size(); i++) {
			// If there's a message in the RECEIVED state that isn't an ACK, it must be ACK'd
			if(InMsgList.get(i).getState() == UDPMessage.MSG_RECEIVED && !InMsgList.get(i).isACK()) {
				return true;
			}
		}
		return false;
	}

	// Handle incoming messages 
	private void handleIncoming() {
		int toHandle = ReceivedMsgList.size();
		for(int i = 0; i < toHandle; i++) {
			UDPMessage current = ReceivedMsgList.get(0);

			// If the message is an ACK, handle it
			if(current.isACK()) {
				handleAck(current);
			// If the received message is a data message, handle it 
			} else {
				handleDataMsg(current);
			}
			
			ReceivedMsgList.remove(current);
		}
	}

	private void handleDataMsg(UDPMessage current) {
		// If we've received a message from the same sender with the same contents and sequence number, this is a duplicate.
		int msg_idx = InMsgList.indexOf(current);
		
		// If it's a new message, add it to the InMsgList
		if(msg_idx == -1) {
			current.setState(UDPMessage.MSG_RECEIVED);
			InMsgList.add(current);
			
			// Add it to the gvh in queue
			gvh.addIncomingMessage(new RobotMessage(current.getContents()));
		} else {
		// If we've received it before, flag its duplicate for re-sending an ACK and reset it's received time
			Log.d(TAG, "-> Received duplicate message: " + current + "\n-> Flagging for re-ACKing");
			UDPMessage duplicate = InMsgList.get(msg_idx);
			duplicate.setState(UDPMessage.MSG_RECEIVED);
			duplicate.setReceivedTime(System.currentTimeMillis());
			InMsgList.set(msg_idx, duplicate);
		}
	}

	// Send a pending (queued or expired) outgoing message
	// If the message is a broadcast, delete it and add one outgoing sent message for each recipient
	// This ensures that repeated transmissions won't be sent to everyone again
	private UDPMessage nextPendingMessage() {
		for(int i = 0; i < OutMsgList.size(); i++) {
			UDPMessage current = new UDPMessage(OutMsgList.get(i));
			
			if(current.getState() == UDPMessage.MSG_QUEUED) {
				current.setState(UDPMessage.MSG_SENT);
				current.setSentTime(System.currentTimeMillis());
				
				if(current.isBroadcast() || current.isDiscovery()) {
					OutMsgList.remove(i);
					if(current.isBroadcast()) {
						Iterator<String> iter = participants.keySet().iterator();
						for(int b = 0; b < participants.size(); b ++) {
							String next = iter.next();
							if(!next.equals(name)) {
								RobotMessage current_msg = new RobotMessage(current.getContents());
								current_msg.setTo(next);
								UDPMessage udp_new = new UDPMessage(current.getSeqNum(), UDPMessage.MSG_SENT, current_msg);
								udp_new.setSentTime(System.currentTimeMillis());
								udp_new.setHandler(current.getHandler());
								OutMsgList.add(udp_new);
								Log.d(TAG, "Adding: " + udp_new);
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
	private boolean isPendingMessage() {
		long time = System.currentTimeMillis();
		for(int i = 0; i < OutMsgList.size(); i++) {
			UDPMessage current = OutMsgList.get(i); 
			
			if(current.getState() == UDPMessage.MSG_SENT && (time - current.getSentTime()) >= TIMEOUT) {
				// If the message has been retried too many times, signal failure
				if(current.getRetries() > MAX_RETRIES) {
					current.getHandler().setFailed();
					OutMsgList.remove(i);
					i --;
				} else {
					// Otherwise, mark the message for resending
					Log.i(TAG, "Found expired message: " + current);
					current.setState(UDPMessage.MSG_QUEUED);
					current.retry();
					OutMsgList.set(i, current);
					return true;
				}
			}
			
			if(current.getState() == UDPMessage.MSG_QUEUED) {
				return true;
				
			}
		}
		return false;
	}

	// Handle an ACK message by marking the associated sent message as ACK'd
	private synchronized void handleAck(UDPMessage ReceivedAck) {
		Log.d(TAG, "Entering handleAck for " + ReceivedAck);
		
		// Check the outgoing list for a sent message matching this ACK
		for(int i = 0; i < OutMsgList.size(); i++) {
			UDPMessage current = OutMsgList.get(i);
			Log.i(TAG, "--> Checking against " + current);
			
			if(current.getState() == UDPMessage.MSG_SENT && ackMatchesMessage(ReceivedAck, current)) {
				Log.i(TAG, "--> MATCH! " + current.getSeqNum());
				//Report that receipt was successful
				current.getHandler().setReceived();
				OutMsgList.remove(i);
				return;
			}
		}
		
		Log.e(TAG, "Received an unexpected ACK: " + ReceivedAck);
	}
	
    private boolean ackMatchesMessage(UDPMessage ack, UDPMessage msg) {
    	return (ack.getSeqNum() == msg.getSeqNum()) 
    			&& (ack.getContents().getFrom().equals(msg.getContents().getTo()))
    			&& (ack.getContents().getTo().equals(msg.getContents().getFrom()));
	}

	public void cancel() {
        try {
        	mConnectedThread.cancel();
        } catch (Exception e) {
            Log.e(ERR, "close() of connect socket failed", e);
        }
    }
	
	private String nameToIP(String to) {
		if(to.equals("ALL") || to.equals("DISCOVER")) {
			return "192.168.1.255";
		} else {
			if(!participants.containsKey(to)) {
				Log.e(ERR, "Can't find IP address for robot " + to);
			}
			return participants.get(to);
		}
	}
	
	private String nameToIP(UDPMessage msg_to_send) {
		return nameToIP(msg_to_send.getContents().getTo());
	}
}
