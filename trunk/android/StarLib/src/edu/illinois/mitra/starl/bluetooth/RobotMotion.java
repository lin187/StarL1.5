package edu.illinois.mitra.starl.bluetooth;

import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.objects.ItemPosition;

public abstract class RobotMotion implements Cancellable {
	
	public boolean inMotion = false;
	
	public abstract void goTo(ItemPosition dest);
	public abstract void goTo(ItemPosition dest, int maxCurveAngle, boolean useCollisionAvoidance);
	public abstract void turnTo(ItemPosition dest);
	
	public abstract void halt();
	public abstract void resume();
	public abstract void stop();
}
