package edu.illinois.mitra.lightpaint.geometry;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class ImageEdge {
	protected final ImagePoint a;
	protected final ImagePoint b;
	protected final double length;
	protected final Set<ImagePoint> pointSet = new HashSet<ImagePoint>();
	protected final double dX;
	protected final double dY;

	public ImageEdge(ImagePoint a, ImagePoint b) {
		if(a == null || b == null)
			throw new IllegalArgumentException("Can not have null point ends!");
		this.a = a;
		this.b = b;
		length = a.distanceTo(b);
		pointSet.add(a);
		pointSet.add(b);
		dX = a.getX() - b.getX();
		dY = a.getY() - b.getY();
	}
	
	public ImagePoint getStart() {
		return a;
	}

	public ImagePoint getEnd() {
		return b;
	}
	
	public double getdX() {
		return dX;
	}

	public double getdY() {
		return dY;
	}
	
	public ImagePoint getOtherEnd(ImagePoint end) {
		if(end == a)
			return b;
		else if(end == b)
			return a;
		else
			throw new IllegalArgumentException("That's not a valid point on this edge");
	}
	
	public ImageEdge scale(double scale) {
		return new ImageEdge(a.scale(scale), b.scale(scale));
	}
	
	public ImageEdge translate(ImagePoint offset) {
		return new ImageEdge(a.add(offset), b.add(offset));
	}
	
	public Set<ImagePoint> getEnds() {
		return pointSet;
	}
	
	public double getLength() {
		return length;
	}
	
	public double distanceTo(ImagePoint p) {
		if(length == 0)
			return a.distanceTo(p);
		
		double t = (p.subtract(a)).dot(b.subtract(a))/(length*length);
		if(t < 0)
			return p.distanceTo(a);
		if(t > 1)
			return p.distanceTo(b);
		ImagePoint projection = a.add(b.subtract(a).scale(t));
		return p.distanceTo(projection);
	}

	public double distanceTo(ImageEdge other) {
		if(intersects(other))
			return 0;
		else
			return Math.min(Math.min(distanceTo(other.a), distanceTo(other.b)), Math.min(other.distanceTo(a), other.distanceTo(b)));
	}
		
	public ImagePoint getIntersection(ImageEdge other) {
		if(a.equals(other.a) || a.equals(other.b))
			return a;
		else if(b.equals(other.a) || b.equals(other.b))
			return b;

		double x4_x3 = other.b.x-other.a.x;
		double y1_y3 = a.y-other.a.y;
		double y4_y3 = other.b.y-other.a.y;
		double x1_x3 = a.x-other.a.x;
		double x2_x1 = b.x-a.x;
		double y2_y1 = b.y-a.y;
		
		double num_a = x4_x3 * y1_y3 - y4_y3 * x1_x3;
		double num_b = x2_x1 * y1_y3 - y2_y1 * x1_x3;
		double den = y4_y3 * x2_x1 - x4_x3 * y2_y1;

		double ua = num_a / den;
		double ub = num_b / den;
		
		double INT_X = a.x+x2_x1*ua;
		double INT_Y = a.y+y2_y1*ua;
		boolean INT_B = (ua >= 0) && (ua <= 1) && (ub >= 0) && (ub <= 1);
		ImagePoint intersection = INT_B ? new ImagePoint(INT_X, INT_Y) : null;
		return intersection;
	}
	
	public boolean intersects(ImageEdge other) {
		return getIntersection(other) != null;
	}
	
	public ImageEdge reversed() {
		return new ImageEdge(b,a);
	}
	
	
	private static final Comparator<ImageEdge> LENGTH_COMPARATOR = new Comparator<ImageEdge>() {
		@Override
		public int compare(ImageEdge o1, ImageEdge o2) {
			return Double.compare(o1.getLength(), o2.getLength());
		}
	};
	
	public static Comparator<ImageEdge> lengthComparator() {
		return LENGTH_COMPARATOR;
	}
	
	@Override
	public String toString() {
		return a + " -> " + b;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		ImageEdge other = (ImageEdge) obj;
		if(a.equals(other.a) && b.equals(other.b))
			return true;
		return false;
	}
}
