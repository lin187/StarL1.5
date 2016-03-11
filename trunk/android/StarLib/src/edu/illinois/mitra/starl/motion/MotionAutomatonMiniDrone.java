package edu.illinois.mitra.starl.motion;

import android.util.Log;

import java.util.Arrays;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.RobotEventListener.Event;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.Model_iRobot;
import edu.illinois.mitra.starl.objects.ObstacleList;

//import edu.illinois.mitra.starl.models.Model_quadcopter;

/**
 * Created by VerivitalLab on 2/19/2016.
 */
public class MotionAutomatonMiniDrone extends RobotMotion {
    protected static final String TAG = "MotionAutomaton";
    protected static final String ERR = "Critical Error";
    final int safeHeight = 150;
    private boolean landed = true;

    protected GlobalVarHolder gvh;
    protected MiniDroneBTI bti;

    // Motion tracking
    protected ItemPosition destination;
    private Model_iRobot mypos;

    //PID controller parameters
    double saturationLimit = 100;
    double windUpLimit = 185;
    int filterLength = 8;
    /*double Kpx = 0.2;
    double Kpy = 0.2;
    double Kix = 0.04;
    double Kiy = 0.04;
    double Kdx = 0.4;
    double Kdy = 0.45;*/
    // the ones below work pretty well
    double Kpx = 0.0714669809792096;
    double Kpy = 0.0714669809792096;
    double Kix = 0.0110786899216426;
    double Kiy = 0.0110786899216426;
    double Kdx = 0.113205037832174;
    double Kdy = 0.113205037832174;

    PIDController PID_x = new PIDController(Kpx, Kix, Kdx, saturationLimit, windUpLimit, filterLength);
    PIDController PID_y = new PIDController(Kpy, Kiy, Kdy, saturationLimit, windUpLimit, filterLength);


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

    public MotionAutomatonMiniDrone(GlobalVarHolder gvh, MiniDroneBTI bti) {
        super(gvh.id.getName());
        this.gvh = gvh;
        this.bti = bti;


    }


    public void goTo(ItemPosition dest, ObstacleList obsList) {
        goTo(dest);
    }

    public void goTo(ItemPosition dest) {
        if((inMotion && !this.destination.equals(dest)) || !inMotion) {
            this.destination = new ItemPosition(dest.name,dest.x,dest.y,0);
            Log.d(TAG, "Going to X: " + Integer.toString(dest.x) + ", Y: " + Integer.toString(dest.y));
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
        while(true) {
            //			gvh.gps.getObspointPositions().updateObs();
            if(running) {
                mypos = gvh.gps.getMyPosition();
   //             if(mypos == null) { continue;}
                int distance = mypos.distanceTo(destination);
                colliding = false;

                if(!colliding && stage != null) {
                    if(stage != prev)
                        gvh.log.e(TAG, "Stage is: " + stage.toString());

                    switch(stage) {
                        case INIT:
                            if(mode == OPMODE.GO_TO) {
                                PID_x.reset();
                                PID_y.reset();
                                bti.setMaxTilt(5); // TODO: add max tilt to motion paramters class
                                if(landed){
                                    // just a safe distance from ground
                                    //takeOff();
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
                                double xCommand = PID_x.getCommand(mypos.x, destination.x);
                                double yCommand = PID_y.getCommand(mypos.y, destination.y);
                                bti.setRoll((byte) xCommand);
                                bti.setPitch((byte) yCommand);
                                Log.d(TAG, "Sent roll: " + xCommand + " Sent pitch: " + yCommand);
                                // send a small yaw command if not within 87-93 degrees
                                if(mypos.angle > 93) {
                                    bti.setYaw((byte) 5);
                                }
                                else if(mypos.angle < 87) {
                                    bti.setYaw((byte) -5);
                                }
                                else {
                                    bti.setYaw((byte) 0);
                                }

                                //next = STAGE.INIT;
                            }
                            break;
                        case HOVER:
                            /*if(distance <= param.GOAL_RADIUS) {
                                bti.hover();
                            }
                            else{
                                double xCommand = PID_x.getCommand(mypos.x, destination.x);
                                double yCommand = PID_y.getCommand(mypos.y, destination.y);
                                bti.setRoll((byte) xCommand);
                                bti.setPitch((byte) yCommand);
                                Log.d(TAG, "Sent roll: " + xCommand + " Sent pitch: " + yCommand);
                            }*/
                            double xCommand = PID_x.getCommand(mypos.x, destination.x);
                            double yCommand = PID_y.getCommand(mypos.y, destination.y);
                            bti.setRoll((byte) xCommand);
                            bti.setPitch((byte) yCommand);
                            Log.d(TAG, "Sent roll: " + xCommand + " Sent pitch: " + yCommand);
                            // do nothing
                            break;
                        case TAKEOFF:
                            bti.sendTakeoff();
                            // sleep to allow time for takeoff
                            landed = false;
                            next = STAGE.MOVE;
                            break;
                        case LAND:
                            bti.sendLanding();
                            break;
                        case GOAL:
                            gvh.log.i(TAG, "At goal!");
                            if(param.STOP_AT_DESTINATION){
                                next = STAGE.HOVER;
                            }
                           // running = false;
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
        bti.sendLanding();

    }

    @Override
    public void motion_stop() {
        //land();
        stage = STAGE.HOVER;
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
