package edu.illinois.mitra.starl.motion;

import java.awt.Point;
import java.util.Arrays;


import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.RobotEventListener.Event;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.starl.objects.*;


/**
 * Motion controller which extends the RobotMotion abstract class. Capable of
 * going to destination waypoints and turning to face waypoints using custom
 * motion parameters. Includes optional collision avoidance which is controlled
 * by the motion parameters setting.
 * 
 * @author Adam Zimmerman
 * @version 1.1
 */
public class MotionAutomaton extends RobotMotion {
	protected static final String TAG = "MotionAutomaton";
	protected static final String ERR = "Critical Error";

	// MOTION CONTROL CONSTANTS
	//	public static int R_arc = 700;
//	public static int R_slowfwd = 700;
//	public static int A_smallturn = 3;
//	public static int A_straight = 6;
//	public static int A_arc = 25;
//	public static int A_arcexit = 30;
//	public static final int param.SLOWTURN_ANGLE = 25;
//	public static final int ROBOT_RADIUS = 180;

	// DELAY BETWEEN EACH RUN OF THE AUTOMATON
//	private static final int AUTOMATON_PERIOD = 60;
//	public static final int SAMPLING_PERIOD = 300;

	// COLLISION AVOIDANCE CONSTANTS
//	public static final int COLLISION_STRAIGHTTIME = 1250;

	protected GlobalVarHolder gvh;
	protected BluetoothInterface bti;

	// Motion tracking
	protected ItemPosition destination;
	private ItemPosition mypos;
	private ItemPosition blocker;

	protected enum STAGE {
		INIT, ARCING, STRAIGHT, TURN, SMALLTURN, GOAL
	}

	private STAGE next = null;
	protected STAGE stage = STAGE.INIT;
	private STAGE prev = null;
	protected boolean running = false;

	private enum OPMODE {
		GO_TO, TURN_TO
	}

	private OPMODE mode = OPMODE.GO_TO;

	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	private volatile MotionParameters param = DEFAULT_PARAMETERS;
	//need to pass some more parameteres into this param
//	MotionParameters.Builder settings = new MotionParameters.Builder();
	
	
//	private volatile MotionParameters param = settings.build();
	
	
	// Collision avoidance
	private enum COLSTAGE {
		TURN, STRAIGHT
	}
	private COLSTAGE colprev = null;
	private COLSTAGE colstage = COLSTAGE.TURN;
	private COLSTAGE colnext = null;
	private int col_straightime = 0;
	private boolean halted = false;

	private double linspeed;
	private double turnspeed;

	public MotionAutomaton(GlobalVarHolder gvh, BluetoothInterface bti) {
		super(gvh.id.getName());
		this.gvh = gvh;
		this.bti = bti;
		this.linspeed = (param.LINSPEED_MAX - param.LINSPEED_MIN) / (double) (param.SLOWFWD_RADIUS - param.GOAL_RADIUS);
		this.turnspeed = (param.TURNSPEED_MAX - param.TURNSPEED_MIN) / (param.SLOWTURN_ANGLE - param.SMALLTURN_ANGLE);
	}

