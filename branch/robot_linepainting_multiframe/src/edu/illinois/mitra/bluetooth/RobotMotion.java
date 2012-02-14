package edu.illinois.mitra.bluetooth;

import java.util.Arrays;

import android.os.Handler;
import android.util.Log;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.Objects.itemPosition;
import edu.illinois.mitra.Objects.positionList;

public class RobotMotion {
	private static final String TAG = "RobotMotion";
	private static final String ERR = "Critical Error";
	
	private globalVarHolder gvh;
	private itemPosition currentpos;	
	private itemPosition destination;
	private BluetoothInterface bti;
	
	private Handler handler;
	
	public boolean inMotion = false;
	private boolean running = true;
	
	// Direction and distance variables
	private int angle = 0;
	private int distance = 0;
	
	public RobotMotion(globalVarHolder gvh, String mac) {
		this.gvh = gvh;
		this.bti = new BluetoothInterface(gvh);
		bti.connect(mac);
		handler = new Handler();
	}

	// Roomba motion commands
	private byte[] robot_turn(int velocity, int dir) {
		if(dir > 0) {
			return new byte[]{(byte) 137, 0x00, (byte) velocity, (byte) 0xFF, (byte) 0xFF};
		} else {
			return new byte[]{(byte) 137, 0x00, (byte) velocity, 0x00, 0x01};
		}
	}
	
	private byte[] robot_straight(int velocity) {
		return new byte[]{(byte) 137, 0x00, (byte) velocity, 0x7F, (byte) 0xFF};
	}
	
	private byte[] robot_curve(int velocity, int radius) {
		radius = (int) Math.copySign((15-Math.abs(radius))*100,-radius);
		return new byte[]{(byte) 137, 0x00, (byte) velocity, (byte) ((radius & 0xFF00) >> 8), (byte) (radius & 0xFF)};
	}
	
	private byte[] robot_stop() {
		return robot_straight(0);
	}
	
	private byte[] robot_play_song(int song) {
		return new byte[]{(byte) 141, (byte) song};
	}
	
	//The turn angle function is currently very imprecise, the wheel encoders could be the cause
	private byte[] robot_turn_angle(int angle, int velocity) {
		if(angle > 0) {		//		SCRIPT			|			TURN											|		WAIT ANGLE						 						  |		STOP
			return new byte[]{(byte) 152, (byte) 13, (byte) 137, 0x00, (byte) velocity, (byte) 0x00, (byte) 0x01, (byte) 157, (byte) ((angle & 0xFF00) >> 8), (byte) (angle & 0xFF), (byte) 137, 0x00, 0x00, 0x7F, (byte) 0xFF, (byte) 153};
		} else {
			return new byte[]{(byte) 152, (byte) 13, (byte) 137, 0x00, (byte) velocity, (byte) 0xFF, (byte) 0xFF, (byte) 157, (byte) ((angle & 0xFF00) >> 8), (byte) (angle & 0xFF), (byte) 137, 0x00, 0x00, 0x7F, (byte) 0xFF,(byte) 153};
		}
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
		
		while(capacity < current) {
			bti.send(req_sensor(26));
			capacity = unsignedShortToInt(bti.readBuffer(2));
			
			bti.send(req_sensor(25));
			current = unsignedShortToInt(bti.readBuffer(2));
			
			Log.d(TAG, "Read from buffer: " + capacity + " and " + current);
		}
		
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
		this.destination = dest;
		if(running) {
			handler.post(new motion_turn(dest,stopinMotion));
		}
	}
	
	public void go_to(itemPosition dest) {
		inMotion = true;
		this.destination = dest;
		currentpos = gvh.getMyPosition();
		turn_to(destination, false);

		handler.post(new motion_go(dest));
	}
	
	public void cancel() {
		running = false;
		//handler = null;
		bti.send(robot_stop());
		bti.disconnect();		
	}
	
