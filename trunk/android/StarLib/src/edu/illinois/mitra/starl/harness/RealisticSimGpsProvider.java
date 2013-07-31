package edu.illinois.mitra.starl.harness;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.illinois.mitra.starl.objects.PositionList;

public class RealisticSimGpsProvider extends Observable implements SimGpsProvider {	
	private Map<String, SimGpsReceiver> receivers;
	private Map<String, TrackedRobot> robots;

	// Waypoint positions and robot positions that are shared among all robots
	private PositionList robot_positions;
	private PositionList waypoint_positions;
	private ObstacleList obspoint_positions;
	private Vector<ObstacleList> viewsOfWorld;
	
	private long period = 100;
	private double angleNoise = 0;
	private double posNoise = 0;
	private int robotRadius = 0;

	private Random rand;
	
	private SimulationEngine se;
		
	public RealisticSimGpsProvider(SimulationEngine se, long period, double angleNoise, double posNoise, int robotRadius) {
		this.se = se;
		this.period = period;
		this.angleNoise = angleNoise;
		this.posNoise = posNoise;
		this.rand = new Random();
		this.robotRadius = robotRadius;
		
		receivers = new HashMap<String, SimGpsReceiver>();
		robots = new ConcurrentHashMap<String, TrackedRobot>();
		
		robot_positions = new PositionList();
		waypoint_positions = new PositionList();
	}
	
	@Override
	public synchronized void registerReceiver(String name, SimGpsReceiver simGpsReceiver) {
		receivers.put(name, simGpsReceiver);
	}
	
	@Override
	public synchronized void addRobot(ItemPosition bot) {
		synchronized(robots) {
			robots.put(bot.name, new TrackedRobot(bot));
		}
		robot_positions.update(bot, se.getTime());
	}
	
	@Override
	public synchronized void setDestination(String name, ItemPosition dest, int vel) {
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
	public void setObspoints(ObstacleList loadedObspoints) {
		if(loadedObspoints != null) obspoint_positions = loadedObspoints;
	}
	
	
	@Override
	public void setViews(ObstacleList environment, int nBots) {
		if(environment != null){
			viewsOfWorld = new Vector<ObstacleList>(3,2);
			ObstacleList obsList = null;
			for(int i = 0; i< nBots ; i++){
				obsList = environment.downloadObs();
				obsList.Gridfy();
				viewsOfWorld.add(obsList);
			}
		}
	}
	

	@Override
	public ObstacleList getObspointPositions() {
		return obspoint_positions;
	}
	
	@Override
	public PositionList getWaypointPositions() {
		return waypoint_positions;
	}
	
	@Override
	public Vector<ObstacleList> getViews() {
		return viewsOfWorld;
	}
	
	@Override
	public void start() {
		// Create a periodic runnable which repeats every "period" ms to report positions
		Thread posupdate = new Thread() {
			@Override
			public void run() {
				Thread.currentThread().setName("RealisticGpsProvider");
				se.registerThread(this);
				
				while(true) {
					synchronized(robots) {
						for(TrackedRobot r : robots.values()) {
							if(r.inMotion()) {
								r.updatePos();
								receivers.get(r.getName()).receivePosition(r.inMotion());	
							}
						}	
					}
					setChanged();
					notifyObservers(robot_positions);
					
					try {
						se.threadSleep(period, this);
						Thread.sleep(Long.MAX_VALUE);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		posupdate.start();
	}	
	
	@Override
	public void notifyObservers(Object data) {
		// Catch NullPointerExceptions by ignorning null data
		if(data != null) super.notifyObservers(data);
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
			timeLastUpdate = se.getTime();
		}
		public void updatePos() {
			double timeSinceUpdate = (se.getTime() - timeLastUpdate)/1000.0;
			int dX = 0, dY = 0;
			double dA = 0;
			
			// Arcing motion
			dA = aNoise + (vRad*timeSinceUpdate);
			dX = (int) (xNoise + Math.cos(Math.toRadians(cur.angle))*(vFwd*timeSinceUpdate));
			dY = (int) (yNoise + Math.sin(Math.toRadians(cur.angle))*(vFwd*timeSinceUpdate));
			
			cur.velocity = (int) (Math.sqrt(Math.pow(dX,2) + Math.pow(dY,2))/timeSinceUpdate);
			cur.x += dX;
			cur.y += dY;
			angle = angle + dA;
			cur.angle = Common.angleWrap((int)(Math.round(angle) + aNoise));
			
			timeLastUpdate = se.getTime();
		}
		public void setVel(int fwd, int rad) {
			if(inMotion()) {
				updatePos();
			} else {
				timeLastUpdate = se.getTime();
			}
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
	
	// This isn't very functional at the moment
	private boolean checkCollision(ItemPosition bot) {
		for(ItemPosition ip : robot_positions.getList()) {
			if(ip.equals(bot)) break;
			if(bot.isFacing(ip, robotRadius) && bot.distanceTo(ip) <= robotRadius) return true;
		}
		return false;
	}

}
