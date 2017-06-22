package edu.illinois.mitra.starl.motion;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.models.Model_GhostAerial;
import edu.illinois.mitra.starl.models.Model_quadcopter;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;

/**
 * Created by wangcs2 on 6/1/2017.
 */

public class MotionAutomaton_GhostAerial extends RobotMotion {

    protected static final String TAG = "MotionAutomaton";
    protected static final String ERR = "Critical Error";
    final int safeHeight = 10; // moved dowm
    private boolean landed = true;

    protected GlobalVarHolder gvh;
    protected GhostAerialBTI bti;

    // Motion tracking
    protected ItemPosition destination;
    private Model_GhostAerial mypos; // TD_NATHAN: probably need to create a minidrone one of these objects, as I think this is for AR drone...?

    //PID controller parameters
    double saturationLimit = 50;
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

    private MotionAutomaton_GhostAerial.STAGE next = null;
    protected MotionAutomaton_GhostAerial.STAGE stage = MotionAutomaton_GhostAerial.STAGE.INIT;
    private MotionAutomaton_GhostAerial.STAGE prev = null;
    protected boolean running = false;
    boolean colliding = false;

    private enum OPMODE {
        GO_TO
    }
    private MotionAutomaton_GhostAerial.OPMODE mode = MotionAutomaton_GhostAerial.OPMODE.GO_TO;

    private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
    private volatile MotionParameters param = DEFAULT_PARAMETERS;
    //need to pass some more parameteres into this param
    //	MotionParameters.Builder settings = new MotionParameters.Builder();


    //	private volatile MotionParameters param = settings.build();

    public MotionAutomaton_GhostAerial(GlobalVarHolder gvh, GhostAerialBTI bti) {
        super(gvh.id.getName());
        this.gvh = gvh;
        this.bti = bti;
    }

    @Override
    public void goTo(ItemPosition dest, ObstacleList obsList) {
        goTo(dest);
    }

