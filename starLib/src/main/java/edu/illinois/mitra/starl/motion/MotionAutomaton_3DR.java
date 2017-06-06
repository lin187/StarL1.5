package edu.illinois.mitra.starl.motion;

/**
 * Created by yangy14 on 6/2/2017.
 */

import java.util.Arrays;


import java.util.*;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.RobotEventListener.Event;
import edu.illinois.mitra.starl.models.Model_3DR;
import edu.illinois.mitra.starl.objects.*;



public class MotionAutomaton_3DR extends RobotMotion{

    protected static final String TAG = "MotionAutomaton";
    protected static final String ERR = "Critical Error";
    final int safeHeight = 122;   //need to set according to 3DR, unit: meters
    private boolean landed = true;

    protected GlobalVarHolder gvh;
    protected MiniDroneBTI bti;   //3DR is connected with wifi, so should we modify BTI to wifi?

    // Motion tracking
    protected ItemPosition destination;
    private Model_3DR mypos; // TD_NATHAN: probably need to create a minidrone one of these objects, as I think this is for AR drone...?

    //PID controller parameters
    double saturationLimit = 0;
    double windUpLimit = 0;
    int filterLength = 0;
    double Kpx = 0;
    double Kpy = 0;
    double Kix = 0;
    double Kiy = 0;
    double Kdx = 0;
    double Kdy = 0;

    PIDController PID_x = new PIDController(Kpx, Kix, Kdx, saturationLimit, windUpLimit, filterLength);
    PIDController PID_y = new PIDController(Kpy, Kiy, Kdy, saturationLimit, windUpLimit, filterLength);
    //need to set to new values as the 3DR



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

    public MotionAutomaton_3DR(GlobalVarHolder gvh, MiniDroneBTI bti) {
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
            gvh.log.d(TAG, "Going to X: " + Integer.toString(dest.x) + ", Y: " + Integer.toString(dest.y));
            //      Log.d(TAG, "Going to X: " + Integer.toString(dest.x) + ", Y: " + Integer.toString(dest.y));
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
                mypos = (Model_3DR)gvh.gps.getMyPosition(); // TD_NATHAN: check. I fixed it.
                //             if(mypos == null) { continue;}
                // if you change to 3D waypoints, use distanceTo instead of distanceTo2D
                int distance = mypos.distanceTo2D(destination);
                colliding = false;

                if(!colliding && stage != null) {
                    if(stage != prev)
                        gvh.log.e(TAG, "Stage is: " + stage.toString());

                    switch(stage) {
                        case INIT:
                            if(mode == OPMODE.GO_TO) {
                                PID_x.reset();
                                PID_y.reset();
                                setMaxTilt(5); // TODO: add max tilt to motion paramters class
                                if(landed){
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
                                double rollCommand = PID_x.getCommand(mypos.x, destination.x);
                                double pitchCommand = PID_y.getCommand(mypos.y, destination.y);
                                double yawCommand = calculateYaw();
                                double gazCommand = 0;
                                setControlInput(yawCommand, pitchCommand, rollCommand, gazCommand);
                                // TD_NATHAN: check and resolve: was mypos.angle
                                // that was the correct solution, has been resolved
                            }
                            break;
                        case HOVER:
                            if(distance <= param.GOAL_RADIUS) {
                                hover();
                                double yawCommand = calculateYaw();
                                setControlInput(yawCommand, 0, 0, 0);
                            }
                            else{
                                double rollCommand = PID_x.getCommand(mypos.x, destination.x);
                                double pitchCommand = PID_y.getCommand(mypos.y, destination.y);
                                double yawCommand = calculateYaw();
                                double gazCommand = 0;
                                setControlInput(yawCommand, pitchCommand, rollCommand, gazCommand);
                            }
                            break;
                        case TAKEOFF:
                            takeOff();
                            landed = false;
                            next = STAGE.MOVE;
                            break;
                        case LAND:
                            land();
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
        land();
    }

    @Override
    public void motion_stop() {
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
        bti.setRoll((byte) roll);
        bti.setPitch((byte) pitch);
        bti.setYaw((byte) yaw_v);
        // currently not moving to 3-D waypoints, so not sending a gaz command
        // if in the future you want to send one, uncomment the following line
        bti.setThrottle((byte) gaz);
        gvh.log.i(TAG, "control input as, yaw, pitch, roll, thrust " + yaw_v + ", " + pitch + ", " +roll + ", " +gaz);
    }

    /**
     *  	take off from ground
     */
    protected void takeOff(){
        //Bluetooth command to control the drone
        bti.sendTakeoff();
        gvh.log.i(TAG, "Drone taking off");
    }

    /**
     * land on the ground
     */
    protected void land(){
        //Bluetooth command to control the drone
        bti.sendLanding();
        gvh.log.i(TAG, "Drone landing");
    }

    /**
     * hover at current position
     */
    protected void hover(){
        //Bluetooth command to control the drone
        bti.hover();
        gvh.log.i(TAG, "Drone hovering");
    }

    private double calculateYaw() {
        // this method calculates a yaw correction, to keep the drone's yaw angle near 90 degrees
        if(mypos.yaw > 93) {
            return 5;
        }
        else if(mypos.yaw < 87) {
            return -5;
        }
        else {
            return 0;
        }
    }

    private void setMaxTilt(float val) {
        bti.setMaxTilt(val);
    }

    @Override
    public void turnTo(ItemPosition dest) {
        throw new IllegalArgumentException("quadcopter does not have a corresponding turn to");
    }

    @Override
    public void setParameters(MotionParameters param) {
        // TODO Auto-generated method stub
    }

}
