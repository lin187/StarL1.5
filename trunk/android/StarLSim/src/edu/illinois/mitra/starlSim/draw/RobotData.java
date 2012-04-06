package edu.illinois.mitra.starlSim.draw;

import java.awt.Color;

public class RobotData
{
	public String name;
	public int x;
	public int y;
	public double degrees;

	// optional
	public int radius;
	public Color c;
	
	public RobotData(String name, int x, int y, double degrees)
	{
		this.name = name;
		this.x = x;
		this.y = y;
		this.degrees = degrees;
	}
}