    @Override
    public void goTo(ItemPosition dest) {
        if (!inMotion || !this.destination.equals(dest)) {
            this.destination = new ItemPosition(dest.name, dest.x, dest.y, 0);
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

    private void startMotion() {
        running = true;
        stage = STAGE.INIT;
        inMotion = true;
    }

    @Override
    public void turnTo(ItemPosition dest) {
        throw new IllegalArgumentException("ghost does not have a corresponding turn to");
    }

    @Override
    public void motion_resume() {
        running = true;
    }

    @Override
    public void motion_stop() {
        stage = STAGE.HOVER;
        this.destination = null;
        running = false;
        inMotion = false;
    }

    @Override
    public void setParameters(MotionParameters param) {
        // TODO Auto-generated method stub
    }


    @Override
    public void run() {
        super.run();
        gvh.threadCreated(this);
        // some control parameters
        double kpx,kpy,kpz, kdx,kdy,kdz;
        kpx = kpy = kpz = 0.00033;
        kdx = kdy = kdz = 0.0006;
        while(true) {
            //			gvh.gps.getObspointPositions().updateObs();
            if(running) {
                mypos = (Model_GhostAerial)gvh.plat.getModel();
//				System.out.println(mypos.toString());
                int distance = (int) Math.sqrt(Math.pow((mypos.x - destination.x),2) + Math.pow((mypos.y - destination.y), 2));
                //int distance = mypos.distanceTo(destination);
                if(mypos.gaz < -50){
                    //		System.out.println("going down");
                }
                colliding = (stage != STAGE.LAND && mypos.gaz < -50);

                if(!colliding && stage != null) {
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
                            if(mypos.z < safeHeight){
                                // just a safe distance from ground
                                takeOff();
                                next = STAGE.TAKEOFF;
                                break;
                            }
                            if(distance <= param.GOAL_RADIUS) {
                                next = STAGE.GOAL;
                            }
                            else{
                                double Ax_d, Ay_d = 0.0;
                                double Ryaw, Rroll, Rpitch, Rvs, Ryawsp = 0.0;
                                //		System.out.println(destination.x - mypos.x + " , " + mypos.v_x);
                                Ax_d = (kpx * (destination.x - mypos.x) - kdx * mypos.v_x) ;
                                Ay_d = (kpy * (destination.y - mypos.y) - kdy * mypos.v_y) ;
                                Ryaw = Math.atan2(destination.y - mypos.y, destination.x - mypos.x);
                                //Ryaw = Math.atan2((destination.y - mypos.x), (destination.x - mypos.y));
                                Ryawsp = kpz * ((Ryaw - Math.toRadians(mypos.yaw)));
                                Rroll = Math.asin((Ay_d * Math.cos(Math.toRadians(mypos.yaw)) - Ax_d * Math.sin(Math.toRadians(mypos.yaw))) %1);
                                Rpitch = Math.asin( (-Ay_d * Math.sin(Math.toRadians(mypos.yaw)) - Ax_d * Math.cos(Math.toRadians(mypos.yaw))) / (Math.cos(Rroll)) %1);
                                Rvs = (kpz * (destination.z - mypos.z) - kdz * mypos.v_z);
                                //	System.out.println(Ryaw + " , " + Ryawsp + " , " +  Rroll  + " , " +  Rpitch + " , " + Rvs);

                                setControlInputRescale(Math.toDegrees(Ryawsp),Math.toDegrees(Rpitch)%360,Math.toDegrees(Rroll)%360,Rvs);
                                //setControlInput(Ryawsp/param.max_yaw_speed, Rpitch%param.max_pitch_roll, Rroll%param.max_pitch_roll, Rvs/param.max_gaz);
                                //next = STAGE.INIT;
                            }
                            break;
                        case HOVER:
                            setControlInput(0,0,0, 0);
                            // do nothing
                            break;
                        case TAKEOFF:
                            switch(mypos.z/(safeHeight/2)){
                                case 0:// 0 - 1/2 safeHeight
                                    setControlInput(0,0,0,1);
                                    break;
                                case 1: // 1/2- 1 safeHeight
                                    setControlInput(0,0,0, 0.5);
                                    break;
                                default: // above safeHeight:
                                    hover();
                                    if(prev != null){
                                        next = prev;
                                    }
                                    else{
                                        next = STAGE.HOVER;
                                    }
                                    break;
                            }
                            break;
                        case LAND:
                            switch(mypos.z/(safeHeight/2)){
                                case 0:// 0 - 1/2 safeHeight
                                    setControlInput(0,0,0,0);
                                    next = STAGE.STOP;
                                    break;
                                case 1: // 1/2- 1 safeHeight
                                    setControlInput(0,0,0, -0.05);
                                    break;
                                default:   // above safeHeight
                                    setControlInput(0,0,0,-0.5);
                                    break;
                            }
                            break;
                        case GOAL:
                            done = true;
                            gvh.log.i(TAG, "At goal!");
                            gvh.log.i("DoneFlag", "write");
                            if(param.STOP_AT_DESTINATION){
                                hover();
                                next = STAGE.HOVER;
                            }
                            running = false;
                            inMotion = false;
                            break;
                        case STOP:
                            gvh.log.i("FailFlag", "write");
                            System.out.println("STOP");
                            motion_stop();
                            //do nothing
                    }
                    if(next != null) {
                        prev = stage;
                        stage = next;
//						System.out.println("Stage transition to " + stage.toString() + ", the previous stage is "+ prev);

                        gvh.log.i(TAG, "Stage transition to " + stage.toString());
                        gvh.trace.traceEvent(TAG, "Stage transition", stage.toString(), gvh.time());
                    }
                    next = null;
                }

                if((colliding || stage == null) ) {
                    gvh.log.i("FailFlag", "write");
                    done = false;
                    motion_stop();
                    //	land();
                    //	stage = STAGE.LAND;
                }
            }
            gvh.sleep(param.AUTOMATON_PERIOD);
        }
    }

    protected void setControlInput(double yaw_v, double pitch, double roll, double gaz){
        //Bluetooth command to control the drone
        bti.setRoll(roll);
        bti.setPitch(pitch);
        bti.setYaw(yaw_v);
        // currently not moving to 3-D waypoints, so not sending a gaz command
        // if in the future you want to send one, uncomment the following line
        //bti.setThrottle((byte) gaz);
        gvh.log.i(TAG, "control input as, yaw, pitch, roll, thrust " + yaw_v + ", " + pitch + ", " +roll + ", " +gaz);
    }

    private void setControlInputRescale(double yaw_v, double pitch, double roll, double gaz){
        setControlInput(rescale(yaw_v, mypos.max_yaw_speed), rescale(pitch, mypos.max_pitch_roll), rescale(roll, mypos.max_pitch_roll), rescale(gaz, mypos.max_gaz));
    }

    private double rescale(double value, double max_value){
        if(Math.abs(value) > max_value){
            return (Math.signum(value));
        }
        else{
            return value/max_value;
        }
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

    @Override
    public void cancel() {
        running = false;
        land();
    }

    private double calculateYaw() {
        // this method calculates a yaw correction, to keep the drone's yaw angle near 90 degrees
        if(mypos.yaw > 93) {
            return 1;
        }
        else if(mypos.yaw < 87) {
            return -1;
        }
        else {
            return 0;
        }
    }

    private void setMaxTilt(float val) {
       // bti.setMaxTilt(val);
    }



}