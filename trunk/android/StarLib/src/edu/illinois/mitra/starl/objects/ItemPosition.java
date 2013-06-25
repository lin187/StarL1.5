package edu.illinois.mitra.starl.objects;

import java.awt.geom.Line2D.Double;
import java.util.HashMap;

import edu.illinois.mitra.starl.exceptions.ItemFormattingException;
import edu.illinois.mitra.starl.interfaces.Traceable;
/**
 * This class represents a position and orientation in the XY plane. All robot and waypoint positions
 * are represented by ItemPositions.
 * 
 * @author Adam Zimmerman
 * @version 1.0
 */
public class ItemPosition implements Comparable<ItemPosition>, Traceable {
	private static final String TAG = "itemPosition";
	private static final String ERR = "Critical Error";
	
	public String name;
	public int x;
	public int y;
	public int angle;
	public int velocity;
	public long receivedTime;
	
	/**
	 * Construct an ItemPosition from a name, X, and Y positions, and an angle in degrees.
	 * 
	 * @param name The name of the new position
	 * @param x X position
	 * @param y Y position
	 * @param angle Direction the position is facing in degrees.
	 */
	public ItemPosition(String name, int x, int y, int angle) {
		if(name.contains(",")) {
			String[] namePieces = name.split(",");
			this.name = namePieces[0];
		} else {
			this.name = name;
		}
		this.x = x;
		this.y = y;
		this.angle = angle;
	}
	
	
	
	/**
	 * Construct an ItemPosition by cloning another
	 * 
	 * @param other The ItemPosition to clone
	 */
	public ItemPosition(ItemPosition other) {
		this.name = other.name;
		this.x = other.x;
		this.y = other.y;
		this.angle = other.angle;
	}
	
	
	/**
	 * Construct an ItemPosition from a received GPS broadcast message
	 * 
	 * @param received GPS broadcast received 
	 * @throws ItemFormattingException
	 */
	public ItemPosition(String received) throws ItemFormattingException {
		String[] parts = received.replace(",", "").split("\\|");
		if(parts.length == 6) {
			this.name = parts[1];
			this.x = Integer.parseInt(parts[2]);
			this.y = Integer.parseInt(parts[3]);
			this.angle = Integer.parseInt(parts[4]);
		} else {
			throw new ItemFormattingException("Should be length 6, is length " + parts.length);
		}
	}
	
	// This compareTo implementation doesn't make tons of sense
	public int compareTo(ItemPosition other) {
		if(!name.equals(other.name)) {
			return 1;
		}
		return 0;
	}
	
	/**
	 * @param other The ItemPosition to measure against
	 * @return Euclidean distance to ItemPosition other
	 */
	public int distanceTo(ItemPosition other) {
		if(other == null) {
			return 0;
		}
		return (int) Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(this.y - other.y, 2));
	}
	
	/**
	 * Determines if this ItemPosition is facing another position with a certain radius
	 * 
	 * @param other The position to check against
	 * @param radius The radius (in distance units) of the other position
	 * @return True if this position is facing a circle of radius with position other.
	 */
	public boolean isFacing(ItemPosition other, int radius) { 
		if(other == null) {
			return false;
		}
		
/*		double isFacingCheck = (other.y - this.y)*Math.sin(Math.toRadians(this.angle)) + (other.x - this.x)*Math.cos(Math.toRadians(this.angle));
		double lineDistance = Math.abs(((other.y - this.y) - (other.x - this.x)*Math.tan(Math.toRadians(this.angle))/Math.sqrt(1+Math.pow(Math.tan(Math.toRadians(angle)),2))));
		if(lineDistance < (2*radius) && (isFacingCheck > 0)) {
			return true;
		}
*/
/**
Code in comment was written by Adam and it is not working correctly.
The following code is written by Yixiao Lin. It is working correctly.
*/
    	double angleT = Math.toDegrees(Math.atan2((other.y - this.y) , (other.x - this.x)));
    	if(angleT  == 90){
    		if(this.y < other.y)
    			angleT = angleT + 90;
    		double temp = this.angle % 360;
    		if(temp > 0)
    			return true;
    		else
    			return false;
    	}
		if(angleT < 0)
		{
			angleT += 360;
		}
		double angleT1, angleT2, angleself;
		angleT1 = (angleT - 90) % 360;
		if(angleT1 < 0)
		{
			angleT1 += 360;
		}
		angleT2 = (angleT + 90) % 360;
		if(angleT2 < 0)
		{
			angleT2 += 360;
		}
		angleself = this.angle % 360;
		if(angleself < 0)
		{
			angleself += 360;
		}
		if(angleT2 <= 180)
		{
			if((angleself < angleT1) && (angleself > angleT2))
				return false;
			else
				return true;
		}
		else
		{
			if(angleself > angleT2 || angleself < angleT1)
				return false;
			else
				return true;
				
		}
	}

	/** 
	 * @param other The ItemPosition to measure against
	 * @return Number of degrees this position must rotate to face position other
	 */
	public int angleTo(ItemPosition other) {
		if(other == null) {
			return 0;
		}
		
		int delta_x = other.x - this.x;
		int delta_y = other.y - this.y;
		int angle = this.angle;
		int otherAngle = (int) Math.toDegrees(Math.atan2(delta_y,delta_x));
		if(angle > 180) {
			angle -= 360;
		}
		int retAngle = Common.min_magitude((otherAngle - angle),(angle - otherAngle));
		
		if(retAngle > 180) {
			retAngle = retAngle-360;
		}
		if(retAngle <= -180) {
			retAngle = retAngle+360;
		}
		return  Math.round(retAngle);
	}
	
	@Override public String toString() {
		return name + ": " + x + ", " + y + " " + angle + "\u00B0";
	}

	public void setPos(int x, int y, int angle) {
		this.x = x;
		this.y = y;
		this.angle = angle;
	}
	
	public void setPos(ItemPosition other) {
		this.x = other.x;
		this.y = other.y;
		this.angle = other.angle;
	}
	
	// Hashing and equals checks are done only against the position's name. Position names are unique!
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemPosition other = (ItemPosition) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public HashMap<String, Object> getXML() {
		HashMap<String, Object> retval = new HashMap<String,Object>();
		retval.put("name", name);
		retval.put("x", x);
		retval.put("y", y);
		retval.put("angle",angle);
		return retval;
	}
	
	public String toMessage() {
		return x + "," + y + "," + angle + "," + name;
	}
	
	public static ItemPosition fromMessage(String msg) {
		String[] parts = msg.split(",");
		if(parts.length != 4)
			throw new IllegalArgumentException("Can not parse ItemPosition from " + msg + ".");
		
		return new ItemPosition(parts[3], Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
	}

	// TODO: Un-deprecate these methods and make x and y private
	// (Or make this class immutable and leave it the way it is)
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getAngle() {
		return angle;
	}
	public String getName() {
		return name;
	}



/*	public boolean isFacing(Line2D.Double segment, int rOBOT_RADIUS) {
		// TODO Auto-generated method stub
		return false;
	}
	*/
}
