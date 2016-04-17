package edu.illinois.mitra.demo.maze;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.*;
import edu.illinois.mitra.starl.motion.*;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;

public class MazeApp extends LogicThread {
	private static final boolean RANDOM_DESTINATION = false;
	public static final int ARRIVED_MSG = 22;
	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	private volatile MotionParameters param = DEFAULT_PARAMETERS;
	final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
	int robotIndex;

	// used to find path through obstacles
	Stack<ItemPosition> pathStack;	
	RRTNode kdTree;
	ObstacleList obsList;
	//obsList is a local map each robot has, when path planning, use this map
	ObstacleList obEnvironment;
	//obEnvironment is the physical environment, used when calculating collisions
	ItemPosition currentDestination, preDestination;


	private enum Stage {
		PICK, GO, DONE, HOLD
	};

	private Stage stage = Stage.PICK;

	public MazeApp(GlobalVarHolder gvh) {
		super(gvh);
		for(int i = 0; i< gvh.gps.get_robot_Positions().getNumPositions(); i++){
			if(gvh.gps.get_robot_Positions().getList().get(i).name == name){
				robotIndex = i;
				break;
			}
		}

		MotionParameters.Builder settings = new MotionParameters.Builder();
		//		settings.ROBOT_RADIUS(400);
		settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLBACK);
		MotionParameters param = settings.build();
		gvh.plat.moat.setParameters(param);

		for(ItemPosition i : gvh.gps.getWaypointPositions())
			destinations.put(i.getName(), i);

		//point the environment to internal data, so that we can update it 
		obEnvironment = gvh.gps.getObspointPositions();

		//download from environment here so that all the robots have their own copy of visible ObstacleList
		obsList = gvh.gps.getViews().elementAt(robotIndex) ;

		gvh.comms.addMsgListener(this, ARRIVED_MSG);
	}

	@Override
	public List<Object> callStarL() {
		while(true) {
			obEnvironment.updateObs();			
			obsList.updateObs();
			kdTree = gvh.plat.reachAvoid.kdTree;

			switch(stage) {
			case PICK:
//				System.out.println("PICK");
				if(destinations.isEmpty()) {
					stage = Stage.DONE;
				} else 
				{
					currentDestination = getRandomElement(destinations);
					gvh.plat.reachAvoid.doReachAvoid(gvh.gps.getMyPosition(), currentDestination, obsList);
					stage = Stage.HOLD;		
				}
				break;	
			case GO:
//				System.out.println("GO");
				if(gvh.plat.reachAvoid.doneFlag) {
					preDestination = currentDestination;
					if(currentDestination != null)
						destinations.remove(currentDestination.getName());
					RobotMessage inform = new RobotMessage("ALL", name, ARRIVED_MSG, currentDestination.getName());
					gvh.comms.addOutgoingMessage(inform);
					stage = Stage.PICK;
				}
				else if(gvh.plat.reachAvoid.failFlag) {
					stage = Stage.PICK;
					// call reach avoid again to plan path again
				}
				break;
			case HOLD:
//				System.out.println("HOLD");
				pathStack = gvh.plat.reachAvoid.pathStack;
				if(pathStack != null){
					preDestination = null;
					kdTree = gvh.plat.reachAvoid.kdTree;
					stage = Stage.GO;
				}
				if(gvh.plat.reachAvoid.failFlag){
					System.out.println("Plan Failed");
					gvh.plat.moat.motion_stop();
				}
			break;
			case DONE:
				System.out.println("DONE");
				gvh.plat.moat.motion_stop();
				return null;
			}
			sleep(100);
		}
	}

	@Override
	protected void receive(RobotMessage m) {
		String posName = m.getContents(0);
		if(destinations.containsKey(posName))
			destinations.remove(posName);

		if(currentDestination.getName().equals(posName)) {
			gvh.plat.moat.cancel();
			stage = Stage.PICK;
		}

	}

	private static final Random rand = new Random();

	@SuppressWarnings("unchecked")
	private <X, T> T getRandomElement(Map<X, T> map) {
		if(RANDOM_DESTINATION)
			return (T) map.values().toArray()[rand.nextInt(map.size())];
		else
			return (T) map.values().toArray()[0];
	}	
}