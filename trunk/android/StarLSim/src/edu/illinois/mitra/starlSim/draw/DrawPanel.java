package edu.illinois.mitra.starlSim.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import edu.illinois.mitra.starlSim.main.SimSettings;


@SuppressWarnings("serial")
public class DrawPanel extends ZoomablePanel
{
	private ArrayList <RobotData> data = new ArrayList <RobotData>();
	private long time = 0l;
	private long lastUpdateTime = 0l;
	private long startTime = 0l;
	private int width = 1, height = 1;
	NumberFormat format = new DecimalFormat("0.00");
	int scaleFactor = 0;
	
	private ArrayList<RobotData> _trace = new ArrayList<RobotData>(); // trace of robot positions
	
	public DrawPanel()
	{
		if (SimSettings.DRAW_TRACE) {
			_trace.ensureCapacity(SimSettings.DRAW_TRACE_LENGTH); // allocate enough memory (huge performance gains)
		}
	}

	@Override
	protected void draw(Graphics2D g)
	{
		Point a = new Point(0, 0);
		Point b = new Point(0, 100);
		
		synchronized(this)
		{
			for (RobotData rd : data)
			{
				drawRobot(g,rd,true);
				
				// Draw world bounding box
				g.setColor(Color.gray);
				g.setStroke(new BasicStroke(2));
				g.drawRect(0, 0, width, height);
				
				// Determine scale
				scaleFactor =  (int) toRealCoords(a).distance(toRealCoords(b));
				
				// keep past history of robot positions (very slow)
				// TODO: do this smarter, e.g., is there a way to avoid clearing the previously drawn robot positions for up to SimSettings.DRAW_TRACE_LENGTH calls to draw? this would be significantly more efficient
				if (SimSettings.DRAW_TRACE) {
					_trace.add(rd);
					if (_trace.size() > SimSettings.DRAW_TRACE_LENGTH)
					{
						_trace.remove(0);
						
					}
				}
			}
			
			// draw past history of robot positions
			if (SimSettings.DRAW_TRACE) {
				for (RobotData rd : _trace) {
					drawRobot(g,rd,false);
				}
			}
		}
	}
	
	@Override
	protected void postDraw(Graphics2D g) {
		g.setColor(Color.black);
		g.setFont(new Font("Tahoma", Font.PLAIN, 15) );
		
		g.drawString((time-startTime)/1000 + " kTic   kTic/Sec:" + format.format(((time-startTime)/1000.0)/((lastUpdateTime-startTime)/1000.0)), 5, getSize().height-5);
		
		g.drawString("SCALE: " + scaleFactor, getSize().width - 95, getSize().height-15);
		g.drawLine(getSize().width - 110, getSize().height-10, getSize().width-10, getSize().height-10);
	}
	
	private void drawRobot(Graphics2D g, RobotData rd, boolean drawId)
	{
		g.setStroke(new BasicStroke(2));
		
		if (rd.c != null)
			g.setColor(rd.c);
		else
			g.setColor(Color.black);
		
		int radius = 50; // TODO: is this accurate / why doesn't it use the constants from SimSettings (perhaps with a scaling factor?) ; try to avoid duplicating constants
		
		if (rd.radius != 0)
			radius = rd.radius;
		
		g.drawOval(rd.x - radius, rd.y - radius, radius*2, radius*2);
		
		// draw angle
		double radians = 2 * Math.PI * rd.degrees / 360.0;
		
		Point2D.Double from = new Point2D.Double(rd.x, rd.y);
		Point2D.Double to = Geometry.projectPoint(from, radius, radians);
		
		Line2D.Double l = new Line2D.Double(from, to);
		
		g.draw(l);
		
		if (drawId)
		{
			// write name to the right of the robot
			g.setFont(new Font("Tahoma", Font.PLAIN, 55) ); // TODO: make this configurable in SimSettings
			g.drawString(rd.name, rd.x - 55, rd.y + radius + 50);
		}
	}

	public void updateData(ArrayList <RobotData> data, long time)
	{
		synchronized(this)
		{
			this.time = time;
			this.data = data;
			this.lastUpdateTime = System.currentTimeMillis();
		}
		
		repaint();
	}
	
	public void setWorld(int width, int height, long startTime) {
		synchronized(this)
		{
			this.startTime = startTime;
			this.width = width;
			this.height = height;
		}
	}
}
