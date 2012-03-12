package edu.illinois.linemutex;

import java.awt.Point;

public class WayPoint
{
	public WayPoint(Point p, String col)
	{
		point = new Point(p.x, p.y);
		color = col;
	}
	
	public Point point;
	public String color;
	public int mutexId = -1;
	public boolean start = false;
	public boolean end = false;
}
