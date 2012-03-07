package edu.illinois.mitra.lightpaint;

import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.itemPosition;

public class LineSegment implements Comparable<LineSegment> {
	private int ID;
	private String color = "000000";
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

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
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
	
	public boolean isGhost() {
		return color.equals("000000");
	}
	
	public itemPosition[] getPositions() {
		return segments;
	}
}
