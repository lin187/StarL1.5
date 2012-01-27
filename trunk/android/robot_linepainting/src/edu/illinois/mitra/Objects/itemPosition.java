package edu.illinois.mitra.Objects;

public class itemPosition implements Comparable<itemPosition> {
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
	
	// This compareTo implementation doesn't make tons of sense
	public int compareTo(itemPosition other) {
		if(name.equals(other.getName())) {
			return 1;
		}
		return 0;
	}
	
	//Return the distance to another position
	public int distanceTo(itemPosition other) {
		return (int) Math.sqrt(Math.pow(getX() - other.getX(), 2) + Math.pow(getY() - other.getY(), 2));
	}
	
	//Return true if this is facing towards another robot
	//This won't return accurate results for waypoints, only use to compare robots
	public boolean isFacing(itemPosition other, int radius) { 
		double isFacingCheck = (other.getY() - this.getY())*Math.sin(Math.toRadians(this.getAngle())) + (other.getX() - this.getX())*Math.cos(Math.toRadians(this.getAngle()));
		double lineDistance = Math.abs(((other.getY() - this.getY()) - (other.getX() - this.getX())*Math.tan(Math.toRadians(this.getAngle()))/Math.sqrt(1+Math.pow(Math.tan(Math.toRadians(getAngle())),2))));
		if(lineDistance < (2*radius) && (isFacingCheck > 0)) {
			return true;
		}
		return false;
	}
	
	//Return how many degrees need to be rotated until facing a position
	public int angleTo(itemPosition other) {
		int delta_x = other.getX() - this.getX();
		int delta_y = other.getY() - this.getY();
		int angle = this.angle;
		int otherAngle = (int) Math.toDegrees(Math.atan2(delta_y,delta_x));
		if(angle > 180) {
			angle -= 360;
		}
		int retAngle = min_magitude((otherAngle - angle),(angle - otherAngle));
		
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + angle;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + x;
		result = prime * result + y;
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
		if (angle != other.angle)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}
	
	private int min_magitude(int a1, int a2) {
		if(Math.abs(a1) < Math.abs(a2)) {
			return a1;
		} else {
			return a2;
		}
	}
}
