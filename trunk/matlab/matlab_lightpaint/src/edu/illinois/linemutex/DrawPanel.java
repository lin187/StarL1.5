package edu.illinois.linemutex;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
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
	String ghostLineColor = "ghost";
	
	 ArrayList <LineOutput> result = null;
	 
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
		 
		 
		 // small test for intersection correctness
		 /*Line2D.Double one = new Line2D.Double(0, 0, 500, 500);
		 Line2D.Double two = new Line2D.Double(500, 500, 1000, 1000); 
		 
		 System.out.println("intersection: " + Geometry.segSegIntersection(two, one));
		 System.exit(1);*/
				
		/* int[][] testLines = 
		 {
//				 {2238,	2020,	2222,	2312},
//				 {2222,	2312,	 983,	2296},
//				  {983,	2296,	 993,	2025},
//				  {993,	2025,	 633,	2015},
//				  {633,	2015,	 225,	1729},
//				  {225,	1729,	 231,	1098},
//				  {231,	1098,	 506,	 733},
//				  {506,	 733,	2699,	 733},
//				 {2699,	 733,	2964,	1024},
//				 {2964,	1024,	2975,	1178},
//				 {2975,	1178,	2969,	1787},
//				 {2969,	1787,	2630,	2004},
//				 {2630,	2004,	 633,	2015},
//				  {633,	2015,	 744,	2603},
//				  {744,	2603,	 744,	2608},
//				  {744,	2608,	1295,	2613},
//				 {1295,	2613,	1417,	2613},
//				 {1417,	2613,	1417,	2322},
//				 {1417,	2322,	1555,	2306},
//				 {1555,	2306,	1539,	2767},
//				 {1539,	2767,	1290,	2767},
//				 {1290,	2767,	1295,	2624},
				 
				 
				 // simple test
				 {0, 0, 1000, 1000},
				 {1000, 1000, 0, 1000},
				 {0, 1000, 1000, 0},
		 };
		 
		 for (int i = 0; i < testLines.length; ++i)
		 {
			 Point a = new Point(testLines[i][0], testLines[i][1]);
			 Point b = new Point(testLines[i][2], testLines[i][3]);
			 
			 colors.add("green");
			 endPoints.add(a);
			 
			 if (i == testLines.length - 1)
			 {
				 endPoints.add(b);
				 colors.add("green");
			 }
		 }//*/
	 }
	 
	@Override
	protected void draw(Graphics2D g)
	{		
		for (Point p : endPoints)
		{
			Ellipse2D.Double e = new Ellipse2D.Double(0, 0, 7, 7);
			g.setColor(Color.GRAY);
			
			e.x = p.x - e.width/2;
			e.y = p.y - e.height/2;
			
			g.fill(e);
		}
		
		// draw output result
		if (result != null)
		{
			for (LineOutput out : result)
			{
				boolean isFirst = true;
				for (int i = 1; i < out.waypoints.size(); ++i)
				{
					WayPoint wpPrev = out.waypoints.get(i-1);
					
					if (isFirst)
					{
						isFirst = false;
						
						// draw the first waypoint
						drawWaypoint(g,wpPrev);
					}
					
					WayPoint wp = out.waypoints.get(i);
					drawWaypoint(g,wp);
					
					// draw the connecting line
					String color = wp.color;
					
					if (color.equals(ghostLineColor))
					{
						g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
						        BasicStroke.JOIN_MITER, 10.0f, new float[] {2.0f}, 0.0f));
						
						g.setColor(Color.black);
					}
					else
					{
						g.setStroke(new BasicStroke(2));
						g.setColor(colorMap.get(color));
					}
					
					g.drawLine(wpPrev.point.x, wpPrev.point.y, wp.point.x, wp.point.y);
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
	
	private void drawWaypoint(Graphics2D g, WayPoint wp)
	{
		Ellipse2D.Double anchor = new Ellipse2D.Double(0, 0, 20, 20);
		g.setStroke(new BasicStroke(1));
		
		String color = wp.color;
		
		if (color.equals(ghostLineColor))
			g.setColor(Color.black);
		else
			g.setColor(colorMap.get(color));
		
		anchor.x = wp.point.x - anchor.width / 2;
		anchor.y = wp.point.y - anchor.height / 2;
		
		g.draw(anchor);
		
		// mutex
		g.setFont(new Font("Tahoma", Font.PLAIN, 24));
		
		int mutex = wp.mutexId;
		
		if (mutex != -1)
		{
			g.setColor(Color.darkGray);
			
			g.drawString("" + mutex, wp.point.x + 10, wp.point.y + 3);
		}
		
		g.setFont(new Font("Tahoma", Font.BOLD, 10));
		
		// start / end
		if (wp.start)
		{
			g.setColor(Color.black);
			
			g.drawString("S", wp.point.x - 8, wp.point.y+4);
		}
		
		if (wp.end)
		{
			g.setColor(Color.black);
			
			g.drawString("E", wp.point.x + 2, wp.point.y+4);
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
	
	private ArrayList<LineInput> makeLineInputData()
	{
		ArrayList <LineInput> rv = new ArrayList <LineInput> ();
		
		if (endPoints.size() > 0)
		{
			Point lastPoint = endPoints.get(0);
			
			for (int i = 1; i < endPoints.size(); ++i)
			{
				Point p = endPoints.get(i);
				String c = colors.get(i);
				
				rv.add(new LineInput(new Point(lastPoint.x, lastPoint.y), new Point(p.x, p.y), c));
			
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
		Rectangle world = new Rectangle(100, 100, 1000, 1000);
		
		result = LineMutexCompute.compute(makeLineInputData(), parent.getWaypointSpacing(), parent.getRobotRadius(),
				parent.getNumRobots(), parent.getMinTravelDistance(), world, ghostLineColor);
		
		/*System.out.println("Result:");
		
		for (LineOutput out : result)
		{
			System.out.print("Waypoints: ");
			
			for (WayPoint wp : out.waypoints)
				System.out.println("Pt = " + wp.point + ", mutex=" + wp.mutexId + ", color = " + wp.color + ", start = " + wp.start + ", end = " + wp.end);
		
			System.out.println();
		}*/
		
		repaint();
	}
}
