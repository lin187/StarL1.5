package edu.illinois.mitra.starlSim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;

import com.centralnexus.input.Joystick;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class FlockDrawer extends Drawer
{
	private Joystick joy = null;
	
	@Override
	public void draw(LogicThread lt, Graphics2D g)
	{
		DeereFlockingWithDetours o = (DeereFlockingWithDetours)lt;
		
		updateJoystick(g, o);
		
		if (o.movingFrom != null && o.movingTo != null)
		{
			// draw a dotted line showing the robot's current motion path
			 //g.setStroke(new BasicStroke(20.0f, BasicStroke.CAP_BUTT,
			//		 BasicStroke.JOIN_MITER, 10.0f, new float[]{35.0f}, 0.0f));
			
			g.setColor(Color.BLUE);
			g.setStroke(new BasicStroke(10));
			 
			g.drawLine(o.movingFrom.x, o.movingFrom.y, o.movingTo.x, o.movingTo.y);
		}
		
		if (o.robotId == 0)
			g.setColor(Color.BLACK);
		else
			g.setColor(Color.LIGHT_GRAY);
		
		g.setStroke(new BasicStroke(4));
		
		WayPoint last = o.movingTo;
		
		for (WayPoint p : o.currentPath)
		{
			g.setColor(Color.LIGHT_GRAY);
			
			drawOval(g, p);
			
			g.setColor(g.getColor().darker());
			
			if (last != null)
				g.drawLine(last.x, last.y, p.x, p.y);
			
			last = p;
		}
 	
		if (o.robotId == 0)
		{
			g.setColor(Color.orange);
			
			last = null;
			
			for (WayPoint p : o.currentDesiredPath)
			{
				if (last != null)
					g.drawLine(last.x, last.y, p.x, p.y);
				
				drawOval(g, p);
				
				last = p;
			}
		}
			
		g.setColor(Color.red);
		
		last = null;
		
		for (WayPoint p : o.potentialLeaderPath)
		{
			if (last != null)
				g.drawLine(last.x, last.y, p.x, p.y);
			
			drawOval(g, p);
			
			last = p;
		}
	
		//draw the circle for the new waypoint
	    g.fillOval(o.newPoint.x, o.newPoint.y, 100, 100);
	    g.setColor(Color.darkGray);
	    g.drawOval(o.newPoint.x, o.newPoint.y, 100, 100);

	}

	boolean initedJoystick = false;
	
	private void updateJoystick(Graphics2D g, DeereFlockingWithDetours logicThread)
	{
		if (logicThread.robotId == 0)
		{
			// if it's the leader
			
			if (initedJoystick == false)
			{
				initedJoystick = true;
				
				try
				{
					joy = Joystick.createInstance();
				}
				catch (IOException e)
				{
					System.out.println("Error initializing joystick: " + e);
				}
			}
			
			if (joy != null)
			{
				final int JOYSTICK_CIRCLE_RADIUS = 75;
				final double JOYSTICK_POS_MULTIPLIER = 1000;
				
				// poll the joystick
				joy.poll();
				
				ItemPosition robotPos = logicThread.gps.getMyPosition();
				
				double offsetX = JOYSTICK_POS_MULTIPLIER * joy.getX();
				double offsetY = 0.75 * JOYSTICK_POS_MULTIPLIER + JOYSTICK_POS_MULTIPLIER * -joy.getY();
				
				double dx = 0;
				double dy = 0;
				
				int degrees = robotPos.angle;
				double radians = Math.toRadians((double)degrees);
				
				// add the y component
				dx += offsetY * Math.cos(radians);
				dy += offsetY * Math.sin(radians);
				
				// add the x component
				double alpha = (Math.PI / 2.0f) - radians;
				dx -= offsetX * Math.cos(alpha);
				dy -= offsetX * Math.sin(alpha);
								
				double joyX = robotPos.x + dx;
				double joyY = robotPos.y + dy;
				int buttons = joy.getButtons();
				
				// draw joystick circle
				g.setColor(Color.black);
				g.setStroke(new BasicStroke(5));
				
				if (buttons == 0) // no buttons pressed
					g.drawOval((int)(joyX - JOYSTICK_CIRCLE_RADIUS), (int)(joyY - JOYSTICK_CIRCLE_RADIUS), 
						2 * JOYSTICK_CIRCLE_RADIUS + 1, 2 * JOYSTICK_CIRCLE_RADIUS + 1);
				else
					g.fillOval((int)(joyX - JOYSTICK_CIRCLE_RADIUS), (int)(joyY - JOYSTICK_CIRCLE_RADIUS), 
							2 * JOYSTICK_CIRCLE_RADIUS + 1, 2 * JOYSTICK_CIRCLE_RADIUS + 1);
				
				if (buttons != 0)
				{
					logicThread.receivedPointInput((int)joyX, (int)joyY);
				}
			}
		}
		
	}

	private void drawOval(Graphics2D g, WayPoint p) 
	{
		final int POINT_DRAW_SIZE = 10;
		
		g.fillOval((int)(p.x - POINT_DRAW_SIZE), (int)(p.y - POINT_DRAW_SIZE), 
				2 * POINT_DRAW_SIZE + 1, 2 * POINT_DRAW_SIZE + 1);
	}
}