	public void goTo(ItemPosition dest) {
		if((inMotion && !this.destination.equals(dest)) || !inMotion) {
			this.destination = dest;
			this.mode = OPMODE.GO_TO;
			startMotion();
		}
	}
/*	
	public void goTo(RRTNode pathNode){
		if(pathNode != null){
			Stack<ItemPosition> pathStack= new Stack<ItemPosition>();
				while(pathNode.parent != null && pathNode != null){
					ItemPosition ToGo= new ItemPosition("midpoint", pathNode.position.x, pathNode.position.y, 0);
					pathStack.push(ToGo);
					pathNode = pathNode.parent;
				}
				while(!pathStack.isEmpty()){
					ItemPosition ToGo = pathStack.pop();
					goTo(ToGo);
				}
		}
	}
*/
	public void turnTo(ItemPosition dest) {
		if((inMotion && !this.destination.equals(dest)) || !inMotion) {
			this.destination = dest;
			this.mode = OPMODE.TURN_TO;
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
		boolean colliding = false;
		while(true) {
			if(running) {
				mypos = gvh.gps.getMyPosition();
				int distance = mypos.distanceTo(destination);
				int angle = mypos.angleTo(destination);
				int absangle = Math.abs(angle);

				switch(param.COLAVOID_MODE) {
				case BUMPERCARS:
					colliding = false;
					break;
				case USE_COLAVOID:
					colliding = collision();
					break;
				case STOP_ON_COLLISION:
					colliding = collision();
				default:
					colliding = false;
					break;
				}

				if(!colliding && stage != null) {
					halted = false;
					if(stage != prev)
						gvh.log.e(TAG, "Stage is: " + stage.toString());
					switch(stage) {
					case INIT:
						halted = false;
						if(mode == OPMODE.GO_TO) {
							if(distance <= param.GOAL_RADIUS) {
								next = STAGE.GOAL;
							} else if(param.ENABLE_ARCING && distance <= param.ARC_RADIUS && absangle <= param.ARCANGLE_MAX) {
								next = STAGE.ARCING;
							} else {
								next = STAGE.TURN;
							}
						} else {
							next = STAGE.TURN;
						}
						break;
					case ARCING:
						// If this is the first run of ARCING, begin the arc
						if(stage != prev) {
							int radius = curveRadius();
							curve(param.ARCSPEED_MAX, radius);
						} else {
							// Otherwise, check exit conditions
							if(absangle > param.ARC_EXIT_ANGLE)
								next = STAGE.TURN;
							if(absangle < param.STRAIGHT_ANGLE)
								next = STAGE.STRAIGHT;
							if(distance <= param.GOAL_RADIUS)
								next = STAGE.GOAL;
						}
						break;
					case STRAIGHT:
						if(stage != prev) {
							straight(LinSpeed(distance));
						} else {
							if(Common.inRange(distance, param.GOAL_RADIUS, param.SLOWFWD_RADIUS))
								straight(LinSpeed(distance));
							if(Common.inRange(absangle, param.SMALLTURN_ANGLE, param.ARCANGLE_MAX))
								next = STAGE.SMALLTURN;
							if(absangle > param.ARCANGLE_MAX)
								next = STAGE.TURN;
							if(distance <= param.GOAL_RADIUS)
								next = STAGE.GOAL;
						}
						break;
					case TURN:
						if(stage != prev) {
							turn(TurnSpeed(absangle), angle);
						} else {
							if(absangle <= param.SMALLTURN_ANGLE) {
								gvh.log.i(TAG, "Turn stage: within angle bounds!");
								next = (mode == OPMODE.GO_TO) ? STAGE.STRAIGHT : STAGE.GOAL;
							} else if(absangle <= param.SLOWTURN_ANGLE) {
								// Resend a reduced-speed turn command if we're
								// within the slow-turn window
								turn(TurnSpeed(absangle), angle);
							}
						}
						break;
					case SMALLTURN:
						if(stage != prev) {
							int radius = curveRadius() / 2;
							curve(LinSpeed(distance), radius);
						} else {
							if(absangle <= param.SMALLTURN_ANGLE)
								next = STAGE.STRAIGHT;
							if(absangle > param.ARCANGLE_MAX)
								next = STAGE.TURN;
							if(distance <= param.GOAL_RADIUS)
								next = STAGE.GOAL;
						}
						break;
					case GOAL:
						gvh.log.i(TAG, "At goal!");
						if(param.STOP_AT_DESTINATION)
							straight(0);
						running = false;
						inMotion = false;
						break;
					}

					prev = stage;
					if(next != null) {
						stage = next;
						gvh.log.i(TAG, "Stage transition to " + stage.toString());
						gvh.trace.traceEvent(TAG, "Stage transition", stage.toString(), gvh.time());
					}
					next = null;
				} else if((colliding && (param.COLAVOID_MODE == COLAVOID_MODE_TYPE.USE_COLAVOID)) || stage == null) {
					// Collision imminent! Stop the robot
					if(stage != null) {
						gvh.log.d(TAG, "Imminent collision detected!");
						stage = null;
						straight(0);
						colnext = null;
						colprev = null;
						colstage = COLSTAGE.TURN;
					}

					switch(colstage) {
					case TURN:
						if(colstage != colprev) {
							gvh.log.d(TAG, "Colliding: sending turn command");
							turn(param.TURNSPEED_MAX, -1 * mypos.angleTo(blocker));
						}

						if(!collision()) {
							colnext = COLSTAGE.STRAIGHT;
							gvh.log.i(TAG, "FREE OF BLOCKER!");
						} else {
							gvh.log.d(TAG, "colliding with " + blocker.name + " - " + mypos.isFacing(blocker, 180) + " - " + mypos.distanceTo(blocker));
						}
						break;
					case STRAIGHT:
						if(colstage != colprev) {
							gvh.log.d(TAG, "Colliding: sending straight command");
							straight(param.LINSPEED_MAX);
							col_straightime = 0;
						} else {
							col_straightime += param.AUTOMATON_PERIOD;
							// If a collision is imminent (again), return to the
							// turn stage
							if(collision()) {
								gvh.log.d(TAG, "Collision imminent! Cancelling straight stage");
								straight(0);
								colnext = COLSTAGE.TURN;
							}
							// If we're collision free and have been for enough
							// time, restart normal motion
							if(!collision() && col_straightime >= param.COLLISION_AVOID_STRAIGHTTIME) {
								gvh.log.d(TAG, "Free! Returning to normal execution");
								colprev = null;
								colnext = null;
								colstage = null;
								stage = STAGE.INIT;
							}
						}
						break;
					}
					colprev = colstage;
					if(colnext != null) {
						colstage = colnext;
						gvh.log.i(TAG, "Advancing stage to " + colnext);
					}
					colnext = null;
				} else if(colliding && (param.COLAVOID_MODE == COLAVOID_MODE_TYPE.STOP_ON_COLLISION)) {
					// Stop the robot if collision avoidance is disabled and a
					// collision is imminent
					if(!halted) {
						halted = true;
						System.out.println(gvh.id.getName() + " HAS COLLIDED!");
						gvh.log.d(TAG, "No collision avoidance! Halting.");
						gvh.trace.traceEvent(TAG, "Halting motion", gvh.time());
						straight(0);
						stage = STAGE.INIT;
					}
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
		straight(0);
		stage = STAGE.INIT;
		this.destination = null;
		running = false;
		inMotion = false;
	}

	@Override
	public void motion_resume() {
		running = true;
	}

	// Calculates the radius of curvature to meet a target
	private int curveRadius() {
		int x0 = mypos.x;
		int y0 = mypos.y;
		int x1 = destination.x;
		int y1 = destination.y;
		int theta = mypos.angle;
		double alpha = -180 + Math.toDegrees(Math.atan2((y1 - y0), (x1 - x0)));
		double rad = -(Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2)) / (2 * Math.sin(Math.toRadians(alpha - theta))));
		return (int) rad;
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

	protected void curve(int velocity, int radius) {
		if(running) {
			sendMotionEvent(Common.MOT_ARCING, velocity, radius);
			bti.send(BluetoothCommands.curve(velocity, radius));
		}
	}

	protected void straight(int velocity) {
		gvh.log.i(TAG, "Straight at velocity " + velocity);
		if(running) {
			if(velocity != 0) {
				sendMotionEvent(Common.MOT_STRAIGHT, velocity);
			} else {
				sendMotionEvent(Common.MOT_STOPPED, 0);
			}
			bti.send(BluetoothCommands.straight(velocity));
		}
	}

	protected void turn(int velocity, int angle) {
		if(running) {
			sendMotionEvent(Common.MOT_TURNING, velocity, angle);
			bti.send(BluetoothCommands.turn(velocity, angle));
		}
	}

	// Ramp linearly from min at param.SMALLTURN_ANGLE to max at param.SLOWTURN_ANGLE
	public int TurnSpeed(int angle) {
		if(angle > param.SLOWTURN_ANGLE) {
			return param.TURNSPEED_MAX;
		} else if(angle > param.SMALLTURN_ANGLE && angle <= param.SLOWTURN_ANGLE) {
			return param.TURNSPEED_MIN + (int) ((angle - param.SMALLTURN_ANGLE) * turnspeed);
		} else {
			return param.TURNSPEED_MIN;
		}
	}

	/**
	 * Slow down linearly upon coming within R_slowfwd of the goal
	 * 
	 * @param distance
	 * @return
	 */
	private int LinSpeed(int distance) {
		if(distance > param.SLOWFWD_RADIUS)
			return param.LINSPEED_MAX;
		if(distance > param.GOAL_RADIUS && distance <= param.SLOWFWD_RADIUS) {
			return param.LINSPEED_MIN + (int) ((distance - param.GOAL_RADIUS) * linspeed);
		}
		return param.LINSPEED_MIN;
	}

	// Detects an imminent collision with another robot
	private boolean collision() {
		boolean colrobot = false;
		boolean colwall = false;
		ItemPosition me = mypos;
		PositionList others = gvh.gps.getPositions();
		for(ItemPosition current : others.getList()) {
			if(!current.name.equals(me.name)) {
				if(me.isFacing(current, param.ROBOT_RADIUS) && me.distanceTo(current) <= 2 * (param.ROBOT_RADIUS)) {
					blocker = current;
					colrobot = true;
					return true;
				}
			}
		}
		ObstacleList list = gvh.gps.getObspointPositions();
		for(int i = 0; i < list.ObList.size(); i++)
		{
			Obstacles currobs = list.ObList.get(i);
			Point nextpoint = currobs.obstacle.firstElement();
			Point curpoint = currobs.obstacle.firstElement();
			ItemPosition wall = new ItemPosition("wall",0,0,0);
			
			for(int j = 0; j < currobs.obstacle.size() ; j++){
				curpoint = currobs.obstacle.get(j);
				if (j == currobs.obstacle.size() -1){
					nextpoint = currobs.obstacle.firstElement();
				}
				else{
					nextpoint = currobs.obstacle.get(j+1);
				}
				Point closeP = currobs.getClosestPointOnSegment(curpoint.x, curpoint.y, nextpoint.x, nextpoint.y, me.x, me.y);
				wall.setPos(closeP.x, closeP.y, 0);
				if((!currobs.validItemPos(me, param.ROBOT_RADIUS))&&me.isFacing(wall,param.ROBOT_RADIUS)){
					colwall = true;
					blocker = wall;
					return (colrobot || colwall);
				}
			}
		}
		return (colrobot ||colwall);

	}

	@Override
	public void setParameters(MotionParameters param) {
		this.param = param;
		this.linspeed = (double) (param.LINSPEED_MAX - param.LINSPEED_MIN) / Math.abs((param.SLOWFWD_RADIUS - param.GOAL_RADIUS));
		this.turnspeed = (param.TURNSPEED_MAX - param.TURNSPEED_MIN) / (param.SLOWTURN_ANGLE - param.SMALLTURN_ANGLE);
	}
}
