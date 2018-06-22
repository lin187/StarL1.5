package edu.illinois.mitra.starl.motion;

import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;

/**
 * Abstract class describing methods which all robot motion controllers should implement
 * @author Adam Zimmerman
 *
 */
public abstract class RobotMotion extends Thread implements Cancellable {
	
	public boolean inMotion = false;
	
	public boolean done = false;
		
	public RobotMotion() {}
	
	public RobotMotion(String name) {
		super("RobotMotion-"+name);
	}
		
	/**
	 * Go to a destination using the default motion parameters
	 * @param dest the robot's destination
	 */
	public abstract void goTo(ItemPosition dest, ObstacleList obsList);
	
	public abstract void goTo(ItemPosition dest);
	
	/**
	 * Turn to face a destination using the default motion parameters
	 * @param dest the destination to face
	 */
	public abstract void turnTo(ItemPosition dest);

	/**
	 * Enable robot motion
	 */
	public abstract void motion_resume();
	
	/**
	 * Stop the robot and disable motion until motion_resume is called. This cancels the current motion. 
	 */
	public abstract void motion_stop();
	
	/**
	 * Set the default motion parameters to use
	 * @param param the parameters to use by default
	 */
	public abstract void setParameters(MotionParameters param);


}
