package edu.illinois.mitra.starl.bluetooth;

import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.objects.ItemPosition;

/**
 * Interface describing methods which all robot motion controllers should implement
 * @author Adam Zimmerman
 *
 */
public abstract class RobotMotion implements Cancellable {
	
	public boolean inMotion = false;
	
	/**
	 * Go to a destination using the default motion parameters
	 * @param dest the robot's destination
	 */
	public abstract void goTo(ItemPosition dest);
	
	/**
	 * Go to a destination using specific motion parameters
	 * @param dest the robot's destination
	 * @param maxCurveAngle the maximum angle at which to travel in an arcing motion (currently, iRobot specific)
	 * @param useCollisionAvoidance enable/disable collision avoidance
	 */
	public abstract void goTo(ItemPosition dest, int maxCurveAngle, boolean useCollisionAvoidance);
	
	/**
	 * Turn to face a destination 
	 * @param dest the destination to face
	 */
	public abstract void turnTo(ItemPosition dest);
	
	/**
	 * iRobot specific
	 * @return the charge percentage of the robot's battery
	 */
	public abstract int getBatteryPercentage();
	
	/**
	 * Stop the robot
	 */
	public abstract void halt();
	
	
	/**
	 * Enable robot motion
	 */
	public abstract void resume();
	
	/**
	 * Stop the robot and disable motion until resume is called 
	 */
	public abstract void stop();
}
