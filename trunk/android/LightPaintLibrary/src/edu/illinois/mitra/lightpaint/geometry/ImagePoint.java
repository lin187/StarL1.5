package edu.illinois.mitra.lightpaint.geometry;

import edu.illinois.mitra.starl.objects.ItemPosition;

public class ImagePoint {
	protected final double x;
	protected final double y;
	protected final int color;

	public ImagePoint(double x, double y, int color) {
		this.color = color;
		this.x = x;
		this.y = y;
	}

	public ImagePoint(double x, double y) {
		this(x, y, 0);
	}

	public int getColor() {
		return color;
	}

	public static ImagePoint fromItemPosition(ItemPosition ip) {
		return new ImagePoint(ip.x, ip.y, ip.getAngle());
	}

	public static ImagePoint fromString(String s) {
		String[] parts = s.split("[\\s,]+");
		if(parts.length < 2)
			throw new IllegalArgumentException("Can't parse an ImagePoint from " + s);
		return new ImagePoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public ImagePoint subtract(ImagePoint other) {
		return new ImagePoint(x - other.x, y - other.y, color);
	}

	public ImagePoint add(ImagePoint other) {
		return new ImagePoint(x + other.x, y + other.y, color);
	}

	public double distanceTo(ImagePoint other) {
		return Math.hypot(x - other.x, y - other.y);
	}

	public ImagePoint scale(double scale) {
		return new ImagePoint(x * scale, y * scale, color);
	}

	/**
	 * Treats two ImagePoints as 2-vectors and computes the dot product
	 * 
	 * @param other
	 * @return
	 */
	public double dot(ImagePoint other) {
		return x * other.x + y * other.y;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")[" + color + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		ImagePoint other = (ImagePoint) obj;
		if(Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if(Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}
}
