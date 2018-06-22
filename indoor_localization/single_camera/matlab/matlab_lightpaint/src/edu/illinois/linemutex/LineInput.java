package edu.illinois.linemutex;

import java.awt.Point;


public class LineInput
{
	public LineInput(Point s, Point e, String color)
	{
		start = new Point(s.x, s.y);
		end = new Point(e.x, e.y);
		this.color = color;
	}
	
	Point start, end;
	String color;
};
