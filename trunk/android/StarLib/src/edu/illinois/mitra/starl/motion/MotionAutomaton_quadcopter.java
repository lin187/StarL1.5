package edu.illinois.mitra.starl.motion;

import java.util.*;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.RobotEventListener.Event;
import edu.illinois.mitra.starl.models.Model_quadcopter;
import edu.illinois.mitra.starl.objects.*;

/**
 * This motion controller is for quadcopter models only
 * 
 * Motion controller which extends the RobotMotion abstract class. Capable of
 * going to destination or passing through a destination without stopping.
 * Includes optional collision avoidance which is controlled
 * by the motion parameters setting.
 *  
 * @author Yixiao Lin
 * @version 1.0
 */
public class MotionAutomaton_quadcopter extends RobotMotion {
	protected static final String TAG = "MotionAutomaton";
	protected static final String ERR = "Critical Error";
	final int safeHeight = 150;

	protected GlobalVarHolder gvh;
	protected BluetoothInterface bti;

	// Motion tracking
	protected ItemPosition destination;
	private Model_quadcopter mypos;


	protected enum STAGE {
		INIT, MOVE, HOVER, TAKEOFF, LAND, GOAL, STOP
	}

	private STAGE next = null;
	protected STAGE stage = STAGE.INIT;
	private STAGE prev = null;
	protected boolean running = false;
	boolean colliding = false;

	private enum OPMODE {
		GO_TO
	}

	private OPMODE mode = OPMODE.GO_TO;

	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	private volatile MotionParameters param = DEFAULT_PARAMETERS;
	//need to pass some more parameteres into this param
	//	MotionParameters.Builder settings = new MotionParameters.Builder();


	//	private volatile MotionParameters param = settings.build();

	public MotionAutomaton_quadcopter(GlobalVarHolder gvh, BluetoothInterface bti) {
		super(gvh.id.getName());
		this.gvh = gvh;
		this.bti = bti;

	}

	public void goTo(ItemPosition dest, ObstacleList obsList) {
		goTo(dest);
	}

	public void goTo(ItemPosition dest) {
		if((inMotion && !this.destination.equals(dest)) || !inMotion) {
			this.destination = new ItemPosition(dest.name,dest.x,dest.y,dest.z);
			//this.destination = dest;
			this.mode = OPMODE.GO_TO;
			startMotion();
		}
	}

	@Override
	public synchronized void start() {
		super.start();
		gvh.log.d(TAG, "STARTED!");
	}

	@Override
	public void run() {
		super.run();
		gvh.threadCreated(this);
		// some control parameters
		double z_error = 10.0;
		double kpx,kpy,kpz, kdx,kdy,kdz,kiz;
		kpx = kpy = kpz = 1.0;
		kdx = kdy = kdz = 0.2;
		kiz = 0.1;
		while(true) {
			//			gvh.gps.getObspointPositions().updateObs();
			if(running) {
				mypos = (Model_quadcopter)gvh.plat.getModel();
				System.out.println(mypos.toString());
				int distance = mypos.distanceTo(destination);				
				colliding = false;

				if(!colliding && stage != null) {
					if(stage != prev)
						gvh.log.e(TAG, "Stage is: " + stage.toString());

					switch(stage) {
					case INIT:
						if(mode == OPMODE.GO_TO) {
							if(mypos.z < safeHeight){
								// just a safe distance from ground
								takeOff();
								next = STAGE.TAKEOFF;
							}
							else{
								if(distance <= param.GOAL_RADIUS) {
									next = STAGE.GOAL;
								}
								else{
									next = STAGE.MOVE;
								}
							}
						}	
						break;
					case MOVE:
						if(distance <= param.GOAL_RADIUS) {
							next = STAGE.GOAL;
						}
						else{
							double Ax_d, Ay_d, Az_d;
							double Ryaw, Rroll, Rpitch, Rthrust;
							z_error += destination.z - mypos.z;
							Ax_d = kpx * (destination.x - mypos.x) - kdx * mypos.v_x;
							Ay_d = kpy * (destination.y - mypos.y) - kdy * mypos.v_y;
							Az_d = kpz * (destination.z - mypos.z) - kdz * mypos.v_z + kiz * z_error;

							Ryaw = Math.atan2((destination.y - mypos.x), (destination.x - mypos.y));
							Rroll = (Ay_d * Math.cos(mypos.yaw) - Ax_d * Math.sin(mypos.yaw));
							Rpitch = -(Ay_d * Math.sin(mypos.yaw) - Ax_d * Math.cos(mypos.yaw));
							Rthrust = Az_d / Math.cos(mypos.pitch) / Math.cos(mypos.roll);

							setControlInput(Ryaw, Rpitch, Rroll, Rthrust);
							//next = STAGE.INIT;
						}
						break;
					case HOVER:
						setControlInput(mypos.yaw,0,0, 0);
						// do nothing
						break;
					case TAKEOFF:
						switch(mypos.z/(safeHeight/2)){
						case 0:// 0 - 1/2 safeHeight
							setControlInput(mypos.yaw,0,0,1);
							break;
						case 1: // 1/2- 1 safeHeight
							setControlInput(mypos.yaw,0,0, 0.5);
							break;
						default: // above safeHeight:
							if(mypos.v_z > 0){
								setControlInput(mypos.yaw, 0,0, -0.2);
							}
							else{
								hover();
								if(prev != null){
									next = prev;
								}
								else{
									next = STAGE.HOVER;
								}
							}
							break;
						}
						break;
					case LAND:
						switch(mypos.z/(safeHeight/2)){
						case 0:// 0 - 1/2 safeHeight
							setControlInput(mypos.yaw,0,0,0);
							next = STAGE.STOP;
							break;
						case 1: // 1/2- 1 safeHeight
							setControlInput(mypos.yaw,0,0, 1);
							break;
						default:   // above safeHeight
							setControlInput(mypos.yaw,0,0,-1);
							break;
						}
						break;
					case GOAL:
						gvh.log.i(TAG, "At goal!");
						if(param.STOP_AT_DESTINATION){
							hover();
							next = STAGE.HOVER;
						}
						running = false;
						inMotion = false;
						break;
					case STOP:
						//do nothing
					}
					if(next != null) {
						prev = stage;
						stage = next;
						System.out.println("Stage transition to " + stage.toString() + "previous stage is "+ prev);

						gvh.log.i(TAG, "Stage transition to " + stage.toString());
						gvh.trace.traceEvent(TAG, "Stage transition", stage.toString(), gvh.time());
					}
					next = null;
				} 

				if((colliding || stage == null) ) {
					land();
					stage = STAGE.LAND;
				}
			}
			gvh.sleep(param.AUTOMATON_PERIOD);
		}
	}

