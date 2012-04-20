package edu.illinois.mitra.starl.harness;

import java.util.Observer;

import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public interface SimGpsProvider {

	public abstract void registerReceiver(String name,
			SimGpsReceiver simGpsReceiver);

	public abstract void addRobot(ItemPosition bot);

	// Implemented only by ideal gps provider
	public abstract void setDestination(String name, ItemPosition dest);
	
	// Implemented only be realistic gps provider
	public abstract void setVelocity(String name, int fwd, int radial);

	public abstract void halt(String name);

	public abstract PositionList getRobotPositions();

	public abstract void setWaypoints(PositionList loadedWaypoints);

	public abstract PositionList getWaypointPositions();

	public abstract void start();
	
	public abstract void addObserver(Observer o);
}