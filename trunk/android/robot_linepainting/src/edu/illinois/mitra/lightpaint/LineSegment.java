package edu.illinois.mitra.lightpaint;

import edu.illinois.mitra.Objects.itemPosition;

public class LineSegment implements Comparable<LineSegment> {
	private int ID;
	private int color = -1;
	private int length;
	private itemPosition[] segments;
	
	public LineSegment(int iD,  int length) {
		super();
		ID = iD;
		this.length = length;
		segments = new itemPosition[length+1];
	}
	
	public void addPostion(itemPosition point) {
		segments[Integer.parseInt(point.getName().split("-")[2])] = point;
		if(color == -1) {
			color = Integer.parseInt(point.getName().split("-")[1]);
		}
	}
	
	public itemPosition getPoint(int point) {
		if(point >= 0 && point <= length) {
			return segments[point];
		} else {
			return null;
		}
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