	public void cancel() {
		running = false;
		bti.disconnect();
	}

	@Override
	public void motion_stop() {
		land();
		stage = STAGE.LAND;
		this.destination = null;
		running = false;
		inMotion = false;
	}

	@Override
	public void motion_resume() {
		running = true;
	}

	private void startMotion() {
		running = true;
		stage = STAGE.INIT;
		inMotion = true;
	}

	protected void sendMotionEvent(int motiontype, int... argument) {
		// TODO: This might not be necessary
		gvh.trace.traceEvent(TAG, "Motion", Arrays.toString(argument), gvh.time());
		gvh.sendRobotEvent(Event.MOTION, motiontype);
	}

	protected void setControlInput(double yaw_v, double pitch, double roll, double gaz){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "control input as, yaw, pitch, roll, thrust " + yaw_v + ", " + pitch + ", " +roll + ", " +gaz);
		/*
		if(running) {
			if(velocity != 0) {
				sendMotionEvent(Common.MOT_STRAIGHT, velocity);
			} else {
				sendMotionEvent(Common.MOT_STOPPED, 0);
			}
			bti.send(BluetoothCommands.straight(velocity));
		}
		 */
	}

	/**
	 *  	take off from ground
	 */
	protected void takeOff(){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "Drone taking off");
	}

	/**
	 * land on the ground
	 */
	protected void land(){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "Drone landing");
	}

	/**
	 * hover at current position
	 */
	protected void hover(){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "Drone hovering");
	}

	@Override
	public void turnTo(ItemPosition dest) {
		throw new IllegalArgumentException("quadcopter does not have a corresponding turn to");
	}

	@Override
	public void setParameters(MotionParameters param) {
		// TODO Auto-generated method stub		
	}


	/**
	 * Slow down linearly upon coming within R_slowfwd of the goal
	 * 
	 * @param distance
	 * @return
	 */
	/*
	private int LinSpeed(int distance) {
		if(distance > param.SLOWFWD_RADIUS)
			return param.LINSPEED_MAX;
		if(distance > param.GOAL_RADIUS && distance <= param.SLOWFWD_RADIUS) {
			return param.LINSPEED_MIN + (int) ((distance - param.GOAL_RADIUS) * linspeed);
		}
		return param.LINSPEED_MIN;
	}

	// Detects an imminent collision with another robot or with any obstacles

	@Override
	public void setParameters(MotionParameters param) {
		this.param = param;/		this.linspeed = (double) (param.LINSPEED_MAX - param.LINSPEED_MIN) / Math.abs((param.SLOWFWD_RADIUS - param.GOAL_RADIUS));
		this.turnspeed = (param.TURNSPEED_MAX - param.TURNSPEED_MIN) / (param.SLOWTURN_ANGLE - param.SMALLTURN_ANGLE);
	}
	 */
}
