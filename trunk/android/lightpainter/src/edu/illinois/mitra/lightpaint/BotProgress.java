package edu.illinois.mitra.lightpaint;

import edu.illinois.mitra.starl.comms.RobotMessage;

public class BotProgress {
	
	private int lastReportedLine = -1;
	private int lastReportedSegment = -1;
	private long lastReceivedTime = -1;
	
	public BotProgress() {
		lastReceivedTime = System.currentTimeMillis();
	}
	
	public void update(RobotMessage msg) {		
		// Contents are in the form [LINE]-[SEGMENT]
		lastReportedLine = Integer.parseInt(msg.getContents(0));
		lastReportedSegment = Integer.parseInt(msg.getContents(1));
		// Update the last received time
		lastReceivedTime = System.currentTimeMillis();
	}

	public long getLastReportTime() {
		return lastReceivedTime;
	}
	
	public int getLastLine() {
		return lastReportedLine;
	}
	
	public int getLastSegment() {
		return lastReportedSegment;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (lastReceivedTime ^ (lastReceivedTime >>> 32));
		result = prime * result + lastReportedLine;
		result = prime * result + lastReportedSegment;
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
		BotProgress other = (BotProgress) obj;
		if (lastReceivedTime != other.lastReceivedTime)
			return false;
		if (lastReportedLine != other.lastReportedLine)
			return false;
		if (lastReportedSegment != other.lastReportedSegment)
			return false;
		return true;
	}	
}
