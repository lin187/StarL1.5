package edu.illinois.mitra.starl.comms;

import java.util.HashMap;
import java.util.List;

import edu.illinois.mitra.starl.interfaces.Traceable;

/**
 * The RobotMessage class is used to pass messages between agents. All messages have a recipient, sender,
 * ID number, and contents. The ID number is used to target a message to a specific thread. Only threads
 * which are registered MessageListeners for a particular ID will receive messages with that ID. No two
 * threads may be registered for the same message ID.
 * 
 * @author Adam Zimmerman
 * @version 1.0
 * 
 * @see edu.illinois.mitra.starl.interfaces.MessageListener
 * @see edu.illinois.mitra.starl.comms.MessageContents
 */
public class RobotMessage implements Traceable {
	private String to;
	private String from;
	private int MID;
	MessageContents contents;

	/**
	 * Creates a new RobotMessage with contents in MessageContents form
	 * @param to The message recipient
	 * @param from The message sender or originator
	 * @param MID The ID of this message
	 * @param contents The contents of this message, in MessageContents form
	 */
	public RobotMessage(String to, String from, int MID, MessageContents contents) {
		this.to = to;
		this.from = from;
		this.MID = MID;
		this.contents = contents;
	}
	
	/**
	 * Creates a new RobotMessage with contents in String form. This is equivalent to creating a
	 * RobotMessage with contents being the MessageContents of a single string.
	 * @param to The message recipient
	 * @param from The message sender or originator
	 * @param MID The ID of this message
	 * @param contents The contents of this message in String form 
	 */
	public RobotMessage(String to, String from, int MID, String contents) {
		this.to = to;
		this.from = from;
		this.MID = MID;
		this.contents = new MessageContents(contents);
	}
	
	
	
	/**
	 * @param other A RobotMessage to clone
	 */
	public RobotMessage(RobotMessage other) {
		this.to = other.getTo();
		this.from = other.getFrom();
		this.contents = other.contents;
		this.MID = other.getMID();
	}
	
	/**
	 * Creates a new RobotMessage from a received broadcast
	 * @param received The received message to be parsed
	 */
	public RobotMessage(String received) {
		String parts[] = received.split("\\|");
		this.from = parts[2];
		this.to = parts[3];
		this.MID = Integer.parseInt(parts[4]);
		this.contents = new MessageContents(parts[5]);
	}
	public List<String> getContentsList() {
		return contents.getContents();
	}
	public String getTo() {
		return to;
	}
	public String getFrom() {
		return from;
	}
	public int getMID() {
		return MID;
	}
	public String getContents(int location) {
		return contents.get(location);
	}
	public MessageContents getContents() {
		return contents;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public void setContents(MessageContents new_contents) {
		this.contents = new_contents;
	}

	@Override
	public String toString() {
		return from + "|" + to + "|" + MID + "|" + contents + "|&";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + MID;
		result = prime * result
				+ ((contents == null) ? 0 : contents.hashCode());
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
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
		RobotMessage other = (RobotMessage) obj;
		if (MID != other.MID)
			return false;
		if (contents == null) {
			if (other.contents != null)
				return false;
		} else if (!contents.equals(other.contents))
			return false;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}

	@Override
	public HashMap<String, Object> getXML() {
		HashMap<String, Object> retval = new HashMap<String, Object>();
		retval.put("to", to);
		retval.put("from", from);
		retval.put("mid", MID);
		retval.put("contents", contents);
		return retval;
	}
}
