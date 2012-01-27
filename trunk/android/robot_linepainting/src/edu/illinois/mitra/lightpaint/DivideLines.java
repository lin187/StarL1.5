package edu.illinois.mitra.lightpaint;

import java.util.Iterator;
import java.util.Set;

import android.util.Log;
import edu.illinois.mitra.LogicThread;
import edu.illinois.mitra.Objects.MutualExclusion;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.Objects.itemPosition;
import edu.illinois.mitra.Objects.positionList;
import edu.illinois.mitra.comms.RobotMessage;

public class DivideLines {
	private static final String TAG = "Assign";
	private static final String ERR = "Critical Error";
	
	private int num_lines = 0;
	private int total_distance = 0;
	private int num_robots = 0;
	private int num_intersections = 0;
	
	private int[] startingLine;
	private int[] endingLine;
	
	private int[] startingPoint;
	private int[] endingPoint;
	
	private LineSegment[] segments;
	
	private globalVarHolder gvh;
	
	public DivideLines(globalVarHolder gvh) {
		this.gvh = gvh;
		num_robots = gvh.getParticipants().size();		
		startingLine = new int[num_robots];
		endingLine = new int[num_robots];
		startingPoint = new int[num_robots];
		endingPoint = new int[num_robots];
	}
	
	public void processWaypoints() {
		positionList wpts = gvh.getWaypointPositions();
		Log.i(TAG, "Found " + wpts.getNumPositions() + " waypoints");
		
		// Get number of lines and intersections
		itemPosition metadata = wpts.getPosition("NUM_LINES");
		num_lines = metadata.getAngle();
		segments = new LineSegment[(num_lines+1)];
		metadata = wpts.getPosition("NUM_INTS");
		num_intersections = metadata.getAngle();
		
		Log.d(TAG, "Created segments array with " + (num_lines+1) + " entries");
		Log.d(TAG, "Expecting " + num_intersections + " intersections");
		
		// Initialize the array of LineSegments		
		for(int i = 0; i <= num_lines; i++) {
			itemPosition current = wpts.getPosition("LINE"+i);
			segments[i] = new LineSegment(i, current.getAngle());	
			total_distance += current.getAngle();
		}
		
		// Fill the array of LineSegments
		for(int i = (num_lines+3); i < wpts.getNumPositions(); i ++) {
			itemPosition current = wpts.getPositionAtIndex(i);
			if(current.getX() != -1) {
				int line = Integer.parseInt(current.getName().split("-")[0]);
				if(line >= 0 && line <= num_lines) {
					segments[line].addPostion(current);
				} else {
					// TODO: Remove this error trapping once we're sure it's no longer needed
					Log.e(ERR, "Parsed out of bounds line #" + line + " out of waypoint " + current);
				}
			}
		}
	}
	
	public void assignLineSegments() {
		// Determine the target distance for each robot
		double target = Math.floor(total_distance/num_robots);
		
		Log.i(TAG, "Total distance: " + total_distance);
		Log.i(TAG, "Total lines: " + num_lines);
		Log.i(TAG, "Robots: " + num_robots);
		Log.i(TAG, "Target distance per robot: " + target);

		// Assign segments to each robot until the average is exceeded
		int current_seg = 0;
		int current_point = 0;
		startingLine[0] = 0;
		startingPoint[0] = 0;
		for(int i = 0; i < num_robots-1; i++) {
			int current_dist = 0;
			Log.i(TAG, "Starting[" + i + "] = " + startingLine[i] + ":" + startingPoint[i]);
			while(current_dist <= target && current_seg < num_lines) {
				// If we've walked to the end of this segment, go to the next one
				if(segments[current_seg].getLength() == current_point) {
					current_seg ++;
					current_point = 0;				
					Log.i(TAG, "\tIncrement to segment " + current_seg);
				} else {
					current_point ++;
				}
				current_dist ++;
				Log.i(TAG, "Current distance: " + current_dist);
			}
			endingLine[i] = current_seg;
			endingPoint[i] = current_point;
			startingLine[i+1] = current_seg;
			startingPoint[i+1] = current_point;
			Log.i(TAG, "Ending[" + i + "] = " + endingLine[i] + ":" + endingPoint[i]);
		}
		endingLine[num_robots-1] = num_lines + 1;
		endingPoint[num_robots-1] = 0;
		Log.i(TAG, "Starting[" + (num_robots-1) + "] = " + startingLine[num_robots-1] + ":" + startingPoint[num_robots-1]);
		Log.i(TAG, "Ending[" + (num_robots-1) + "] = " + endingLine[num_robots-1] + ":" + endingPoint[num_robots-1]);
	}
	
