package edu.illinois.mitra.starl.comms;

import java.util.HashMap;

import edu.illinois.mitra.starl.interfaces.Traceable;

/**
 * UDPMessage is a wrapper for RobotMessage which includes additional
 * information to track acknowledgments. This is used only by low level message
 * handling and shouldn't be needed by StarL applications
 * 
 * @author Adam Zimmerman
 * @version 1.0
 */
public class UdpMessage implements Traceable {
	public static enum MessageState {
		QUEUED, SENT, ACK, RECEIVED, ACK_SENT
	};
	
	public static final int DEFAULT_MAX_RETRIES = 15;

	// Messaging protocol variables
	private int seqNum;
	private MessageState state = MessageState.QUEUED;
	private long sentTime = -1;
	private long receivedTime = -1;
	private int retries = 0;
	private int max_retries = DEFAULT_MAX_RETRIES;

	// Message contents
	private RobotMessage contents;

	// Success/failure handler
	private MessageResult handler;

	// Construct an outgoing UDPMessage
	public UdpMessage(int seqNum, MessageState state, RobotMessage contents) {
		super();
		this.seqNum = seqNum;
		this.contents = contents;
		this.state = state;
	}

	// Construct an outgoing UDPMessage with a custom number of retries
	public UdpMessage(int seqNum, MessageState state, int retries, RobotMessage contents) {
		super();
		this.seqNum = seqNum;
		this.contents = contents;
		this.state = state;
	}

	// Construct a UDPMessage from a received string
	public UdpMessage(String contents, long receivedTime) {
		String[] parts = contents.split("\\|");
		this.contents = new RobotMessage(contents);
		this.seqNum = Integer.parseInt(parts[1]);
		this.state = MessageState.RECEIVED;
		this.receivedTime = receivedTime;
	}

	public UdpMessage(UdpMessage other) {
		this.contents = new RobotMessage(other.getContents());
		this.seqNum = other.seqNum;
		this.state = other.state;
		this.retries = other.retries;
		this.sentTime = other.sentTime;
		this.receivedTime = other.receivedTime;
		this.handler = other.handler;
	}

	public MessageState getState() {
		return state;
	}

	public void setState(MessageState state) {
		this.state = state;
	}

	public int getSeqNum() {
		return seqNum;
	}

	public RobotMessage getContents() {
		return contents;
	}

	public long getSentTime() {
		return sentTime;
	}

	public void setSentTime(long l) {
		this.sentTime = l;
	}

	public int getRetries() {
		return retries;
	}

	public void retry() {
		retries++;
	}

	public int getMaxRetries() {
		return max_retries;
	}

	public String toString() {
		return "M|" + Integer.toString(seqNum) + "|" + contents.toString();
	}

	public boolean isACK() {
		return (contents.getMID() == 0);
	}

	public UdpMessage getAck(String from) {
		RobotMessage ack = new RobotMessage(contents.getFrom(), from, 0, "ACK");
		return new UdpMessage(seqNum, MessageState.ACK, ack);
	}

	public boolean isBroadcast() {
		return contents.getTo().equals("ALL");
	}

	public boolean isDiscovery() {
		return contents.getTo().equals("DISCOVER");
	}

	public long getReceivedTime() {
		return receivedTime;
	}

	public void setReceivedTime(long receivedTime) {
		this.receivedTime = receivedTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contents == null) ? 0 : contents.hashCode());
		result = prime * result + ((handler == null) ? 0 : handler.hashCode());
		result = prime * result + (int) (receivedTime ^ (receivedTime >>> 32));
		result = prime * result + retries;
		result = prime * result + (int) (sentTime ^ (sentTime >>> 32));
		result = prime * result + seqNum;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		UdpMessage other = (UdpMessage) obj;
		if(contents == null) {
			if(other.contents != null)
				return false;
		} else if(!contents.equals(other.contents))
			return false;
		if(handler == null) {
			if(other.handler != null)
				return false;
		} else if(!handler.equals(other.handler))
			return false;
		if(receivedTime != other.receivedTime)
			return false;
		if(retries != other.retries)
			return false;
		if(sentTime != other.sentTime)
			return false;
		if(seqNum != other.seqNum)
			return false;
		if(state != other.state)
			return false;
		return true;
	}

	public MessageResult getHandler() {
		return handler;
	}

	public void setHandler(MessageResult handler) {
		this.handler = handler;
	}

	@Override
	public HashMap<String, Object> getXML() {
		HashMap<String, Object> retval = new HashMap<String, Object>();
		retval.put("seqnum", seqNum);
		retval.put("state", state);
		retval.put("retries", retries);
		retval.putAll(contents.getXML());
		return retval;
	}
}