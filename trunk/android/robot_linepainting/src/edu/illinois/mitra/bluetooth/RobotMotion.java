package edu.illinois.mitra.bluetooth;

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
			handler.post(new motion_turn(gvh,dest,stopinMotion));
		}
	}
	
	public void go_to(itemPosition dest) {
		inMotion = true;
		this.destination = dest;
		currentpos = gvh.getMyPosition();
		turn_to(destination, false);

		handler.post(new motion_go(gvh,dest));
	}
	
	public void cancel() {
		running = false;
		//handler = null;
		bti.send(robot_stop());
		bti.disconnect();		
	}
	
	// Motion class implementing turning
	class motion_turn implements Runnable {
		globalVarHolder gvh;
		itemPosition destination;
		boolean stop;
		public motion_turn(globalVarHolder gvh, itemPosition destination, boolean stop) {
			this.gvh = gvh;
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
				handler.postDelayed(new motion_turn(gvh,destination,stop), 50);
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
		globalVarHolder gvh;
		itemPosition destination;
		public motion_go(globalVarHolder gvh, itemPosition destination) {
			this.gvh = gvh;
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
					}
				}
				handler.postDelayed(this, 50);
			} else {
				bti.send(robot_stop());
				inMotion = false;
			}
		}
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
	}
}
