package edu.illinois.mitra.starl.harness;

import java.util.HashMap;
import java.util.Observable;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public class IdealSimGpsProvider extends Observable {
	private static final int VELOCITY = 200;	// Millimeters per second
	
	private HashMap<String, SimGpsReceiver> receivers;
	private HashMap<String, TrackedRobot> robots;

	// Waypoint positions and robot positions that are shared among all robots
	private PositionList robot_positions;
	private PositionList waypoint_positions;
	
	private long period = 100;
	private int angleNoise = 0;
	private int posNoise = 0;

	private Random rand;
	
	private ScheduledThreadPoolExecutor exec;
		
	public IdealSimGpsProvider(long period, int angleNoise, int posNoise) {
		this.period = period;
		this.angleNoise = angleNoise;
		this.posNoise = posNoise;
		this.rand = new Random();
		
		receivers = new HashMap<String, SimGpsReceiver>();
		robots = new HashMap<String, TrackedRobot>();
		exec = new ScheduledThreadPoolExecutor(1);
		
		robot_positions = new PositionList();
		waypoint_positions = new PositionList();
	}
	
	public synchronized void registerReceiver(String name, SimGpsReceiver simGpsReceiver) {
		receivers.put(name, simGpsReceiver);
	}
	
	public synchronized void addRobot(ItemPosition bot) {
		robots.put(bot.getName(), new TrackedRobot(bot));
		robot_positions.update(bot);
	}
	
	public synchronized void setDestination(String name, ItemPosition dest) {
		robots.get(name).setDest(dest);
	}
	
	public synchronized void halt(String name) {
		robots.get(name).setDest(null);
	}
	
	public PositionList getRobotPositions() {
		return robot_positions;
	}


	public void setWaypoints(PositionList loadedWaypoints) {
		if(loadedWaypoints != null) waypoint_positions = loadedWaypoints;
	}
	
	public PositionList getWaypointPositions() {
		return waypoint_positions;
	}
	
	public void start() {
		// Create a periodic runnable which repeats every "period" ms to report positions
		exec.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				for(TrackedRobot r : robots.values()) {
					if(r.hasChanged()) {
						r.updatePos();
						receivers.get(r.getName()).receivePosition(r.inMotion());	
					}
				}	
				setChanged();
				notifyObservers(robot_positions);
			}
		}, period, period, TimeUnit.MILLISECONDS);
	}

	private class TrackedRobot {
		private ItemPosition start = null;
		private ItemPosition pos = null;
		private ItemPosition dest = null;
		private boolean newdest = false;
		private boolean reportpos = false;
		private long timeLastUpdate = 0;		
		private long totalMotionTime = 0;
		private long totalTimeInMotion = 0;
		private double motAngle = 0;
		private double vX = 0;
		private double vY = 0;
		private int aNoise = 0;
		private int xNoise = 0;
		private int yNoise = 0;
		
		public TrackedRobot(ItemPosition pos) {
			this.pos = pos;
			timeLastUpdate = System.currentTimeMillis();
		}
		public void updatePos() {
			long timeSinceUpdate = (System.currentTimeMillis() - timeLastUpdate);
			if(newdest) {
				// Snap to heading
				// Calculate angle and X/Y velocities
				int deltaX = dest.getX()-start.getX();
				int deltaY = dest.getY()-start.getY();
				motAngle = Math.atan2(deltaY, deltaX);
				vX = (Math.cos(motAngle) * VELOCITY);
				vY = (Math.sin(motAngle) * VELOCITY);
				
				// Set position to ideal angle +/- noise
				int angle = (int)Math.toDegrees(Math.atan2(deltaY, deltaX));
				if(angleNoise != 0) aNoise = rand.nextInt(angleNoise*2)-angleNoise;
				pos.setPos(start.getX(), start.getY(), angle+aNoise);
				newdest = false;
			} else if(dest != null) {
				// Calculate noise
				if(angleNoise != 0) aNoise = rand.nextInt(angleNoise*2)-angleNoise;
				if(posNoise != 0) {
					xNoise = rand.nextInt(posNoise*2) - posNoise;
					yNoise = rand.nextInt(posNoise*2) - posNoise;
				}
				// Determine how far we've traveled since the motion started
				// If we've been traveling for longer than it should take to reach the
				// destination, set position to destination and assume we're now at rest. 
				totalTimeInMotion += timeSinceUpdate;
				if(totalTimeInMotion < totalMotionTime) {
					int dX = (int)(vX * totalTimeInMotion)/1000;
					int dY = (int)(vY * totalTimeInMotion)/1000;
					pos.setPos(start.getX()+dX+xNoise, start.getY()+dY+yNoise, (int)Math.toDegrees(motAngle));
				} else {
					pos.setPos(dest.getX()+xNoise, dest.getY()+yNoise, pos.getAngle()+aNoise);
					dest = null;
					reportpos = true;
				}
			} else {
				reportpos = false;
			}
			timeLastUpdate = System.currentTimeMillis();
		}
		public void setDest(ItemPosition dest) {
			if(hasChanged()) updatePos();
			this.dest = dest;
			this.start = new ItemPosition(pos);
			totalMotionTime = (int)(this.start.distanceTo(dest)*1000.0)/VELOCITY;
			totalTimeInMotion = 0;
			newdest = (dest != null);
		}
		public boolean hasChanged() {
			if(reportpos || inMotion()) {
				reportpos = false;
				return true;
			}
			return false;
		}
		public boolean inMotion() {
			return dest != null;
		}
		public String getName() {
			return pos.getName();
		}
	}
}
