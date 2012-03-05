package edu.illinois.mitra.lightpaint;

import java.util.HashSet;
import java.util.Set;

import android.util.Log;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.itemPosition;
import edu.illinois.mitra.starl.objects.positionList;


public class ImageFrame {
	private static final String TAG = "ImageFrame";
	private static final String ERR = "Critical Error";
	
	private LineSegment[] segments;
	private int frame_number = 0;
	private int num_intersections = -1;
	private int num_lines = -1;
	private int total_distance = 0;
	
	public ImageFrame(int frame_number, int num_lines, positionList waypoints) {
		this.frame_number = frame_number;
		this.num_lines = num_lines;
		segments = new LineSegment[num_lines];	
		populate(waypoints);
	}
	
	private void populate(positionList waypoints) {
		// Fetch the number of intersections
		num_intersections = waypoints.getPosition("NUM_INTS_IN_FRAME_" + frame_number).getAngle();	
		
		// First create all line segment objects
		Log.i(TAG,"Creating all LineSegment objects...");
		for(int i = 0; i < num_lines; i++) {
			itemPosition cur = waypoints.getPosition("F" + frame_number + "_L" + i);
			if(cur.getAngle() == -1) {
				segments[i] = new LineSegment(i, cur.getX());
				
				segments[i].setColor(waypoints.getPositionRegex("F" + frame_number + "L" + i + "C_[0-9a-fA-F]{6}").getName().split("_")[1]);
				Log.i(TAG, "Initialized segment " + i);
				total_distance += cur.getX();
			}
		}		
		
		// Fill the array of LineSegments
		Log.i(TAG,"Filling the array of LineSegments");
		for(int i = 0; i < waypoints.getNumPositions(); i ++) {
			itemPosition current = waypoints.getPositionAtIndex(i);
			if(current.getAngle() != -1) {
				int[] vals = common.partsToInts(current.getName(),"-");
				
				if(vals == null) {
					continue;
				}
				
				// If this waypoint belongs to a line in the current frame, add it to that line
				if(current.getX() > 0 && current.getY() > 0 && vals[0] == frame_number && vals[1] >= 0 && vals[1] < num_lines) {
					segments[vals[1]].addPostion(current);
				}
			}
		}
			
		Log.d(TAG, "Created frame with " + num_lines + " lines");
		Log.d(TAG, "Expecting " + num_intersections + " intersections");
	}
	
	//--------------------------------------------------------------------------
	// Functions for getting and checking individual points and their properties
	//--------------------------------------------------------------------------
	public itemPosition getLinePoint(int line_num, int point) {
		// If the requested point isn't on the line, return null 
		if(!linePointExists(line_num, point)) {
			Log.e(ERR, TAG + ": requested point " + point + " on line " + line_num + " which doesn't exist!");
			return null;
		}
		return segments[line_num].getPoint(point);
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
			return (segments[line_num].getLength() > point);
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
		return pos.getName().matches("[0-9]+-[0-9]+-[0-9]+-[0-9]+");
	}
	
	public int intersectionNumber(int line, int point) {
		return intersectionNumber(getLinePoint(line,point));
	}
	public int intersectionNumber(itemPosition pos) {
		if(isIntersection(pos)) {
			return Integer.parseInt(pos.getName().split("-")[3]);
		} else {
			return -1;
		}
	}
	
	
	
	// Intersection numbers within RADIUS
	
	public Set<Integer> intersectionNumbers(itemPosition pos, int RADIUS) {
		HashSet<Integer> retval = new HashSet<Integer>();

		if(!isIntersection(pos)) {
			return retval;
		}
		
		retval.add(intersectionNumber(pos));
		// Find all points within RADIUS of the requested point
		for(int i = 0; i < num_lines; i++) {
			itemPosition[] segs = segments[i].getPositions();
			for(itemPosition pt : segs) {
				if(isIntersection(pt) && pt.distanceTo(pos) <= RADIUS) {
					retval.add(intersectionNumber(pt));
				}
			}
		}	
		return retval;
	}	
	public Set<Integer> intersectionNumbers(int line, int point, int RADIUS) {
		return intersectionNumbers(getLinePoint(line,point), RADIUS);
	}
	public Set<Integer> intersectionNumbers(int[] pos, int RADIUS) {
		return intersectionNumbers(getLinePoint(pos), RADIUS);
	}
	

	public String lineColor(int line) {
		return segments[line].getColor();
	}
	public int lineLen(int line_num) {
		return segments[line_num].getLength();
	}
	public int getTotalDistance() {
		return total_distance;
	}
	public int getNumLines() {
		return num_lines;
	}
	public LineSegment[] getLines() {
		return segments;
	}
	public int getNextLineNum(int line_num, int point) {
		if(linePointExists(line_num,point+1)) {
			// If the next point on this line exists
			return line_num;
		} else if(linePointExists(line_num+1,0)) {
			// Otherwise, if the first point on the next line exists
			return line_num + 1;
		}
		// Otherwise, we're at the edge of the artwork
		return -1;
	}
	public itemPosition getNextLinePoint(int line_num, int point) {
		// Return the position of the next point along the line
		if(linePointExists(line_num,point+1)) {
			return segments[line_num].getPoint(point+1);			
		// Otherwise, if the first point on the next line exists
		} else if(linePointExists(line_num+1,0)) {
			return segments[line_num+1].getPoint(0);
		}
		// Otherwise, we're at the end of the drawing, return null
		return null;
	}

	public itemPosition getNextLinePoint(itemPosition pos) {
		int[] parts = common.partsToInts(pos.getName(), "-");
		return getNextLinePoint(parts[1], parts[2]);
	}

	public itemPosition getNextLinePoint(int[] pos) {
		return getNextLinePoint(pos[0], pos[1]);
	}
	
	public itemPosition getLinePoint(int[] pos) {
		return getLinePoint(pos[0], pos[1]);
	}

	public int getNextLineNum(int[] pos) {
		return getNextLineNum(pos[0], pos[1]);
	}

	public int getNextPointNum(int[] pos) {
		return getNextPointNum(pos[0], pos[1]);
	}

	public boolean isIntersection(int[] curPos) {
		return isIntersection(curPos[0], curPos[1]);
	}

	public int intersectionNumber(int[] curPos) {
		return intersectionNumber(curPos[0], curPos[1]);
	}
	public boolean isGhost(int line) {
		return segments[line].isGhost();
	}
}
