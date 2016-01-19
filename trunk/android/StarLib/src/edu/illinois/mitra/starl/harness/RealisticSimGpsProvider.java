package edu.illinois.mitra.starl.harness;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import edu.illinois.mitra.starl.interfaces.TrackedRobot;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.Model_iRobot;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.illinois.mitra.starl.objects.Obstacles;
import edu.illinois.mitra.starl.objects.Point3d;
import edu.illinois.mitra.starl.objects.PositionList;

/**
 * This defines methods for initializing motion environment related objects  
 * It also updates robots' movement, collision, other sensor info
 *
 * @author Yixiao Lin & Adam Zimmerman
 * @version 2.0
 */

public class RealisticSimGpsProvider extends Observable implements SimGpsProvider {	
	private Map<String, SimGpsReceiver> receivers;
	private Map<String, TrackedModel<Model_iRobot>> robots;

	// Waypoint positions and robot positions that are shared among all robots
	private PositionList<Model_iRobot> robot_positions;
	private PositionList<ItemPosition> waypoint_positions;
	private PositionList<ItemPosition> sensepoint_positions;
	private ObstacleList obspoint_positions;
	private Vector<ObstacleList> viewsOfWorld;
	
	private long period = 100;
	private double[] noises;

	private SimulationEngine se;
		
	public RealisticSimGpsProvider(SimulationEngine se, long period, double angleNoise, double posNoise, int robotRadius) {
		this.se = se;
		this.period = period;
		noises = new double[3];
		noises[0] = posNoise;
		noises[1] = posNoise;
		noises[2] = angleNoise;
		
		receivers = new HashMap<String, SimGpsReceiver>();
		robots = new ConcurrentHashMap<String, TrackedModel<Model_iRobot>>();
		
		waypoint_positions = new PositionList<ItemPosition>();
		sensepoint_positions = new PositionList<ItemPosition>();
		robot_positions = new PositionList<Model_iRobot>();
	}
	
	@Override
	public synchronized void registerReceiver(String name, SimGpsReceiver simGpsReceiver) {
		receivers.put(name, simGpsReceiver);
	}
	
	@Override
	public synchronized void addRobot(Model_iRobot bot) {
		synchronized(robots) {
			robots.put(bot.name, new TrackedModel<Model_iRobot>(bot));
		}
		robot_positions.update(bot);
	}
	
	@Override
	public synchronized void setDestination(String name, ItemPosition dest, int vel) {
		throw new RuntimeException("setDestination is not implemented for realistic simulated motion! " +
				"RealisticSimGpsProvider MUST be used with RealisticSimMotionAutomaton");
	}

	@Override
	public void setVelocity(String name, int fwd, int rad) {
		((Model_iRobot) robots.get(name).cur).vFwd = fwd;
		((Model_iRobot) robots.get(name).cur).vRad = rad;
	}
	
	@Override
	public synchronized void halt(String name) {
		setVelocity(name, 0, 0);
	}
	
	@Override
	public PositionList<Model_iRobot> getRobotPositions() {
		return robot_positions;
	}

	@Override
	public void setWaypoints(PositionList<ItemPosition> loadedWaypoints) {
		if(loadedWaypoints != null) waypoint_positions = loadedWaypoints;
	}
	
	@Override
	public void setSensepoints(PositionList<ItemPosition> loadedSensepoints) {
		if(loadedSensepoints != null) sensepoint_positions = loadedSensepoints;
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
	public PositionList<ItemPosition> getWaypointPositions() {
		return waypoint_positions;
	}
	
	@Override
	public PositionList<ItemPosition> getSensePositions() {
		return sensepoint_positions;
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
						for(TrackedModel<Model_iRobot> r : robots.values()) {
							if(r.cur.inMotion()) {
								r.updatePos();
								receivers.get(r.getName()).receivePosition((r.cur.inMotion()));	
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

	private class TrackedModel<T extends ItemPosition & TrackedRobot>{
		//private boolean stopMoving = false;
		private T cur = null;
		private long timeLastUpdate = 0;
		
		public TrackedModel(T pos) {
			this.cur = pos;
			timeLastUpdate = se.getTime();
			pos.initialize();
		}
		public void updatePos() {
			double timeSinceUpdate = (se.getTime() - timeLastUpdate)/1000.0;
			
			Point3d p_point = cur.predict(noises, timeSinceUpdate);
			boolean collided = checkCollision(p_point);
			cur.updatePos(!collided);
			
			cur.updateSensor(obspoint_positions, sensepoint_positions);

			timeLastUpdate = se.getTime();
		}
		
		public boolean checkCollision(Point3d bot) {
			//double min_distance = Double.MAX_VALUE;
			boolean toReturn = false;
			for(Model_iRobot current : robot_positions.getList()) {
				if(!current.name.equals(cur.name)) {
					if(cur instanceof Model_iRobot){
						if(bot.distanceTo(current) <= (((Model_iRobot) cur).radius + current.radius)) {
							//update sensors for both robots
							current.collision(cur);
							cur.collision(current);
							toReturn = true;
						}
						//min_distance = Math.min(bot.distanceTo(current) - current.radius, min_distance);

					}
				}
			}
			ObstacleList list = obspoint_positions;
			for(int i = 0; i < list.ObList.size(); i++)
			{
				Obstacles currobs = list.ObList.get(i);
				Point3d nextpoint = currobs.obstacle.firstElement();
				Point3d curpoint = currobs.obstacle.firstElement();
				ItemPosition wall = new ItemPosition("wall",0,0,0);
				
				for(int j = 0; j < currobs.obstacle.size() ; j++){
					curpoint = currobs.obstacle.get(j);
					if (j == currobs.obstacle.size() -1){
						nextpoint = currobs.obstacle.firstElement();
					}
					else{
						nextpoint = currobs.obstacle.get(j+1);
					}
					Point3d closeP = currobs.getClosestPointOnSegment(curpoint.x, curpoint.y, nextpoint.x, nextpoint.y, bot.x, bot.y);
					wall.setPos(closeP.x, closeP.y, 0);
					double distance = Math.sqrt(Math.pow(closeP.x - bot.x, 2) + Math.pow(closeP.y - bot.y, 2)) ;
					
					//need to modify some conditions of bump sensors, we have left and right bump sensor for now
					if(cur instanceof Model_iRobot){
						if((distance < (((Model_iRobot) cur).radius))){
							//update the bump sensor 
							cur.collision(wall);
							toReturn = true;
						}
						/*
						min_distance = Math.min(distance, min_distance);

						if(min_distance < 0.95* (((Model_iRobot) cur).radius)){
							stopMoving = true;
							System.out.println("stopped moving");
						}
						else{
							stopMoving = false;
						}
						*/
					}
				}
			}
			if(!toReturn){
				cur.collision(null);
			}
			return toReturn;
		}
		/*
		@Deprecated
		public void setVel(int fwd, int rad) {
			if(inMotion()) {
				updatePos();
			} else {
				timeLastUpdate = se.getTime();
			}
			cur.vFwd = fwd;
			cur.vRad = rad;
		}
		 */
		public String getName() {
			return cur.name;
		}
		
	}
	
	@Override
	public void addObserver(Observer o) {
		super.addObserver(o);
	}
	
}
