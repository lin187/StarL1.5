package edu.illinois.mitra.starlSim.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;



@SuppressWarnings("serial")
public class DrawPanel extends ZoomablePanel
{
	private ArrayList <RobotData> data = new ArrayList <RobotData>();
	
	public DrawPanel()
	{
		 
	}

	@Override
	protected void draw(Graphics2D g)
	{
		synchronized(this)
		{
			for (RobotData rd : data)
			{
				drawRobot(g,rd);
			}
		}
	}
	
	private void drawRobot(Graphics2D g, RobotData rd)
	{
		g.setStroke(new BasicStroke(2));
		
		if (rd.c != null)
			g.setColor(rd.c);
		else
			g.setColor(Color.black);
		
		int radius = 50;
		
		if (rd.radius != 0)
			radius = rd.radius;
		
		g.drawOval(rd.x - radius / 2, rd.y - radius / 2, radius, radius);
		
		// draw angle
		double radians = 2 * Math.PI * rd.degrees / 360.0;
		
		Point2D.Double from = new Point2D.Double(rd.x, rd.y);
		Point2D.Double to = Geometry.projectPoint(from, 50, radians);
		
		Line2D.Double l = new Line2D.Double(from, to);
		
		g.draw(l);
		
		
		// write name to the right of the robot
		g.setFont(new Font("Tahoma", Font.PLAIN, 35) );
		g.drawString(rd.name, rd.x + radius + 5, rd.y  + 15);
	}

	public void updateData(ArrayList <RobotData> data)
	{
		synchronized(this)
		{
			this.data = data;
		}
		
		repaint();
	}
}
