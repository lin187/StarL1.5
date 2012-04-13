package edu.illinois.mitra.starl.gvh;

import edu.illinois.mitra.starl.interfaces.GpsReceiver;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public class Gps {
	private static final String TAG = "GPSReceiver";
	private static final String ERR = "Critical Error";
	
	private GlobalVarHolder gvh;
	private GpsReceiver mGpsReceiver;
	protected PositionList robot_positions;
	protected PositionList waypoint_positions;
	private String name;
	
	public Gps(GlobalVarHolder gvh, GpsReceiver mGpsReceiver) {
		this.mGpsReceiver = mGpsReceiver;
		this.robot_positions = mGpsReceiver.getRobots();
		this.waypoint_positions = mGpsReceiver.getWaypoints();
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
}
