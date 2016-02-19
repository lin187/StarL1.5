package edu.illinois.mitra.starl.harness;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.models.Model_quadcopter;
import edu.illinois.mitra.starl.motion.MotionAutomaton_quadcopter;

public class RealisticSimMotionAutomaton_quadcopter extends MotionAutomaton_quadcopter {
	private SimGpsProvider gpsp;
	private String name;
	private Model_quadcopter my_model;
	
	public RealisticSimMotionAutomaton_quadcopter(GlobalVarHolder gvh, SimGpsProvider gpsp) {
		super(gvh, null);
		name = gvh.id.getName();
		this.gpsp = gpsp;
		this.my_model = (Model_quadcopter)gvh.plat.model;
	}

	@Override
	public void setControlInput(double v_yaw, double pitch, double roll, double gaz){
		gpsp.setControlInput(name, v_yaw, pitch, roll, gaz);
	}

	/**
	 *  	take off from ground
	 */
	@Override
	protected void takeOff(){
		gvh.log.i(TAG, "Drone taking off");
		setControlInput(my_model.yaw, 0, 0, 1);
	}
	
	/**
	 * land on the ground
	 */
	@Override
	protected void land(){
		gvh.log.i(TAG, "Drone landing");
		//setControlInput(my_model.yaw, 0, 0, 5);
	}
	
	/**
	 * hover at current position
	 */
	@Override
	protected void hover(){
		gvh.log.i(TAG, "Drone hovering");
		setControlInput(my_model.yaw, 0, 0, 0);
	}

	@Override
	public void cancel() {
		super.running = false;
	}
}