	public void sendAssignments() {
		String name = gvh.getName();
		String[] robots = gvh.getParticipants().toArray(new String[0]);
		
		for(int i = 0; i < robots.length; i++) {
			String current = (String) robots[i];
			RobotMessage sendLine = new RobotMessage(current, name, LogicThread.MSG_INFORMLINE, startingLine[i] + ":" + startingPoint[i] + "," + endingLine[i] + ":" + endingPoint[i]);
			Log.d(TAG, i + "| To " + current + ": " + startingLine[i] + ":" + startingPoint[i] + "," + endingLine[i] + ":" + endingPoint[i]);
			gvh.addOutgoingMessage(sendLine);
		}
	}
	
	public itemPosition getLinePoint(int line_num, int point) {
		// If the requested point isn't on the line, return null 
		if(!linePointExists(line_num, point)) {
			Log.e(ERR, TAG + ": requested point " + point + " on line " + line_num + " which doesn't exist!");
			return null;
		}
		return segments[line_num].getPoint(point);
	}
	
	public itemPosition getNextLinePoint(int line_num, int point) {
		// Return the position of the next point along the line
		if(linePointExists(line_num,point+1)) {
			// If the next point on this line exists
			return segments[line_num].getPoint(point+1);
		} else if(linePointExists(line_num+1,0)) {
			// Otherwise, if the first point on the next line exists
			return segments[line_num+1].getPoint(0);
		}
		// Otherwise, we're at the end of the drawing, return null
		return null;
	}
	
	public int getNextLineNum(int line_num, int point) {
		if(linePointExists(line_num,point+1)) {
			// If the next point on this line exists
			return line_num;
		} else if(linePointExists(line_num+1,0)) {
			// Otherwise, if the first point on the next line exists
			return line_num + 1;
		}
		return -1;
	}
	
	public int getNextPointNum(int line_num, int point) {
		if(linePointExists(line_num,point+1)) {
			// If the next point on this line exists
			return point+1;
		} else if(linePointExists(line_num+1,0)) {
			// Otherwise, if the first point on the next line exists
			return 0;
		}
		return -1;
	}
	
	public boolean linePointExists(int line_num, int point) {
		if(line_num < segments.length) {
			return (segments[line_num].getLength() >= point);
		} else {
			return false;
		}
	}
	
	public int getNumIntersections() {
		return num_intersections;
	}
	
	public boolean isIntersection(int line, int point) {
		if(linePointExists(line, point)) {
			return isIntersection(getLinePoint(line,point));
		} else {
			return false;
		}
	}
	
	public boolean isIntersection(itemPosition pos) {
		return pos.getName().matches("[0-9]+-[0-9]+-[0-9]+-[0-9]+-[0-9]+");
	}
	
	public int intersectionNumber(int line, int point) {
		return intersectionNumber(getLinePoint(line,point));
	}
	
	public int intersectionNumber(itemPosition pos) {
		if(isIntersection(pos)) {
			return Integer.parseInt(pos.getName().split("-")[4]);
		} else {
			return -1;
		}
	}
	
	public int lineColor(int line) {
		return segments[line].getColor();
	}
	
	public int lineLen(int line_num) {
		return segments[line_num].getLength();
	}
}