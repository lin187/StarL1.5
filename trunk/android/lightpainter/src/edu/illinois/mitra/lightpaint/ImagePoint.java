package edu.illinois.mitra.lightpaint;

import edu.illinois.mitra.starl.objects.ItemPosition;

public class ImagePoint implements Comparable<ImagePoint> {
	ItemPosition pos;
	boolean start,end;
	int robot,point,mutex;
	String color;

	public ImagePoint(ItemPosition wpt) throws ImproperWaypointException {
		pos = wpt;
		String[] parts = wpt.getName().split(":");
		if(parts.length != 6) {
			throw new ImproperWaypointException();
		}
		robot = Integer.parseInt(parts[0]);
		point = Integer.parseInt(parts[1]);
		color = parts[2];
		mutex = Integer.parseInt(parts[3]);
		start = parts[4].equals("1");
		end = parts[5].equals("1");
	}

	public int compareTo(ImagePoint arg0) {
		return this.point-arg0.point;
	}
	
	public ItemPosition getPos() {
		return pos;
	}

	public boolean isStart() {
		return start;
	}

	public boolean isEnd() {
		return end;
	}

	public int getRobot() {
		return robot;
	}

	public int getPoint() {
		return point;
	}

	public int getMutex() {
		return mutex;
	}

	public String getColor() {
		return color;
	}
}
