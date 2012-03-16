package edu.illinois.mitra.starl.objects;

import java.util.HashMap;

import android.util.Log;
import edu.illinois.mitra.starl.exceptions.ItemFormattingException;
import edu.illinois.mitra.starl.interfaces.Traceable;

public class itemPosition implements Comparable<itemPosition>, Traceable {
	private static final String TAG = "itemPosition";
	private static final String ERR = "Critical Error";
	
	private String name;
	private int x;
	private int y;
	private int angle;
	
	public itemPosition(String name, int x, int y, int angle) {
		super();
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
	
	public itemPosition(String received) throws ItemFormattingException {
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
	public int compareTo(itemPosition other) {
		if(!name.equals(other.getName())) {
			return 1;
		}
		return 0;
	}
	
	//Return the distance to another position
	public int distanceTo(itemPosition other) {
		if(other == null) {
			Log.e(ERR, "Called distanceTo on a null object!");
			return 0;
		}
		return (int) Math.sqrt(Math.pow(getX() - other.getX(), 2) + Math.pow(getY() - other.getY(), 2));
	}
	
	//Return true if this is facing towards another robot
	public boolean isFacing(itemPosition other, int radius) { 
		if(other == null) {
			Log.e(ERR, "Called isFacing on a null object!");
			return false;
		}
		
		double isFacingCheck = (other.getY() - this.getY())*Math.sin(Math.toRadians(this.getAngle())) + (other.getX() - this.getX())*Math.cos(Math.toRadians(this.getAngle()));
		double lineDistance = Math.abs(((other.getY() - this.getY()) - (other.getX() - this.getX())*Math.tan(Math.toRadians(this.getAngle()))/Math.sqrt(1+Math.pow(Math.tan(Math.toRadians(getAngle())),2))));
		if(lineDistance < (2*radius) && (isFacingCheck > 0)) {
			return true;
		}
		return false;
	}
	
	//Return how many degrees need to be rotated until facing a position
	public int angleTo(itemPosition other) {
		if(other == null) {
			Log.e(ERR, "Called angleTo on a null object!");
			return 0;
		}
		
		int delta_x = other.getX() - this.getX();
		int delta_y = other.getY() - this.getY();
		int angle = this.angle;
		int otherAngle = (int) Math.toDegrees(Math.atan2(delta_y,delta_x));
		if(angle > 180) {
			angle -= 360;
		}
		int retAngle = common.min_magitude((otherAngle - angle),(angle - otherAngle));
		
		if(retAngle > 180) {
			retAngle = retAngle-360;
		}
		if(retAngle < -180) {
			retAngle = retAngle+360;
		}
		return  Math.round(retAngle);
	}
	
	public String getName() {
		return name;
	}
	
	@Override public String toString() {
		return name + ": " + x + ", " + y + " " + angle + "\u00B0";
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getAngle() {
		return angle;
	}

	public void setPos(int x, int y, int angle) {
		this.x = x;
		this.y = y;
		this.angle = angle;
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
		itemPosition other = (itemPosition) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public HashMap<String, Object> getXML() {
		HashMap<String, Object> retval = new HashMap<String,Object>();
		retval.put("name", name);
		retval.put("x", x);
		retval.put("y", y);
		retval.put("angle",angle);
		return retval;
	}
}
