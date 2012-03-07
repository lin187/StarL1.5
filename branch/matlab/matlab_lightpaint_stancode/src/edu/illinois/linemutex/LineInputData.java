package edu.illinois.linemutex;


public class LineInputData
{
	public LineInputData(DoublePoint s, DoublePoint e, String color)
	{
		start = s;
		end = e;
		this.color = color;
	}
	
	DoublePoint start, end;
	String color;
};
