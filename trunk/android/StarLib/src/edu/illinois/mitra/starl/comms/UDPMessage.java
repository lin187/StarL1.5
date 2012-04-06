package edu.illinois.mitra.starl.comms;

import java.util.HashMap;

import edu.illinois.mitra.starl.interfaces.Traceable;

public class UDPMessage implements Traceable {
	public static final int MSG_QUEUED = 0;
	public static final int MSG_SENT = 1;
	public static final int MSG_ACKD = 2;
	public static final int MSG_RECEIVED = 3;
	public static final int MSG_ACK_SENT = 4;
	
	public static final int REPORT_FAILED = 5;
	public static final int REPORT_DELIVERED = 6;
	
	// Messaging protocol variables
	private int seqNum;
	private int state = 0;
	private long sentTime = -1;
	private long receivedTime = -1;
	private int retries = 0;
	
	// Message contents
	private RobotMessage contents;
	
	// Success/failure handler
	private MessageResult handler;
	
	// Construct an outgoing UDPMessage
	public UDPMessage(int seqNum, int state, RobotMessage contents) {
		super();
		this.seqNum = seqNum;
		this.contents = contents;
		this.state = state;
	}

	// Construct a UDPMessage from a received string
	public UDPMessage(String contents, long receivedTime) {
		String[] parts = contents.split("\\|");
		this.contents = new RobotMessage(contents);
		this.seqNum = Integer.parseInt(parts[1]);
		this.state = UDPMessage.MSG_RECEIVED;
		this.receivedTime = receivedTime;
	}
	
	public UDPMessage(UDPMessage other) {
		this.contents = new RobotMessage(other.getContents());
		this.seqNum = other.seqNum;
		this.state = other.state;
		this.retries = other.retries;
		this.sentTime = other.sentTime;
		this.receivedTime = other.receivedTime;
		this.handler = other.handler;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
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
		retries ++;
	}
	
	public String toString() {
		return "M|" + Integer.toString(seqNum) + "|" + contents.toString();
	}
	
	public boolean isACK() {
		return (contents.getMID() == 0);
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
		result = prime * result
				+ ((contents == null) ? 0 : contents.hashCode());
		result = prime * result + seqNum;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UDPMessage other = (UDPMessage) obj;
		if (contents == null) {
			if (other.contents != null)
				return false;
		} else if (!contents.getFrom().equals(other.contents.getFrom())) {
			return false;
		} else if (!contents.getContentsList().equals(other.contents.getContentsList())) {
			return false;
		} else if (contents.getMID() != other.contents.getMID()) {
			return false;
		}
		if (seqNum != other.seqNum)
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