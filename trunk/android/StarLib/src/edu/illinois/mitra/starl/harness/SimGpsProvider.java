package edu.illinois.mitra.starl.harness;

import java.util.Observer;
import java.util.Vector;

import edu.illinois.mitra.starl.objects.*;

public interface SimGpsProvider {

	public abstract void registerReceiver(String name,
			SimGpsReceiver simGpsReceiver);

	public abstract void addRobot(Model_iRobot bot);

	// Implemented only by ideal gps provider
	public abstract void setDestination(String name, ItemPosition dest, int vel);
	
	// Implemented only be realistic gps provider
	public abstract void setVelocity(String name, int fwd, int radial);

	public abstract void halt(String name);

	public abstract PositionList<Model_iRobot> getRobotPositions();

	public abstract void setWaypoints(PositionList<ItemPosition> loadedWaypoints);
	
	public abstract void setSensepoints(PositionList<ItemPosition> loadedSensepoints);
	
	public abstract void setObspoints(ObstacleList loadedObspoints);

	public abstract void setViews(ObstacleList environment, int nBots);

	public abstract PositionList<ItemPosition> getWaypointPositions();
	
	public abstract PositionList<ItemPosition> getSensePositions();

	public abstract ObstacleList getObspointPositions();
	
	public abstract Vector<ObstacleList> getViews();
	
	public abstract void start();
	
	public abstract void addObserver(Observer o);



}