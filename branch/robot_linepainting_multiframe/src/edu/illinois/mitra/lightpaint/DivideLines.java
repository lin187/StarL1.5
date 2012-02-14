package edu.illinois.mitra.lightpaint;

import android.util.Log;
import edu.illinois.mitra.LogicThread;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.Objects.positionList;
import edu.illinois.mitra.comms.RobotMessage;

public class DivideLines {
	private static final String TAG = "DivideLines";
	private static final String ERR = "Critical Error";
	
	private static int MIN_ROBOT_TRAVEL_DIST = 1000;
	
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
		
		// TODO: THIS ISN'T DIVIDING LINES PROPERLY!
		
		for(int i = 0; i < num_operating_robots[frame]-1; i++) {
			int current_dist = 0;
			Log.i(TAG, "Starting[" + i + "] = " + startingLine[i][frame] + ":" + startingPoint[i][frame]);
			while(current_line < num_lines && (current_dist <= target || lines[current_line].isIntersectionPoint(current_point))) {
				// If we've walked to the end of this segment, go to the next one
				if(lines[current_line].getLength()-1 == current_point) {
					current_line ++;
					current_point = 0;				
					Log.i(TAG, "   Increment to line " + current_line + " of length " + lines[current_line].getLength());
				} else {
					current_point ++;
				}
				current_dist ++;
				Log.i(TAG, "Current distance: " + current_dist + ". linept: " + current_point);
			}
			if(lines[current_line].isIntersectionPoint(current_point)) {
				Log.e(TAG,"Line divider just placed an endpoint at an intersection!");
			}
			endingLine[i][frame] = current_line;
			endingPoint[i][frame] = current_point;
			if(num_operating_robots[frame] > 1) {
				startingLine[i+1][frame] = current_line;
				startingPoint[i+1][frame] = current_point;
			}
			Log.i(TAG, "Ending[" + i + "] = " + endingLine[i][frame] + ":" + endingPoint[i][frame]);
		}
		endingLine[num_operating_robots[frame]-1][frame] = num_lines + 1;
		endingPoint[num_operating_robots[frame]-1][frame] = 0;
		
		// If not all of the robots were used, fill in the rest of the arrays with null values
		for(int i = num_operating_robots[frame]; i < num_robots; i++) {
			startingLine[i][frame] = -1;
			endingLine[i][frame] = -1;
			startingPoint[i][frame] = -1;
			endingPoint[i][frame] = -1;
		}
		
		Log.i(TAG, "Starting[" + (num_operating_robots[frame]-1) + "] = " + startingLine[num_operating_robots[frame]-1][frame] + ":" + startingPoint[num_operating_robots[frame]-1][frame]);
		Log.i(TAG, "Ending[" + (num_operating_robots[frame]-1) + "] = " + endingLine[num_operating_robots[frame]-1][frame] + ":" + endingPoint[num_operating_robots[frame]-1][frame]);
	}
	
	public void sendAssignments(int frame) {
		String name = gvh.getName();
		String[] robots = gvh.getParticipants().toArray(new String[0]);
		
		for(int i = 0; i < robots.length; i++) {
			String current = (String) robots[i];
			// TODO: Include the frame number in the outgoing message?
			RobotMessage sendLine = new RobotMessage(current, name, LogicThread.MSG_INFORMLINE, startingLine[i][frame] + ":" + startingPoint[i][frame] + "," + endingLine[i][frame] + ":" + endingPoint[i][frame]);
			Log.d(TAG, i + "| To " + current + ": " + startingLine[i][frame] + ":" + startingPoint[i][frame] + "," + endingLine[i][frame] + ":" + endingPoint[i][frame]);
			gvh.addOutgoingMessage(sendLine);
		}
	}
	public ImageFrame getFrame(int frame) {
		return frames[frame];
	}
	public int getNumFrames() {
		return num_frames;
	}
}