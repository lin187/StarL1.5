package edu.illinois.mitra.starl.bluetooth;

import java.util.HashSet;

import android.os.Handler;
import android.util.Log;
import edu.illinois.mitra.starl.interfaces.MotionEventProvider;
import edu.illinois.mitra.starl.interfaces.MotionListener;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.globalVarHolder;
import edu.illinois.mitra.starl.objects.itemPosition;
import edu.illinois.mitra.starl.objects.positionList;

public class RobotMotion implements MotionEventProvider {
	private static final String TAG = "RobotMotion";
	private static final String ERR = "Critical Error";
	
	private static final int TURN = 60;
	private static final int SPEED = 200;
	private static final int DELAY = 50;
	
	private globalVarHolder gvh;
	private itemPosition currentpos;	
	private BluetoothInterface bti;
	
	private itemPosition blocker = null;
	private Handler handler;
	
	private HashSet<MotionListener> listeners;
	
	private boolean running = true;
	
	public boolean inMotion = false;
	
	// Direction and distance variables
	private int angle = 0;
	private int distance = 0;
	
	public RobotMotion(globalVarHolder gvh, String mac) {
		this.gvh = gvh;
		this.bti = new BluetoothInterface(gvh, this);
		bti.connect(mac);
		handler = new Handler();
		listeners = new HashSet<MotionListener>();
	}

	// Roomba motion commands
	private byte[] robot_turn(int velocity, int dir) {
		sendMotionEvent(common.MOT_TURNING);
		if(dir > 0) {
			return new byte[]{(byte) 137, 0x00, (byte) velocity, (byte) 0xFF, (byte) 0xFF};
		} else {
			return new byte[]{(byte) 137, 0x00, (byte) velocity, 0x00, 0x01};
		}
	}
	
	private byte[] robot_straight(int velocity) {
		if(velocity != 0) {
			sendMotionEvent(common.MOT_STRAIGHT);
		}
		return new byte[]{(byte) 137, 0x00, (byte) velocity, 0x7F, (byte) 0xFF};
	}
	
	private byte[] robot_curve(int velocity, int radius) {
		sendMotionEvent(common.MOT_ARCING);
		return new byte[]{(byte) 137, 0x00, (byte) velocity, (byte) ((radius & 0xFF00) >> 8), (byte) (radius & 0xFF)};
	}
	
	private byte[] robot_stop() {
		sendMotionEvent(common.MOT_STOPPED);
		return robot_straight(0);
	}
	
	private byte[] robot_play_song(int song) {
		return new byte[]{(byte) 141, (byte) song};
	}
	
	//Turns on one of the three LEDs, specified by an int 0->2
	private byte[] robot_led(int led) {
		byte led_state = (byte) (led!=0?(6*led-4):0x00);
		return new byte[]{(byte) 139, led_state, 0x00, (byte) (led==0?0xFF:0x00)};
	}

	private byte[] req_sensor(int packetID) {
		return new byte[]{(byte) 142, (byte) packetID};
	}
	
	public int getBatteryPercentage() {
		int capacity = -1;
		int current = 0;
		int attempts = 0;
		
		while((capacity < current) && attempts < 30) {
			bti.send(req_sensor(26));
			capacity = unsignedShortToInt(bti.readBuffer(2));
			
			bti.send(req_sensor(25));
			current = unsignedShortToInt(bti.readBuffer(2));
			
			Log.d(TAG, "Read from buffer: " + capacity + " and " + current);
			attempts ++;
		}
		current = common.cap(current, 0, capacity);
		return (int) Math.round((new Float(current)/capacity)*100.0);
	}
	
	public void song() {
		bti.send(robot_play_song(0));
	}
	
	public void song(int num) {
		bti.send(robot_play_song(num));
	}
	
	public void blink(int led) {
		bti.send(robot_led(led));
	}
	
	public void turn_to(itemPosition dest) {
		turn_to(dest,true);
	}
	
	public void turn_to(itemPosition dest, final boolean stopinMotion) {		
		inMotion = true;
		if(running && dest != null) {
			handler.post(new motion_turn(dest,stopinMotion));
		}
		if(dest == null) {
			Log.e(ERR, "Tried to pass a null destination to turn_to");
		}
	}

	public void go_to(itemPosition dest) {
		inMotion = true;
		currentpos = gvh.getMyPosition();

		handler.post(new motion_go_curve(dest));
	}

	public void go_to(itemPosition dest, int maxCurveAngle) {
		inMotion = true;
		currentpos = gvh.getMyPosition();

		handler.post(new motion_go_curve(dest, maxCurveAngle, true));
	}
	
	public void go_to(itemPosition dest, int maxCurveAngle, boolean useCollisionAvoidance) {
		inMotion = true;
		currentpos = gvh.getMyPosition();

		handler.post(new motion_go_curve(dest, maxCurveAngle, useCollisionAvoidance));
	}
	
