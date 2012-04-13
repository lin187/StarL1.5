package edu.illinois.mitra.starl.bluetooth;

import java.util.Arrays;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

/**
 * iRobot Create specific. Experimental motion automaton set to replace BluetoothInterface once it's fully tested.
 * @author Adam Zimmerman
 * @version 1.1
 * @see BluetoothRobotMotion
 */
public class MotionAutomaton extends Thread implements Cancellable {
	private static final String TAG = "MotionAutomaton";
	private static final String ERR = "Critical Error";
	
	// MOTION CONTROL CONSTANTS
	public static final int R_arc = 700;
	public static final int R_goal = 100;
	public static final int A_smallturn = 2;
	public static final int A_straight = 5;
	public static final int A_arc = 25;
	public static final int A_arcexit = 30;
	
	private static final int VELOCITY = 200;
	private static final int TURNSPEED = 60;
	
	// DELAY BETWEEN EACH RUN OF THE AUTOMATON
	public static final int DELAY_TIME = 60;
	public static final int SAMPLING_PERIOD = 300;
		
	// COLLISION AVOIDANCE CONSTANTS
	public static final int COLLISION_STRAIGHTTIME = 1000;
	
	private GlobalVarHolder gvh;
	private BluetoothInterface bti;
	private DeadReckoner reckoner;
	
	// Motion tracking
	private ItemPosition destination;
	private ItemPosition mypos;
	private ItemPosition blocker;
	private enum STAGE {
		INIT, ARCING, STRAIGHT, TURN, SMALLTURN, GOAL
	}
	private STAGE next = null;
	private STAGE stage = STAGE.INIT;
	private STAGE prev = null;
	private boolean running = false;
	private enum OPMODE {
		GO_TO, TURN_TO
	}
	private OPMODE mode = OPMODE.GO_TO;
	public boolean inMotion;
	
	// Collision avoidance
	private enum COLSTAGE {
		TURN, STRAIGHT
	}
	private COLSTAGE colprev = null; 
	private COLSTAGE colstage = COLSTAGE.TURN;
	private COLSTAGE colnext = null; 
	private int col_straightime = 0;

	public MotionAutomaton(GlobalVarHolder gvh, BluetoothInterface bti) {
		super();
		this.gvh = gvh;
		this.bti = bti;
		reckoner = new DeadReckoner(gvh, bti, SAMPLING_PERIOD);
	}
	
	public void GoTo(ItemPosition dest) {
		destination = dest;
		mode = OPMODE.GO_TO;
		startMotion();
	}
	
	public void TurnTo(ItemPosition dest) {
		destination = dest;
		mode = OPMODE.TURN_TO;
		startMotion();
	}
	
	@Override
	public synchronized void start() {
		super.start();
		reckoner.start();
	}
	
	private void startMotion() {
		running = true;
		stage = STAGE.INIT;
		inMotion = true;
	}
	