	// Motion class implementing turning
	class motion_turn implements Runnable {
		itemPosition destination;
		boolean stop;
		public motion_turn(itemPosition destination, boolean stop) {
			this.destination = destination;
			this.stop = stop;
		}
		public void run() {
			currentpos = gvh.getMyPosition();
			angle = currentpos.angleTo(destination);
			distance = currentpos.distanceTo(destination);
			if(Math.abs(angle) > 5 && distance > 100) {
				distance = currentpos.distanceTo(destination);
				bti.send(robot_turn(80, angle));
				handler.postDelayed(new motion_turn(destination,stop), 50);
			} else {
				bti.send(robot_stop());
				if(stop) {
					inMotion = false;
				}
			}
		}
	}
	// Motion class implements forward motion
	class motion_go implements Runnable {
		itemPosition destination;
		public motion_go(itemPosition destination) {
			this.destination = destination;
		}
		public void run() {
			// TODO Determine if this line is even necessary:
			currentpos = gvh.getMyPosition();
			angle = currentpos.angleTo(destination);
			distance = currentpos.distanceTo(destination);
			if(distance > 100) {
				if(Math.abs(angle) > 10) {
					// Too far off the path, stop and turn
					bti.send(robot_turn(60, angle));
				} else if(Math.abs(angle) > 2) {
					// Off path, curve back to the path
					bti.send(robot_curve(200,angle));
				} else {
					// On path, go straight if no collision
					if(!collision()) {
						bti.send(robot_straight(200));
					} else {
						bti.send(robot_stop());
						handler.post(new motion_go_colavoid(destination,0));
						return;
					}
				}
				handler.postDelayed(this, 50);
			} else {
				bti.send(robot_stop());
				inMotion = false;
			}
		}

	}
	
	// Motion class to handle imminent collisions
	class motion_go_colavoid implements Runnable {
		int mystate = 0;
		itemPosition dest;
		globalVarHolder gvh;
		
		public motion_go_colavoid(itemPosition destination, int state) {
			this.mystate = state;
			this.dest = destination;
		}
				
		public void run() {
			switch(mystate) {
			case 0:
				Log.d(TAG,"0 - Turning to the right");
				// Turn to the right
				bti.send(robot_turn(80, 1));
				handler.postDelayed(new motion_go_colavoid(dest,1), 150);
				break;
			case 1:
				// If no robot in the way, go straight
				// Otherwise, turn again
				if(collision()) {
					Log.d(TAG,"1 - Collision! Turning to the right again");
					handler.post(new motion_go_colavoid(dest,0));
				} else {
					Log.d(TAG,"1 - No collision, going straight.");
					bti.send(robot_straight(200));
					handler.postDelayed(new motion_go_colavoid(dest,2),1250);
				}
				break;
			case 2:
				// If we've gone straight and are in the clear, resume normal motion
				// Otherwise, turn again
				if(collision()) {
					Log.d(TAG,"2 - Collision! Turning to the right again");
					bti.send(robot_turn(80, 1));
					handler.postDelayed(new motion_go_colavoid(dest,1), 150);					
				} else {
					Log.d(TAG,"2 - Free! Resuming normal motion");
					handler.post(new motion_go(dest));
				}
			}
		}	
	}
	
	// Detects an imminent collision with another robot
	private boolean collision() {
		itemPosition me = gvh.getMyPosition();
		positionList others = gvh.getPositions();
		for(int i = 0; i < others.getNumPositions(); i ++) {
			itemPosition current = others.getPositionAtIndex(i);
			if(!current.getName().equals(me.getName())) {
				if(me.isFacing(current, 180) && me.distanceTo(current) < 500) {
					return true;
				}
			}
		}
		return false;
	}
	
	// Calculates the radius of curvature to meet a target
	private int curveRadius(itemPosition destination) {
		int x0 = gvh.getMyPosition().getX();
		int y0 = gvh.getMyPosition().getY();
		int x1 = destination.getX();
		int y1 = destination.getY();
		int theta = gvh.getMyPosition().getAngle();
		
		double alpha = Math.atan((y1-y0)/(x1-x0));

		return (int) Math.round(Math.abs(Math.sqrt((x1-x0)^2 + (y1-y0)^2)/(2*Math.sin(alpha-theta))));
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
}
