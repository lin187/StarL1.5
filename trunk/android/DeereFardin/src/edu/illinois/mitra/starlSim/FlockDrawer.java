package edu.illinois.mitra.starlSim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class FlockDrawer extends Drawer
{
	@Override
	public void draw(LogicThread lt, Graphics2D g)
	{
		DeereFlockingWithDetours o = (DeereFlockingWithDetours)lt;
		
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
			if (last != null)
				g.drawLine(last.x, last.y, p.x, p.y);
			
			drawOval(g, p);
			
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
	}

	private void drawOval(Graphics2D g, WayPoint p) 
	{
		final int POINT_DRAW_SIZE = 10;
		
		g.fillOval((int)(p.x - POINT_DRAW_SIZE), (int)(p.y - POINT_DRAW_SIZE), 
				2 * POINT_DRAW_SIZE + 1, 2 * POINT_DRAW_SIZE + 1);
	}
}