	// Motion class implementing turning
	class motion_turn implements Runnable {
		itemPosition dest;
		boolean stop;
		public motion_turn(itemPosition destination, boolean stop) {
			this.dest = destination;
			this.stop = stop;
		}
		public void run() {
			if(running) {
				currentpos = gvh.getMyPosition();
				angle = currentpos.angleTo(dest);
				distance = currentpos.distanceTo(dest);
				if(Math.abs(angle) > 5 && distance > 100) {
					distance = currentpos.distanceTo(dest);
					bti.send(robot_turn(80, angle));
					handler.postDelayed(new motion_turn(dest,stop), 50);
				} else {
					bti.send(robot_stop());
					if(stop) {
						inMotion = false;
					}
				}
			}
		}
	}
	
	// Motion class to intelligently drive in curves
	class motion_go_curve implements Runnable {
		private static final int THRESH_GOAL = 110;
		private static final int THRESH_SHORT = 800;
		private static final int THRESH_MED = 1500;
		
		private static final int ANGLE_NARROW = 3;
		private static final int ANGLE_SMALL = 7;
		private int ANGLE_WIDE = 100;
		
		private static final int GOAL = 0;
		private static final int SHORT = 1;
		private static final int MED = 2;
		private static final int LONG = 3;
		
		private static final int NARROW = 0;
		private static final int SMALL = 1;
		private static final int WIDE = 2;
		private static final int OBTUSE = 3;
		
		private itemPosition dest;
		private int distance = 0;
		private int angle = 0;
		private int state = -1;
		private int nextState = -1;
		private boolean colavoid = true;
		
		public motion_go_curve(itemPosition dest) {
			this.dest = dest;
			this.state = 0;
			this.nextState = -1;
			ANGLE_WIDE = 100;
			colavoid = true;
		}
		
		public motion_go_curve(itemPosition dest, int maxCurveAngle, boolean useCollisionAvoidance) {
			ANGLE_WIDE = maxCurveAngle;
			this.dest = dest;
			this.state = 0;
			this.nextState = -1;
			this.colavoid = useCollisionAvoidance;
		}
		
		public void run() {
			if(running) {
			currentpos = gvh.getMyPosition();
			state = nextState;
			if(collision()) {
				Log.d(TAG, "Collision with " + blocker);
				if(colavoid) {
					handler.post(new motion_go_colavoid(dest));
				} else {
					//Log.d(TAG, "No avoidance, waiting for resolution...");
					bti.send(robot_stop());
					postMe(DELAY);
				}
			} else {
				//Log.d(TAG, "Stage = " + state);
				angle = currentpos.angleTo(dest);
				distance = currentpos.distanceTo(dest);
				int azone = getAngleZone(angle);
				int dzone = getRangeZone(distance);
				
				// First run of algorithm. Analyze the current situation and begin motion accordingly
				if(state == -1) {
					state = getNextState(dzone, azone);
					//Log.d(TAG, "FIRST RUN: analyzed current position, going to state " + state);
				}
				
				// If we're within close range, we're at the goal
				if(dzone == GOAL) {
					nextState = 3;
				}
				
				switch(state) {
				case 0:
					// Go straight
					bti.send(robot_straight(SPEED));
					nextState = getNextState(dzone, azone);
					break;
					
				case 11:
					// Start arcing
					int r = common.cap(curveRadius(dest),-2000,2000);
					if(r < 1000) {
						r = (int) (r * .9);
					}
					//Log.d(TAG, "Starting curve of radius " + r);
					bti.send(robot_curve(SPEED,r));
					nextState = 1;
					break;
					
				case 1:
					// Arcing in progress
					if(azone == NARROW) {
						// If we're facing the target, go straight
						//Log.d(TAG, "Facing the goal, stopping the curve and going straight");
						bti.send(robot_straight(SPEED));
						nextState = 0;
					} else if(azone == OBTUSE) {
						// We missed! Turn and go straight to the goal
						//Log.d(TAG, "Missed the goal! Stopping the curve and turning straight to the goal");
						bti.send(robot_turn(TURN, angle));
						nextState = 2;
					} else {
						nextState = 1;
					}
					if(dzone == GOAL) {
						// At destination, stop moving
						nextState = 3;
					}
					break;
					
				case 2:
					// Turn to face goal
					if(getAngleZone(angle) != NARROW) {
						//Log.d(TAG, "Turning to face goal...");
						bti.send(robot_turn(TURN, angle));
						nextState = 2;
					} else {
						nextState = getNextState(dzone, azone);
						//Log.d(TAG, "Facing goal! Next state determined to be " + nextState);
					}
					break;
					
				case 3:
					// At the goal
					nextState = 3;
					break;
				}
				
				if(state == 3 || nextState == 3) {
					//Log.d(TAG, "Done!");
					bti.send(robot_stop());
					inMotion = false;
				} else {
					postMe(DELAY);
				}
			}
			}
		}		
		
