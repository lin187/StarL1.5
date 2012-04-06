package edu.illinois.mitra.starl.harness;

import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.RobotEventListener;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class IdealSimMotionAutomaton extends RobotMotion implements RobotEventListener {
	private static final String TAG = "MotionAutomaton";
	private IdealSimGpsProvider gpspro;
	private String name;
	
	private GlobalVarHolder gvh;
	
	public IdealSimMotionAutomaton(GlobalVarHolder gvh, IdealSimGpsProvider gpspro) {
		this.gvh = gvh;
		this.gpspro = gpspro;
		name = gvh.id.getName();
		gvh.addEventListener(this);
		gvh.trace.traceEvent(TAG, "Created");
	}
	@Override
	public void goTo(ItemPosition dest) {
		gvh.trace.traceEvent(TAG, "Go To", dest);
		gpspro.setDestination(name, dest);
		inMotion = true;
	}
	
	@Override
	public void goTo(ItemPosition dest, int maxCurveAngle, boolean useCollisionAvoidance) {
		this.goTo(dest);
	}

	@Override
	public void turnTo(ItemPosition dest) {
		throw new RuntimeException("turnTo isn't implemented for ideal motion yet. TOO BAD FOR YOU.");		
	}

	@Override
	public void cancel() {
		gvh.trace.traceEvent(TAG, "Cancelled");
		halt();
	}
	
	@Override
	public void halt() {
		gvh.trace.traceEvent(TAG, "Halt");
		gpspro.halt(name);
	}

	@Override
	public void resume() {		
	}

	@Override
	public void stop() {
		halt();
	}
	@Override
	public void robotEvent(int type, int event) {
		if(type == Common.EVENT_MOTION) {
			inMotion = (event!=Common.MOT_STOPPED);
		}
	}
}
