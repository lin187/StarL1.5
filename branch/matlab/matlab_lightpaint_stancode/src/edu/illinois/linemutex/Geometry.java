package edu.illinois.linemutex;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;


public class Geometry
{
	public static double SMALL = 0.0000001;

	public static double getLength(Line2D.Double line)
	{
		double dx = line.x1 - line.x2;
		double dy = line.y1 - line.y2;
		
		return (float)Math.sqrt(dx*dx + dy*dy);
	}

	/**
	 * up is positive infinity, down is negative infinity, NAN is if start == end
	 * @param line
	 * @return
	 */
	public static double getSlope(Line2D.Double line)
	{
		double rv = Double.NaN;
		double dx = line.x2 - line.x1;
		double dy = line.y2 - line.y1;
		
		if (Math.abs(dx) < SMALL)
		{
			// dx is zero
			
			if (dy > SMALL)
				rv = Double.POSITIVE_INFINITY;
			else if (dy < -SMALL)
				rv = Double.NEGATIVE_INFINITY;
		}
		else
		{
			// dx is nonzero
			rv = dy / dx;
		}
		
		return rv;
	}

	public static double getAngle(Line2D.Double line)
	{
		double dx = line.x2 - line.x1;
		double dy = line.y2 - line.y1;
		
		return Math.atan2(dy, dx);
	}

	public static Point2D.Double projectPoint(Point2D.Double orgin, double magnitude, double angle)
	{
		double x = magnitude * Math.cos(angle);
		double y = magnitude * Math.sin(angle);
		
		return new Point2D.Double(orgin.x + x, orgin.y + y);
	}
	
	public static double segSegDist(Line2D.Double one, Line2D.Double two)
	{
		double rv = Double.MAX_VALUE;
		
		if (one.intersectsLine(two))
		{
			rv = 0;
		}
		else
		{
			rv = Math.min(rv, one.ptSegDist(two.getP1()));
			rv = Math.min(rv, one.ptSegDist(two.getP2()));
			
			rv = Math.min(rv, two.ptSegDist(one.getP1()));
			rv = Math.min(rv, two.ptSegDist(one.getP2()));
		}
		
		return rv;
	}
	
}