	@Override
	public void run() {
		//super.run();
		while(true) {
		while(running) {
			// TODO: Dead reckoner is dead wrong.
			reckoner.getLatestEstimate();
			mypos = gvh.gps.getMyPosition();//reckoner.getLatestEstimate();
			int distance = mypos.distanceTo(destination);
			int angle = mypos.angleTo(destination);
			int absangle = Math.abs(angle);
			
			if(!collision() && stage != null) {
				switch(stage) {
				case INIT:
					if(mode == OPMODE.GO_TO) {
						if(distance <= R_goal) {
							next = STAGE.GOAL;
						} else if(distance <= R_arc && angle <= A_arc) {
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
						curve(VELOCITY,radius);
					} else {
						// Otherwise, check exit conditions
						if(absangle > A_arcexit) next = STAGE.TURN;					
						if(absangle < A_straight) next = STAGE.STRAIGHT;
						if(distance <= R_goal) next = STAGE.GOAL;
					}
					break;
				case STRAIGHT:
					if(stage != prev) {
						straight(VELOCITY);
					} else {
						if(Common.inRange(absangle, A_smallturn, A_arc)) next = STAGE.SMALLTURN;
						if(absangle > A_arc) next = STAGE.TURN;
						if(distance <= R_goal) next = STAGE.GOAL;
					}
					break;
				case TURN:
					if(stage != prev) {
						turn(TURNSPEED, angle);
					} else {
						if(absangle <= A_smallturn) {
							gvh.log.i(TAG, "Turn stage: within angle bounds!");
							next = (mode == OPMODE.GO_TO) ? STAGE.STRAIGHT : STAGE.GOAL;
						}
					}
					break;
				case SMALLTURN:
					if(stage != prev) {
						int radius = curveRadius()/2;
						curve(VELOCITY, radius);
					} else {
						if(absangle <= A_smallturn) stage = STAGE.STRAIGHT;
						if(distance <= R_goal) stage = STAGE.GOAL;
					}
					break;
				case GOAL:
					gvh.log.i(TAG, "At goal!");
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
				sleep(DELAY_TIME);
			} else {
				// Collision imminent! Stop the robot
				if(stage != null) {
					gvh.log.d(TAG, "Imminent collision detected!");
					stage = null;
					straight(0);
					colstage = COLSTAGE.TURN;
				}
				
				colprev = colstage;
				if(colnext != null) {
					colstage = colnext;
				}
				colnext = null;
				
				switch(colstage) {
				case TURN:
					if(colstage != colprev) {
						col_straightime = 0;
						turn(TURNSPEED, (int)Math.copySign(1, -mypos.angleTo(blocker)));
					}
					
					if(!collision()) colnext = COLSTAGE.STRAIGHT;
					break;
				case STRAIGHT:
					if(colstage != colprev) {
						straight(VELOCITY);
					}
					col_straightime += DELAY_TIME;
					
					// If a collision is imminent (again), return to the turn stage
					if(collision()) {
						straight(0);
						colnext = COLSTAGE.TURN;
					}
					// If we're collision free and have been for enough time, restart normal motion
					if(!collision() && col_straightime >= COLLISION_STRAIGHTTIME) {
						colprev = null;
						colnext = null;
						colstage = COLSTAGE.TURN;
						stage = STAGE.INIT;
					}
					break;
				}
				sleep(DELAY_TIME);
			}
		}
		sleep(DELAY_TIME);
		}
	}
	
	public void cancel() {
		running = false;
		bti.disconnect();
	}
	
	public void motionHalt() {
		bti.send(BluetoothCommands.straight(0));
		running = false;
	}
	
	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	// Calculates the radius of curvature to meet a target
	private int curveRadius() {
		int x0 = mypos.x;
		int y0 = mypos.y;
		int x1 = destination.x;
		int y1 = destination.y;
		int theta = mypos.angle;
		double alpha = -180+Math.toDegrees(Math.atan2((y1-y0),(x1-x0)));
		double rad = -(Math.sqrt(Math.pow(x1-x0,2) + Math.pow(y1-y0,2))/(2*Math.sin(Math.toRadians(alpha-theta))));
		return (int) rad;
	}
	
	private void sendMotionEvent(int motiontype, int ... argument) {
		gvh.trace.traceEvent(TAG, "Motion", Arrays.asList(argument).toString());
		gvh.sendRobotEvent(Common.EVENT_MOTION, motiontype);
	}
	
	private void curve(int velocity, int radius) {
		if(running) {
			sendMotionEvent(Common.MOT_ARCING, velocity, radius);
			bti.send(BluetoothCommands.curve(velocity, radius));
		}
	}
	
	private void straight(int velocity) {
		if(running) {
			if(velocity != 0) {
				sendMotionEvent(Common.MOT_STRAIGHT, velocity);
			} else {
				sendMotionEvent(Common.MOT_STOPPED, 0);
			}
			bti.send(BluetoothCommands.straight(velocity));
		}
	}
	
	private void turn(int velocity, int angle) {
		if(running) {
			sendMotionEvent(Common.MOT_TURNING, velocity, angle);
			bti.send(BluetoothCommands.turn(velocity, angle));
		}
	}
	
	// Detects an imminent collision with another robot
	private boolean collision() {
		// TODO: See if estimated position or true position is better here
		ItemPosition me = mypos; //gvh.getMyPosition();
		PositionList others = gvh.gps.getPositions();
		for(ItemPosition current : others.getList()) {
			if(!current.name.equals(me.name)) {
				if(me.isFacing(current, 160) && me.distanceTo(current) < 450) {
					blocker = current;
					return true;
				}
			}
		}
		blocker = null;
		return false;
	}
}
