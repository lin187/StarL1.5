package edu.illinois.mitra.starlSim.draw;

import java.awt.Color;

import edu.illinois.mitra.starl.objects.ObstacleList;

public class RobotData
{
	public String name;
	public int x;
	public int y;
	public double degrees;
	public long time;

	// optional
	public int radius;
	public Color c;
	public ObstacleList world;
	public int type;
	public boolean leftbump;
	public boolean rightbump;
	
	public RobotData(String name, int x, int y, double degrees)
	{
		this.name = name;
		this.x = x;
		this.y = y;
		this.degrees = degrees;
	}
	
	public RobotData(String name, int x, int y, double degrees, Color color) {
		this(name, x, y, degrees);
		this.c = color;
	}
	
	public RobotData(String name, int x, int y, double degrees, Color color, ObstacleList world) {
		this(name, x, y, degrees, color);
		this.world = world;
		
	}
	
	public RobotData(String name, int x, int y, double degrees, Color color, ObstacleList world, boolean leftbump, boolean rightbump) {
		this(name, x, y, degrees, color);
		this.world = world;
		this.leftbump = leftbump;
		this.rightbump = rightbump;
	}
	
	public RobotData(String name, int x, int y, double degrees, long t) {
		this(name, x, y, degrees);
		this.time = t;
	}
	
	public RobotData(String name, int x, int y, double degrees, long t, Color color) {
		this(name, x, y, degrees, t);
		this.c = color;
	}
	
	public RobotData(String name, int x, int y, double degrees, long t, Color color, ObstacleList world) {
		this(name, x, y, degrees, t, color);
		this.world = world;
	}
	
	public RobotData(String name, int x, int y, double degrees, long t, Color color, ObstacleList world, boolean leftbump, boolean rightbump) {
		this(name, x, y, degrees, t, color);
		this.world = world;
		this.leftbump = leftbump;
		this.rightbump = rightbump;
	}
}
