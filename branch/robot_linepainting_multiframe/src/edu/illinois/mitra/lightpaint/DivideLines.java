package edu.illinois.mitra.lightpaint;

import android.util.Log;
import edu.illinois.mitra.LogicThread;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.Objects.itemPosition;
import edu.illinois.mitra.Objects.positionList;
import edu.illinois.mitra.comms.RobotMessage;

public class DivideLines {
	private static final String TAG = "DivideLines";
	private static final String ERR = "Critical Error";
	
	private static int MIN_ROBOT_TRAVEL_DIST = 1500;
	
	private int num_frames = 0;
	private int num_robots = 0;
	private int spacing = 0;
	
	
	private int[] num_operating_robots;
	
	private int[][] startingLine;
	private int[][] endingLine;
	
	private int[][] startingPoint;
	private int[][] endingPoint;
	
	private ImageFrame[] frames;
	
	private globalVarHolder gvh;
	
	public DivideLines(globalVarHolder gvh) {
		this.gvh = gvh;
		num_robots = gvh.getParticipants().size();
		
		// Extract the number of frames
		num_frames = gvh.getWaypointPosition("NUM_FRAMES").getAngle();
		
		// Extract the point spacing
		spacing = gvh.getWaypointPosition("SPACING").getAngle();
		
		// Set up the integer arrays of starting and ending points
		startingLine = new int[num_robots][num_frames];
		endingLine = new int[num_robots][num_frames];
		startingPoint = new int[num_robots][num_frames];
		endingPoint = new int[num_robots][num_frames];
		
		frames = new ImageFrame[num_frames];
		
		num_operating_robots = new int[num_frames];
	}
	
	public void processWaypoints() {
		positionList wpts = gvh.getWaypointPositions();
		Log.i(TAG, "Found " + wpts.getNumPositions() + " waypoints");
		
		for(int i = 0; i < num_frames; i++) {
			frames[i] = new ImageFrame(i,wpts.getPosition("NUM_LINES_IN_FRAME_" + i).getAngle(),wpts);
		}
	}
	
	public void assignLineSegments() {
		for(int i = 0; i < num_frames; i++) {
			Log.e(TAG, "Frame " + i);
			assignLineSegments(i);
		}
	}
	
	private void assignLineSegments(int frame) {		
		int total_distance = frames[frame].getTotalDistance();
		int num_lines = frames[frame].getNumLines();
		LineSegment[] lines = frames[frame].getLines();
		
		// Determine the number of robots that are needed
		int real_dist = spacing*total_distance;
		num_operating_robots[frame] = 1;
		while(num_operating_robots[frame] < num_robots && (real_dist/num_operating_robots[frame]) > MIN_ROBOT_TRAVEL_DIST) {
			num_operating_robots[frame] ++;
		}		
		
		// Determine the target distance for each robot
		double target = Math.floor(total_distance/num_operating_robots[frame]);
		
		Log.d(TAG, "Assigning line segments for frame " + frame);
		Log.d(TAG, "Total distance: " + total_distance);
		Log.d(TAG, "Total lines: " + num_lines);
		Log.d(TAG, "Robots: " + num_operating_robots[frame] + " of " + num_robots);
		Log.d(TAG, "Target distance per robot: " + target);

		// Assign segments to each robot until the average is exceeded
		int current_line = 0;
		int current_point = 0;
		startingLine[0][frame] = 0;
		startingPoint[0][frame] = 0;
		
		Log.i(TAG, "Starting at line " + current_line + " of length " + lines[current_line].getLength());
				
		for(int i = 0; i < num_operating_robots[frame]-1; i++) {
			int current_dist = 0;
			Log.i(TAG, "Starting[" + i + "] = " + startingLine[i][frame] + ":" + startingPoint[i][frame]);
			while(continueDividing(i, frame, current_line, current_point, num_lines, current_dist, target, lines)) {
				// If we've walked to the end of this segment, go to the next one
				if(lines[current_line].getLength()-1 == current_point) {
					current_line ++;
					current_point = 0;				
				} else {
					current_point ++;
				}
				current_dist ++;
			}
			endingLine[i][frame] = current_line;
			endingPoint[i][frame] = current_point;
			if(num_operating_robots[frame] > 1) {
				startingLine[i+1][frame] = current_line;
				startingPoint[i+1][frame] = current_point;
			}
			Log.i(TAG, "Ending[" + i + "] = " + endingLine[i][frame] + ":" + endingPoint[i][frame]);
			Log.i(TAG, "Distance covered: " + distanceCovered(lines,startingLine[i][frame],startingPoint[i][frame],endingLine[i][frame],endingPoint[i][frame]));
		}
		endingLine[num_operating_robots[frame]-1][frame] = num_lines + 1;
		endingPoint[num_operating_robots[frame]-1][frame] = 0;
	
		
		// If the last robot has less than the minimum travel distance, remove it from the execution
		int lastbot = num_operating_robots[frame]-1;
		int lastdst = distanceCovered(lines,startingLine[lastbot][frame],startingPoint[lastbot][frame],endingLine[lastbot][frame],endingPoint[lastbot][frame]);
		
		Log.i(TAG, "Starting[" + (lastbot) + "] = " + startingLine[lastbot][frame] + ":" + startingPoint[lastbot][frame]);
		Log.i(TAG, "Ending[" + (lastbot) + "] = " + endingLine[lastbot][frame] + ":" + endingPoint[lastbot][frame]);
		Log.i(TAG, "Distance covered: " + lastdst);
		
		if(lastbot > 0 && lastdst < MIN_ROBOT_TRAVEL_DIST) {
			Log.e(TAG, "The last robot only covered " + lastdst + " and was removed from execution!");
			// Eliminate the last robot from the execution
			endingLine[lastbot-1][frame] = endingLine[lastbot][frame];
			endingPoint[lastbot-1][frame] = endingPoint[lastbot][frame];
			num_operating_robots[frame] --;
		}
		
		// If not all of the robots were used, fill in the rest of the arrays with null values
		for(int i = num_operating_robots[frame]; i < num_robots; i++) {
			startingLine[i][frame] = -1;
			endingLine[i][frame] = -1;
			startingPoint[i][frame] = -1;
			endingPoint[i][frame] = -1;
		}		

	}
	
