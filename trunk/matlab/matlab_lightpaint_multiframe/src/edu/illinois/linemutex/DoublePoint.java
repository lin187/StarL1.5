package edu.illinois.linemutex;

import java.awt.geom.Point2D;


// sad I couldn't just use Point2D.Double directly, matlab doesn't like java.awt
@SuppressWarnings("serial")
public class DoublePoint extends Point2D.Double
{
	public DoublePoint(double x, double y)
	{
		super(x, y);
	}

	// matlab interface hack
	public static DoublePoint makePoint(double x, double y)
	{
		return new DoublePoint(x, y);
	}
}
