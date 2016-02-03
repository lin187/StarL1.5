package edu.illinois.mitra.starl.interfaces;

import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.illinois.mitra.starl.objects.Point3d;
import edu.illinois.mitra.starl.objects.PositionList;

public interface TrackedRobot {
	public abstract void initialize();
	public abstract Point3d predict(double[] noises, double timeSinceUpdate);
	public abstract void collision(Point3d collision_point);
	public abstract void updatePos(boolean followPredict);
	public abstract boolean inMotion();
	public abstract void updateSensor(ObstacleList obspoint_positions, PositionList<ItemPosition> sensepoint_positions);
}