		private int getNextState(int dzone, int azone) {
			if(dzone == GOAL) {
				return 3;
			}
			switch(azone) {
			case NARROW:
				// Go straight towards destination
				return 0;
			case SMALL:
				// Arc to destination
				return 11;
			case WIDE:
				// Arc to destination
				return 11;				
			case OBTUSE:
				// Turn to within NARROW and proceed straight
				return 2;
			default:
				Log.e(ERR, "RobotMotion: curve motion function getNextState encountered an unknown angle zone: " + azone);
			}
			return 2;
		}
		
		private void postMe(int delay) {
			if(running) {
				handler.postDelayed(this, delay);
			} else {
				Log.e(TAG, "Halted motion_go_curve: No longer running");
			}
		}
		
		private int getRangeZone(int distance) {
			if(distance <= THRESH_GOAL) {
				return GOAL;
			} else if(distance <= THRESH_SHORT) {
				return SHORT;
			} else if(distance <= THRESH_MED) {
				return MED;				
			} else {
				return LONG;
			}
		}
		
		private int getAngleZone(int angle) {
			int absangle = Math.abs(angle);
			if(absangle <= ANGLE_NARROW) {
				return NARROW;
			} else if(absangle <= ANGLE_SMALL) {
				return SMALL;
			} else if(absangle <= ANGLE_WIDE) {
				return WIDE;				
			} else {
				return OBTUSE;
			}
		}
	}
		
	// Motion class to handle imminent collisions
	class motion_go_colavoid implements Runnable {
		int mystate = 0;
		itemPosition dest;
		
		public motion_go_colavoid(itemPosition destination, int state) {
			this.mystate = state;
			this.dest = destination;
		}
		
		public motion_go_colavoid(itemPosition destination) {
			this.mystate = 0;
			this.dest = destination;
		}
				
		public void run() {
			currentpos = gvh.getMyPosition();
			switch(mystate) {
			case 0:
				//Log.d(TAG,"0 - Turning away from blocker!");
				// Turn to the right
				bti.send(robot_turn(100, -currentpos.angleTo(blocker)));
				handler.postDelayed(new motion_go_colavoid(dest,1), 150);
				break;
			case 1:
				// If no robot in the way, go straight
				// Otherwise, turn again
				if(collision()) {
					//Log.d(TAG,"1 - Collision! Turning again");
					bti.send(robot_turn(100, -currentpos.angleTo(blocker)));
					handler.post(new motion_go_colavoid(dest,0));
				} else {
					//Log.d(TAG,"1 - No collision, going straight.");
					bti.send(robot_straight(SPEED));
					handler.postDelayed(new motion_go_colavoid(dest,2),650);
				}
				break;
			case 2:
				// If we've gone straight and are in the clear, resume normal motion
				// Otherwise, turn again
				if(collision()) {
					//Log.d(TAG,"2 - Collision! Turning again");
					bti.send(robot_turn(100, -currentpos.angleTo(blocker)));
					handler.postDelayed(new motion_go_colavoid(dest,1), 150);					
				} else {
					//Log.d(TAG,"2 - Free! Resuming normal motion");
					handler.post(new motion_go_curve(dest));
				}
			}
		}	
	}
	
	// Detects an imminent collision with another robot
	private boolean collision() {
		itemPosition me = gvh.getMyPosition();
		positionList others = gvh.getPositions();
		for(itemPosition current : others.getList()) {
			if(!current.getName().equals(me.getName())) {
				if(me.isFacing(current, 120) && me.distanceTo(current) < 450) {
					blocker = current;
					return true;
				}
			}
		}
		blocker = null;
		return false;
	}
	
	// Calculates the radius of curvature to meet a target
	private int curveRadius(itemPosition destination) {
		int x0 = gvh.getMyPosition().getX();
		int y0 = gvh.getMyPosition().getY();
		int x1 = destination.getX();
		int y1 = destination.getY();
		int theta = gvh.getMyPosition().getAngle();
			
		double alpha = -180+Math.toDegrees(Math.atan2((y1-y0),(x1-x0)));

		double rad = -(Math.sqrt(Math.pow(x1-x0,2) + Math.pow(y1-y0,2))/(2*Math.sin(Math.toRadians(alpha-theta))));

		return (int) rad;
	}
	
	/**
	 * Converts a two byte array to an integer
	 * @param b a byte array of length 2
	 * @return an int representing the unsigned short
	 */
	private static final int unsignedShortToInt(byte[] b) 
	{
		if(b.length != 2){
			return -99;
		}
	    int i = 0;
	    i |= b[0] & 0xFF;
	    i <<= 8;
	    i |= b[1] & 0xFF;
	    return i;
	}

	public void cancel() {
		halt();
		bti.disconnect();		
	}
	
	public void halt() {
		running = false;
		stop();
	}
	
	public void stop() {
		handler.removeCallbacksAndMessages(null);
		bti.send(robot_stop());
	}	
	
	public void resume() {
		running = true;
	}

	@Override
	public void addMotionListener(MotionListener l) {
		listeners.add(l);		
	}

	@Override
	public void removeMotionListener(MotionListener l) {
		listeners.remove(l);
	}
	
	private void sendMotionEvent(int e) {
		for(MotionListener l : listeners) {
			l.motionEvent(e);
		}
	}
}
