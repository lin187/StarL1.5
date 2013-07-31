package edu.illinois.mitra.starl.gvh;

import java.util.Vector;

import edu.illinois.mitra.starl.interfaces.GpsReceiver;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.illinois.mitra.starl.objects.PositionList;

/**
 * Handles receiving GPS broadcasts and updating the robot position and waypoints. Instantiated by the GlobalVarHolder
 * 
 * @author Adam Zimmerman
 * @version 1.0
 * @see GlobalVarHolder
 *
 */
public class Gps {
	private static final String TAG = "GPSReceiver";
	private static final String ERR = "Critical Error";
	
	private GlobalVarHolder gvh;
	private GpsReceiver mGpsReceiver;
	protected PositionList robot_positions;
	protected PositionList waypoint_positions;
	//this is the environment that is used for calculating collisions
	protected ObstacleList obs_positions;
	
	//this is the individual robot view of the world, stored for drawing on the simulator
	protected Vector<ObstacleList> viewOfWorlds;
	
	private String name;
	
	public Gps(GlobalVarHolder gvh, GpsReceiver mGpsReceiver) {
		this.mGpsReceiver = mGpsReceiver;
		this.robot_positions = mGpsReceiver.getRobots();
		this.waypoint_positions = mGpsReceiver.getWaypoints();
		this.obs_positions = mGpsReceiver.getObspoints();
		this.viewOfWorlds = mGpsReceiver.getViews();
		this.gvh = gvh;
		name = gvh.id.getName();
	}
		
	public void startGps() {
		gvh.log.i(TAG, "Starting GPS receiver");
		mGpsReceiver.start();
	}
	
	public void stopGps() {
		gvh.log.i(TAG, "Stopping GPS receiver");
		mGpsReceiver.cancel();
	}

	public PositionList getPositions() {
		return robot_positions;
	}
	
	public ItemPosition getPosition(String robot_name) {
		return robot_positions.getPosition(robot_name);
	}
	
	public ItemPosition getMyPosition() {
		return robot_positions.getPosition(name);
	}

	public PositionList getWaypointPositions() {
		return waypoint_positions;
	}
	
	public ItemPosition getWaypointPosition(String waypoint_name) {
		return waypoint_positions.getPosition(waypoint_name);
	}

	public ObstacleList getObspointPositions() {
		return obs_positions;
	}
	
	public Vector<ObstacleList> getViews(){
		return viewOfWorlds;
	}
	
}
