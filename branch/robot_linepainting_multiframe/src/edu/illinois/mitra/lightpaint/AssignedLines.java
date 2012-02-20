package edu.illinois.mitra.lightpaint;

import edu.illinois.mitra.comms.RobotMessage;

public class AssignedLines {
	private int my_startline = 0;
	private int my_endline = 0;
	private int my_startpoint = 0;
	private int my_endpoint = 0;
	
	private int my_currentline = 0;
	private int my_currentpoint = 0;
	
	public AssignedLines() {
	}
	
	public void parseAssignmentMessage(RobotMessage msg) {
		String[] parts = msg.getContents().split(",");
		String[] startParts = parts[0].split(":");
		String[] endParts = parts[1].split(":");
		
		my_startline = Integer.parseInt(startParts[0]);
		my_startpoint = Integer.parseInt(startParts[1]);
		my_endline = Integer.parseInt(endParts[0]);
		my_endpoint = Integer.parseInt(endParts[1]);
	}
	
	public void setCurToStart() {
		my_currentline = my_startline;
		my_currentpoint = my_startpoint;
	}
	
	public void setCurPos(int line, int point) {
		my_currentline = line;
		my_currentpoint = point;
	}
	
	public boolean includedInFrame() {
		return (my_startline >= 0 || my_endline >= 0 || my_startpoint >= 0 || my_endpoint >= 0);
	}
	
	public boolean equalsEndPos(int line, int point) {
		return (line == my_endline) && (point == my_endpoint);
	}
	
	// Return current line and current position in array {my_startline, my_startpoint}
	public int[] getCurPos() {
		return new int[]{my_currentline, my_currentpoint};
	}
	
	public int[] getStartPos() {
		return new int[]{my_startline, my_startpoint};
	}
	
	public String curString() {
		return new String(my_currentline + ", " + my_currentpoint); 
	}
	
	public String rangeString() {
		return new String(my_startline + ", " + my_startpoint + " -> " + my_endline + ", " + my_endpoint);
	}
	
	public int getCurLine() {
		return my_currentline;
	}
	
	public int getCurPoint() {
		return my_currentpoint;
	}
}