	// Return true if the target distance has been met AND the current point isn't an intersection AND we're still 
	// on a valid line AND there's no start or stop point within RADIUS
	private boolean continueDividing(int i, int frame, int current_line, int current_point, int num_lines, int current_dist, double target, LineSegment[] lines) {
		final int TOO_CLOSE_RADIUS = 2*spacing;
		if(current_line >= num_lines) { 
			return false;
		}
		boolean retval = (current_dist <= target || lines[current_line].isIntersectionPoint(current_point));

		itemPosition cur = lines[current_line].getPoint(current_point);
		for(; i > 0; i--) {
			itemPosition otherStart = lines[startingLine[i][frame]].getPoint(startingPoint[i][frame]);
			itemPosition otherEnd = lines[endingLine[i][frame]].getPoint(endingPoint[i][frame]);
			retval |= (cur.distanceTo(otherStart) < TOO_CLOSE_RADIUS);
			retval |= (cur.distanceTo(otherEnd) < TOO_CLOSE_RADIUS);
		}
	
		return retval;
	}

	
	// Return the total distance covered by an assignment
	private int distanceCovered(LineSegment[] lines, int ln, int pt, int endLine, int endPt) {
		int dist = 0;
		
		while(!(pt == endPt && ln == endLine)) {
			if(pt < lines[ln].getLength()) {
				pt ++;
				dist ++;
			} else if(ln < lines.length-1) {
				pt = 0;
				ln ++;
			} else {
				break;
			}
		}
		return spacing*dist;
	}
	
	public void sendAssignments(int frame) {
		String name = gvh.getName();
		String[] robots = gvh.getParticipants().toArray(new String[0]);
		Log.d(TAG, "Sending for frame " + frame);
		for(int i = 0; i < robots.length; i++) {
			int closestIdx = closestRobot(robots, frame, startingLine[i][frame], startingPoint[i][frame]);
			String current = robots[closestIdx];

			RobotMessage sendLine = new RobotMessage(current, name, LogicThread.MSG_INFORMLINE, startingLine[i][frame] + ":" + startingPoint[i][frame] + "," + endingLine[i][frame] + ":" + endingPoint[i][frame]);
			
			Log.d(TAG, i + "| To " + current + ": " + startingLine[i][frame] + ":" + startingPoint[i][frame] + "," + endingLine[i][frame] + ":" + endingPoint[i][frame]);
			
			gvh.addOutgoingMessage(sendLine);
			
			robots[closestIdx] = "";
		}
	}
	
	// Given an array of robot names and a location, returns the index of the closest robot to that location
	private int closestRobot(String[] robots, int frame, int startLine, int startPoint) {
		int minDist = 9999999;
		int minidx = -1;
		int dist = 0;
		itemPosition start = null;
		
		// If assignment isn't "you're not participating", fetch the location of the starting point
		if(startLine >= 0 && startPoint >= 0) {
			start = frames[frame].getLinePoint(startLine, startPoint);
		}
		
		for(int i = 0; i < robots.length; i ++) {
			if(!robots[i].equals("")) {
				// If the assignment is "you're not participating", return the first non-empty robot
				if(start == null) {
					return i;
				}
				dist = gvh.getPosition(robots[i]).distanceTo(start);
				if(dist < minDist) {
					minDist = dist;
					minidx = i;
				}
			}
		}
		return minidx;
	}
	
	public ImageFrame getFrame(int frame) {
		return frames[frame];
	}
	public int getNumFrames() {
		return num_frames;
	}
}