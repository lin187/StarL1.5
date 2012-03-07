package edu.illinois.linemutex;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.TreeMap;



@SuppressWarnings("serial")
public class DrawPanel extends ZoomablePanel
{
	DrawFrame parent = null;
	
	ArrayList <Point> endPoints = new ArrayList <Point>();
	ArrayList <String> colors = new ArrayList <String>();
	
	TreeMap <String, Color> colorMap = new TreeMap <String, Color>();
	
	 ArrayList <LineOutputData> result = null;
	 
	 private Point mousePoint = null;

	 public DrawPanel(DrawFrame parent)
	 {
		 this.parent = parent;
		 
		 // populate colorMap
		 
		 colorMap.put("red", Color.red);
		 colorMap.put("green", Color.green);
		 colorMap.put("blue", Color.blue);
		 colorMap.put("orange", Color.orange);
		 colorMap.put("pink", Color.pink);
		 colorMap.put("gray", Color.gray);
				
		 
		 /*colors.add(Color.green);
		 colors.add(Color.green);
		 colors.add(Color.green);
		 
		 endPoints.add(new Point(400, 400));
		 endPoints.add(new Point(500, 400));
		 endPoints.add(new Point(400, 420));
		 
		 result = compute(makeLineInputData(), SPACING, ROBOT_RADIUS);*/
	 }
	 
	@Override
	protected void draw(Graphics2D g)
	{
		// draw all the lines
		Ellipse2D.Double endPoint = new Ellipse2D.Double(0, 0, 10, 10);
		g.setStroke(new BasicStroke(3));
		
		// draw first point
		if (endPoints.size() > 0)
		{
			Point p = endPoints.get(0);
			
			g.setColor(Color.black);
			endPoint.x = p.x - endPoint.width / 2;
			endPoint.y = p.y - endPoint.width / 2;
			
			g.fill(endPoint);
		}
		
		for (int index = 1; index < endPoints.size(); ++index)
		{
			Point cur = endPoints.get(index);
			Point prev = endPoints.get(index - 1);
			
			// draw a line to the previous point
			g.setColor(colorMap.get(colors.get(index)));
			g.drawLine(cur.x,cur.y, prev.x, prev.y);
			
			// draw endpoints
			g.setColor(Color.black);
			
			for (Point p : new Point[]{cur, prev})
			{
				endPoint.x = p.x - endPoint.width / 2;
				endPoint.y = p.y - endPoint.width / 2;
				
				g.fill(endPoint);
			}	
		}
		
		// draw output result
		if (result != null)
		{
			Ellipse2D.Double anchor = new Ellipse2D.Double(0, 0, 12, 12);
			g.setStroke(new BasicStroke(1));
			g.setFont(new Font("Tahoma", Font.PLAIN, 20));
			
			for (LineOutputData out : result)
			{
				for (int i = 0; i < out.points.size(); ++i)
				{
					DoublePoint p = out.points.get(i);
					
					Color c = colorMap.get(out.colors.get(i));
					
					anchor.x = p.x - anchor.width / 2;
					anchor.y = p.y - anchor.width / 2;
					
					g.setColor(c);
					g.draw(anchor);
					
					// mutex
					int mutex = out.mutexId.get(i);
					int DEFAULT_MUTEX_ID = 0;
					
					if (mutex != DEFAULT_MUTEX_ID)
					{
						g.setColor(Color.darkGray);
						g.drawString("" + mutex, (int)p.x+7, (int)p.y + 5);
					}
				}
			}
		}

		if (mousePoint != null)
		{
			// draw a circle around the mouse with radius = 2 * robotRadius
			double radius = parent.getRobotRadius();
			g.setStroke(new BasicStroke(1));
			g.setColor(Color.red.darker());
			
			g.draw(new Ellipse2D.Double(mousePoint.x - radius, mousePoint.y - radius, 2*radius, 2*radius));
		}
	}
	
	protected void mousePressedAt(Point p, MouseEvent e) 
	{		
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			// add to set
				
			String[] allColors = 
			{
				"red",
				"green",
				"blue",
				"orange",
				"pink",
				"gray",
			};
			
			String c = allColors[(int)(Math.random() * allColors.length)];
			
			endPoints.add(p);
			colors.add(c);
		}
		else if (e.getButton() == MouseEvent.BUTTON3)
		{
			if (endPoints.size() > 0)
			{
				endPoints.remove(endPoints.size() - 1);
				colors.remove(colors.size() - 1);
			}
		}
			
		compute();
	}
	

	protected void mouseExitedAt(Point realPoint, MouseEvent e)
	{
		mousePoint = null;
		repaint();
	}
	
	protected void mouseMovedAt(Point realPoint, MouseEvent e) 
	{
		mousePoint = realPoint;
		repaint();
	}
	
	private ArrayList<LineInputData> makeLineInputData()
	{
		ArrayList <LineInputData> rv = new ArrayList <LineInputData> ();
		
		if (endPoints.size() > 0)
		{
			Point lastPoint = endPoints.get(0);
			
			for (int i = 1; i < endPoints.size(); ++i)
			{
				Point p = endPoints.get(i);
				String c = colors.get(i);
				
				rv.add(new LineInputData(new DoublePoint((double)lastPoint.x, (double)lastPoint.y), new DoublePoint((double)p.x, (double)p.y), c));
				
				lastPoint = p;
			}
		}
		
		return rv;
	}

	

	public void clear()
	{
		result = null;
		
		endPoints.clear();
		colors.clear();
		
		repaint();
	}
	
	public void compute()
	{
		result = LineMutexCompute.compute(makeLineInputData(), parent.getWaypointSpacing(), parent.getRobotRadius());
		
		repaint();
	}
}
