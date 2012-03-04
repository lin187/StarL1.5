package edu.illinois.mitra.starl.comms;

public class RobotMessage {
	private String to;
	private String from;
	private int MID;
	private String contents;

	public RobotMessage(String to, String from, int MID, String contents) {
		super();
		this.to = to;
		this.from = from;
		this.MID = MID;
		this.contents = contents;
	}
	
	public RobotMessage(RobotMessage other) {
		this.to = other.getTo();
		this.from = other.getFrom();
		this.contents = other.getContents();
		this.MID = other.getMID();
	}
	
	public RobotMessage(String received) {
		String parts[] = received.split("\\|");
		this.from = parts[2];
		this.to = parts[3];
		this.MID = Integer.parseInt(parts[4]);
		this.contents = parts[5];
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
	public String getContents() {
		return contents;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public void setMID(int mid) {
		this.MID = mid;
	}
	public void setContents(String to) {
		this.contents = to;
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
}
