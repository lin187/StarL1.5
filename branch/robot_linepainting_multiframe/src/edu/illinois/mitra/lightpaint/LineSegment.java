package edu.illinois.mitra.lightpaint;

import android.util.Log;
import edu.illinois.mitra.Objects.common;
import edu.illinois.mitra.Objects.itemPosition;

public class LineSegment implements Comparable<LineSegment> {
	private int ID;
	private int color = 0;
	private int length;
	private itemPosition[] segments;
	
	public LineSegment(int iD,  int length) {
		super();
		ID = iD;
		this.length = length;
		segments = new itemPosition[length];
	}
	
	public void addPostion(itemPosition point) {
		segments[common.partsToInts(point.getName(), "-")[2]] = point;
	}
	
	public itemPosition getPoint(int point) {
		if(point >= 0 && point <= length) {
			Log.d("TEST", "Requested " + point + " out of " + segments.length);
			return segments[point];
		} else {
			return null;
		}
	}
	
	public boolean isIntersectionPoint(int point) {
		return segments[point].getName().matches("[0-9]+-[0-9]+-[0-9]+-[0-9]+");
	}

	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int compareTo(LineSegment another) {
		return another.getID() - ID;
	}
}
