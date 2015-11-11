package edu.illinois.mitra.starl.interfaces;

import java.util.Vector;

import edu.illinois.mitra.starl.objects.*;


public interface GpsReceiver extends Cancellable {

	public abstract void start();
	
	public abstract PositionList<Model_iRobot> getRobots();
	public abstract PositionList<ItemPosition> getWaypoints();
	public abstract ObstacleList getObspoints();
	public abstract Vector<ObstacleList> getViews();
	

}