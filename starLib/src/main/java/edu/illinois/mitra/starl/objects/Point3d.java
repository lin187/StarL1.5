package edu.illinois.mitra.starl.objects;

import java.util.HashMap;

import edu.illinois.mitra.starl.exceptions.ItemFormattingException;
import edu.illinois.mitra.starl.interfaces.Traceable;

/**
 * This class represents a point in XYZ plane.
 * Robots or any other points with extra properties should be sub classed from this class
 * @author Yixiao Lin
 * @version 1.0
 */

public class Point3d implements Traceable {
//	private static final String TAG = "Point3d";
//	private static final String ERR = "Critical Error";

	public int x;
	public int y;
	public int z;
	
	public Point3d(){
		x = 0;
		y = 0;
		z = 0;
	}
	
	/**
	 * Construct an Point3d from a name, X, and Y positions, With Z= 0 as default
	 * 
	 * @param name The name of the new position
	 * @param x X position
	 * @param y Y position
	 */
	
	public Point3d(int x, int y) {
		//constructor for calculation temp point
		this.x = x;
		this.y = y;
		this.z = 0;
	}
	
	/**
	 * Construct an Point3d from a name, X, Y and Z positions
	 * 
	 * @param name The name of the new position
	 * @param x X position
	 * @param y Y position
	 * @param z Z position
	 */
	 
	public Point3d(int x, int y, int z) {
		//constructor for calculation temp point
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Construct an Point3d by cloning another
	 * Do not use this method to clone robots, it will only clone name, position and heading
	 * @param other The Point3d to clone
	 */
	
	public Point3d(Point3d other) {
		this(other.x, other.y, other.z);
	}
	
	
	@Override 
	public String toString() {
		return "Point3d" + ": " + x + ", " + y + ", " + z;
	}

	public void setPos(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public <T extends Point3d> void setPos(T other) {
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}
	
	/**
	 * @param other The Point3d to measure against
	 * @return Euclidean distance to Point3d other
	 */
	public int distanceTo(Point3d other) {
		if(other == null) {
			return 0;
		}
		return (int) Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(this.y - other.y, 2) + Math.pow(this.z - other.z, 2));
	}

    public int distanceTo2D(Point3d other) {
        return (int) Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(this.y - other.y, 2));
    }
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}
	
	public String toMessage() {
		return x + "," + y + "," + z;
	}
	
	/**
	 * Construct an Point3d from a received GPS broadcast message
	 * 
	 * Sub class should override this method
	 * 
	 * @param received GPS broadcast received 
	 * @throws ItemFormattingException
	 * 
	 */

	public Point3d(String received) throws ItemFormattingException {
		throw new ItemFormattingException("No implmentation provided, must override in subclass");
	}

	@Override
	public HashMap<String, Object> getXML() {
		HashMap<String, Object> retval = new HashMap<String,Object>();
		retval.put("name", ' ');
		retval.put("x", x);
		retval.put("y", y);
		retval.put("z",z);
		return retval;
	}
	
	// Hashing and equals checks are done only against the position's name. Position names are unique!
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + toString().hashCode();
		return result;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	public int getZ(){
		return z;
	}

}

