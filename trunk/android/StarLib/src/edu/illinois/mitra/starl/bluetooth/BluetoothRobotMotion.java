package edu.illinois.mitra.starl.bluetooth;

import java.util.Arrays;

import android.os.Handler;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public class BluetoothRobotMotion extends RobotMotion {
	private static final String TAG = "RobotMotion";
	private static final String ERR = "Critical Error";
	
	private static final int TURN = 60;
	private static final int SPEED = 200;
	private static final int DELAY = 50;
	
	private GlobalVarHolder gvh;
	private ItemPosition currentpos;	
	private BluetoothInterface bti;
	
	private ItemPosition blocker = null;
	private Handler handler;
	
	private boolean running = true;
	
	// Direction and distance variables
	private int angle = 0;
	private int distance = 0;
	
	public BluetoothRobotMotion(GlobalVarHolder gvh, String mac) {
		this.gvh = gvh;
		gvh.log.d(TAG, "Creating new BluetoothInterface");
		this.bti = new BluetoothInterface(gvh, mac);
		handler = new Handler();
	}

	// Roomba motion commands
	private byte[] robot_turn(int velocity, int dir) {
		sendMotionEvent(Common.MOT_TURNING, velocity, dir);
		if(dir > 0) {
			return new byte[]{(byte) 137, 0x00, (byte) velocity, (byte) 0xFF, (byte) 0xFF};
		} else {
			return new byte[]{(byte) 137, 0x00, (byte) velocity, 0x00, 0x01};
		}
	}
	
	private byte[] robot_straight(int velocity) {
		if(velocity != 0) {
			sendMotionEvent(Common.MOT_STRAIGHT, velocity);
		}
		return new byte[]{(byte) 137, 0x00, (byte) velocity, 0x7F, (byte) 0xFF};
	}
	
	private byte[] robot_curve(int velocity, int radius) {
		sendMotionEvent(Common.MOT_ARCING, velocity, radius);
		return new byte[]{(byte) 137, 0x00, (byte) velocity, (byte) ((radius & 0xFF00) >> 8), (byte) (radius & 0xFF)};
	}
	
	private byte[] robot_stop() {
		sendMotionEvent(Common.MOT_STOPPED, 0);
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
		int capacity, charge;
		capacity = Common.unsignedShortToInt(bti.sendReceive(req_sensor(26), 2));
		charge = Common.unsignedShortToInt(bti.sendReceive(req_sensor(25), 2));
		
		int retval = (int) ((100.0*charge)/capacity);
		gvh.log.d(TAG, "Battery: " + charge + " out of " + capacity + " = " + retval + "%");
		return Common.cap(retval, 0, 100);
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
	
	public void goTo(ItemPosition dest) {
		inMotion = true;
		currentpos = gvh.gps.getMyPosition();

		handler.post(new motion_go_curve(dest));
	}

	public void turnTo(ItemPosition dest) {
		turn_to(dest,true);
	}
	
	public void turn_to(ItemPosition dest, final boolean stopinMotion) {		
		inMotion = true;
		if(running && dest != null) {
			handler.post(new motion_turn(dest,stopinMotion));
		}
		if(dest == null) {
			gvh.log.e(ERR, "Tried to pass a null destination to turn_to");
		}
	}

	public void go_to(ItemPosition dest, int maxCurveAngle) {
		inMotion = true;
		currentpos = gvh.gps.getMyPosition();

		handler.post(new motion_go_curve(dest, maxCurveAngle, true));
	}	

	public void goTo(ItemPosition dest, int maxCurveAngle, boolean useCollisionAvoidance) {
		inMotion = true;
		currentpos = gvh.gps.getMyPosition();

		handler.post(new motion_go_curve(dest, maxCurveAngle, useCollisionAvoidance));
	}
	
	// Motion class implementing turning
	class motion_turn implements Runnable {
		ItemPosition dest;
		boolean stop;
		public motion_turn(ItemPosition destination, boolean stop) {
			this.dest = destination;
			this.stop = stop;
		}
		public void run() {
			if(running) {
				currentpos = gvh.gps.getMyPosition();
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
		
		private ItemPosition dest;
		private int distance = 0;
		private int angle = 0;
		private int state = -1;
		private int nextState = -1;
		private boolean colavoid = true;
		
		public motion_go_curve(ItemPosition dest) {
			this.dest = dest;
			this.state = 0;
			this.nextState = -1;
			ANGLE_WIDE = 100;
			colavoid = true;
		}
		
		public motion_go_curve(ItemPosition dest, int maxCurveAngle, boolean useCollisionAvoidance) {
			ANGLE_WIDE = maxCurveAngle;
			this.dest = dest;
			this.state = 0;
			this.nextState = -1;
			this.colavoid = useCollisionAvoidance;
		}
		
		public void run() {
			if(running) {
			currentpos = gvh.gps.getMyPosition();
			state = nextState;
			if(collision()) {
				gvh.log.d(TAG, "Collision with " + blocker);
				if(colavoid) {
					handler.post(new motion_go_colavoid(dest));
				} else {
					//gvh.d(TAG, "No avoidance, waiting for resolution...");
					bti.send(robot_stop());
					postMe(DELAY);
				}
			} else {
				//gvh.d(TAG, "Stage = " + state);
				angle = currentpos.angleTo(dest);
				distance = currentpos.distanceTo(dest);
				int azone = getAngleZone(angle);
				int dzone = getRangeZone(distance);
				
				// First run of algorithm. Analyze the current situation and begin motion accordingly
				if(state == -1) {
					state = getNextState(dzone, azone);
					//gvh.d(TAG, "FIRST RUN: analyzed current position, going to state " + state);
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
					int r = Common.cap(curveRadius(dest),-2000,2000);
					if(r < 1000) {
						r = (int) (r * .9);
					}
					//gvh.d(TAG, "Starting curve of radius " + r);
					bti.send(robot_curve(SPEED,r));
					nextState = 1;
					break;
					
				case 1:
					// Arcing in progress
					if(azone == NARROW) {
						// If we're facing the target, go straight
						//gvh.d(TAG, "Facing the goal, stopping the curve and going straight");
						bti.send(robot_straight(SPEED));
						nextState = 0;
					} else if(azone == OBTUSE) {
						// We missed! Turn and go straight to the goal
						//gvh.d(TAG, "Missed the goal! Stopping the curve and turning straight to the goal");
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
						//gvh.d(TAG, "Turning to face goal...");
						bti.send(robot_turn(TURN, angle));
						nextState = 2;
					} else {
						nextState = getNextState(dzone, azone);
						//gvh.d(TAG, "Facing goal! Next state determined to be " + nextState);
					}
					break;
					
				case 3:
					// At the goal
					nextState = 3;
					break;
				}
				
				if(state == 3 || nextState == 3) {
					//gvh.d(TAG, "Done!");
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
				gvh.log.e(ERR, "RobotMotion: curve motion function getNextState encountered an unknown angle zone: " + azone);
			}
			return 2;
		}
		
		private void postMe(int delay) {
			if(running) {
				handler.postDelayed(this, delay);
			} else {
				gvh.log.e(TAG, "Halted motion_go_curve: No longer running");
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
		ItemPosition dest;
		
		public motion_go_colavoid(ItemPosition destination, int state) {
			this.mystate = state;
			this.dest = destination;
		}
		
		public motion_go_colavoid(ItemPosition destination) {
			this.mystate = 0;
			this.dest = destination;
		}
				
		public void run() {
			currentpos = gvh.gps.getMyPosition();
			switch(mystate) {
			case 0:
				//gvh.d(TAG,"0 - Turning away from blocker!");
				// Turn to the right
				bti.send(robot_turn(100, -currentpos.angleTo(blocker)));
				handler.postDelayed(new motion_go_colavoid(dest,1), 150);
				break;
			case 1:
				// If no robot in the way, go straight
				// Otherwise, turn again
				if(collision()) {
					//gvh.d(TAG,"1 - Collision! Turning again");
					bti.send(robot_turn(100, -currentpos.angleTo(blocker)));
					handler.post(new motion_go_colavoid(dest,0));
				} else {
					//gvh.d(TAG,"1 - No collision, going straight.");
					bti.send(robot_straight(SPEED));
					handler.postDelayed(new motion_go_colavoid(dest,2),650);
				}
				break;
			case 2:
				// If we've gone straight and are in the clear, resume normal motion
				// Otherwise, turn again
				if(collision()) {
					//gvh.d(TAG,"2 - Collision! Turning again");
					bti.send(robot_turn(100, -currentpos.angleTo(blocker)));
					handler.postDelayed(new motion_go_colavoid(dest,1), 150);					
				} else {
					//gvh.d(TAG,"2 - Free! Resuming normal motion");
					handler.post(new motion_go_curve(dest));
				}
			}
		}	
	}
	
	// Detects an imminent collision with another robot
	private boolean collision() {
		ItemPosition me = gvh.gps.getMyPosition();
		PositionList others = gvh.gps.getPositions();
		for(ItemPosition current : others.getList()) {
			if(!current.getName().equals(me.getName())) {
				if(me.isFacing(current, 160) && me.distanceTo(current) < 450) {
					blocker = current;
					return true;
				}
			}
		}
		blocker = null;
		return false;
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
	
	private void sendMotionEvent(int motiontype, int ... argument) {
		gvh.trace.traceEvent(TAG, "Motion", Arrays.asList(argument).toString());
		gvh.sendRobotEvent(Common.EVENT_MOTION, motiontype);
	}
	
	// Calculates the radius of curvature to meet a target
	private int curveRadius(ItemPosition destination) {
		int x0 = gvh.gps.getMyPosition().getX();
		int y0 = gvh.gps.getMyPosition().getY();
		int x1 = destination.getX();
		int y1 = destination.getY();
		int theta = gvh.gps.getMyPosition().getAngle();
			
		double alpha = -180+Math.toDegrees(Math.atan2((y1-y0),(x1-x0)));

		double rad = -(Math.sqrt(Math.pow(x1-x0,2) + Math.pow(y1-y0,2))/(2*Math.sin(Math.toRadians(alpha-theta))));

		return (int) rad;
	}
}
