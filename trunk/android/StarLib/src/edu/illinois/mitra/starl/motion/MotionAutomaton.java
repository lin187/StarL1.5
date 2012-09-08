package edu.illinois.mitra.starl.motion;

import java.util.Arrays;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.RobotEventListener.Event;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;

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
	public static int R_arc = 700;
	public static int R_goal = 75;
	public static int R_slowfwd = 700;
	public static int A_smallturn = 3;
	public static int A_straight = 6;
	public static int A_arc = 25;
	public static int A_arcexit = 30;

	public static final int A_slowturn = 25;

	public static final int ROBOT_RADIUS = 180; // TODO: put this in SimSettings
												// and make consistent (mod a
												// scaling factor): this is
												// another (different valued)
												// robot radius

	// DELAY BETWEEN EACH RUN OF THE AUTOMATON
	public static final int DELAY_TIME = 60;
	public static final int SAMPLING_PERIOD = 300;

	// COLLISION AVOIDANCE CONSTANTS
	public static final int COLLISION_STRAIGHTTIME = 1250;

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

	private MotionParameters param;
	private static final MotionParameters DEFAULT_PARAMETERS = new MotionParameters();

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
		this.param = DEFAULT_PARAMETERS;
		this.linspeed = (this.param.LINSPEED_MAX - this.param.LINSPEED_MIN) / (1.0 * (R_slowfwd - R_goal));
		this.turnspeed = (this.param.TURNSPEED_MAX - this.param.TURNSPEED_MIN) / (A_slowturn - A_smallturn);
	}

	public void goTo(ItemPosition dest) {
		goTo(dest, DEFAULT_PARAMETERS);
	}

	public void goTo(ItemPosition dest, MotionParameters param) {
		if((inMotion && !this.destination.equals(dest)) || !inMotion) {
			this.destination = dest;
			this.param = param;
			this.mode = OPMODE.GO_TO;
			startMotion();
		}
	}

	public void turnTo(ItemPosition dest) {
		turnTo(dest, DEFAULT_PARAMETERS);
	}

	public void turnTo(ItemPosition dest, MotionParameters param) {
		if((inMotion && !this.destination.equals(dest)) || !inMotion) {
			this.destination = dest;
			this.param = param;
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
							if(distance <= R_goal) {
								next = STAGE.GOAL;
							} else if(distance <= R_arc && absangle <= param.ARCANGLE_MAX && param.ENABLE_ARCING) {
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
							if(absangle > A_arcexit)
								next = STAGE.TURN;
							if(absangle < A_straight)
								next = STAGE.STRAIGHT;
							if(distance <= R_goal)
								next = STAGE.GOAL;
						}
						break;
					case STRAIGHT:
						if(stage != prev) {
							straight(LinSpeed(distance));
						} else {
							if(Common.inRange(distance, R_goal, R_slowfwd))
								straight(LinSpeed(distance));
							if(Common.inRange(absangle, A_smallturn, param.ARCANGLE_MAX))
								next = STAGE.SMALLTURN;
							if(absangle > param.ARCANGLE_MAX)
								next = STAGE.TURN;
							if(distance <= R_goal)
								next = STAGE.GOAL;
						}
						break;
					case TURN:
						if(stage != prev) {
							turn(TurnSpeed(absangle), angle);
						} else {
							if(absangle <= A_smallturn) {
								gvh.log.i(TAG, "Turn stage: within angle bounds!");
								next = (mode == OPMODE.GO_TO) ? STAGE.STRAIGHT : STAGE.GOAL;
							} else if(absangle <= A_slowturn) {
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
							if(absangle <= A_smallturn)
								next = STAGE.STRAIGHT;
							if(absangle > param.ARCANGLE_MAX)
								next = STAGE.TURN;
							if(distance <= R_goal)
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
						gvh.trace.traceEvent(TAG, "Stage transition", stage.toString());
					}
					next = null;
					// gvh.sleep(DELAY_TIME);
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
							col_straightime += DELAY_TIME;
							// If a collision is imminent (again), return to the
							// turn stage
							if(collision()) {
								gvh.log.d(TAG, "Collision imminent! Cancelling straight stage");
								straight(0);
								colnext = COLSTAGE.TURN;
							}
							// If we're collision free and have been for enough
							// time, restart normal motion
							if(!collision() && col_straightime >= COLLISION_STRAIGHTTIME) {
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
						gvh.trace.traceEvent(TAG, "Halting motion");
						straight(0);
						stage = STAGE.INIT;
					}
				}
			}
			gvh.sleep(DELAY_TIME);
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
		// TODO: Is this needed??? Or even working properly??
		gvh.trace.traceEvent(TAG, "Motion", Arrays.asList(argument).toString());
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

	// Ramp linearly from min at a_smallturn to max at a_slowturn
	public int TurnSpeed(int angle) {
		if(angle > A_slowturn) {
			return param.TURNSPEED_MAX;
		} else if(angle > A_smallturn && angle <= A_slowturn) {
			return param.TURNSPEED_MIN + (int) ((angle - A_smallturn) * turnspeed);
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
		if(distance > R_slowfwd)
			return param.LINSPEED_MAX;
		if(distance > R_goal && distance <= R_slowfwd) {
			return param.LINSPEED_MIN + (int) ((distance - R_goal) * linspeed);
		}
		return param.LINSPEED_MIN;
	}

	// Detects an imminent collision with another robot
	private boolean collision() {
		ItemPosition me = mypos;
		PositionList others = gvh.gps.getPositions();
		for(ItemPosition current : others.getList()) {
			if(!current.name.equals(me.name)) {
				if(me.isFacing(current, ROBOT_RADIUS) && me.distanceTo(current) < 2 * ROBOT_RADIUS) {
					blocker = current;
					return true;
				}
			}
		}
		blocker = null;
		return false;
	}

	@Override
	public void setParameters(MotionParameters param) {
		this.param = param;
		this.linspeed = (param.LINSPEED_MAX - param.LINSPEED_MIN) / (1.0 * (R_slowfwd - R_goal));
		this.turnspeed = (param.TURNSPEED_MAX - param.TURNSPEED_MIN) / (A_slowturn - A_smallturn);
	}
}
