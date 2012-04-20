package edu.illinois.mitra.starl.harness;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public class RealisticSimGpsProvider extends Observable implements SimGpsProvider {
	private static final int VELOCITY = 200;	// Millimeters per second
	
	private HashMap<String, SimGpsReceiver> receivers;
	private HashMap<String, TrackedRobot> robots;

	// Waypoint positions and robot positions that are shared among all robots
	private PositionList robot_positions;
	private PositionList waypoint_positions;
	
	private long period = 100;
	private double angleNoise = 0;
	private double posNoise = 0;

	private Random rand;
	
	private ScheduledThreadPoolExecutor exec;
		
	public RealisticSimGpsProvider(long period, double angleNoise, double posNoise) {
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
	
	@Override
	public synchronized void registerReceiver(String name, SimGpsReceiver simGpsReceiver) {
		receivers.put(name, simGpsReceiver);
	}
	
	@Override
	public synchronized void addRobot(ItemPosition bot) {
		robots.put(bot.name, new TrackedRobot(bot));
		robot_positions.update(bot);
	}
	
	@Override
	public synchronized void setDestination(String name, ItemPosition dest) {
		throw new RuntimeException("setDestination is not implemented for realistic simulated motion! " +
				"RealisticSimGpsProvider MUST be used with RealisticSimMotionAutomaton");
	}

	@Override
	public void setVelocity(String name, int fwd, int rad) {
		robots.get(name).setVel(fwd, rad);
	}
	
	@Override
	public synchronized void halt(String name) {
		robots.get(name).setVel(0, 0);
	}
	
	@Override
	public PositionList getRobotPositions() {
		return robot_positions;
	}

	@Override
	public void setWaypoints(PositionList loadedWaypoints) {
		if(loadedWaypoints != null) waypoint_positions = loadedWaypoints;
	}
	
	@Override
	public PositionList getWaypointPositions() {
		return waypoint_positions;
	}
	
	@Override
	public void start() {
		// Create a periodic runnable which repeats every "period" ms to report positions
		exec.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				for(TrackedRobot r : robots.values()) {
					if(r.inMotion()) {
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
		private ItemPosition cur = null;
		private long timeLastUpdate = 0;		
		private double vFwd = 0;
		private double vRad = 0;
		private double aNoise = (rand.nextDouble()*2*angleNoise) - angleNoise;
		private double xNoise = (rand.nextDouble()*2*posNoise) - posNoise;
		private double yNoise = (rand.nextDouble()*2*posNoise) - posNoise;
		private double angle = 0;
		
		public TrackedRobot(ItemPosition pos) {
			this.cur = pos;
			angle = cur.angle;
			timeLastUpdate = System.currentTimeMillis();
		}
		public void updatePos() {
			double timeSinceUpdate = (System.currentTimeMillis() - timeLastUpdate)/1000.0;

			int dX = 0, dY = 0;
			double dA = 0;
			
			// Arcing motion
			dA = (vRad*timeSinceUpdate);
			dX = (int) (Math.cos(Math.toRadians(cur.angle))*(vFwd*timeSinceUpdate));
			dY = (int) (Math.sin(Math.toRadians(cur.angle))*(vFwd*timeSinceUpdate));
			
			cur.x += dX+xNoise;
			cur.y += dY+yNoise;
			angle = angle + dA;
			cur.angle = Common.angleWrap((int)(Math.round(angle) + aNoise));
			
			timeLastUpdate = System.currentTimeMillis();
		}
		public void setVel(int fwd, int rad) {
			if(hasChanged()) updatePos();
			vFwd = fwd;
			vRad = rad;
		}
		public boolean inMotion() {
			return (vFwd != 0 || vRad != 0);
		}
		public String getName() {
			return cur.name;
		}
	}
	
	@Override
	public void addObserver(Observer o) {
		super.addObserver(o);
	}
}
