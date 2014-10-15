package edu.illinois.mitra.demo.traffic;

import java.util.*;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.*;
import edu.illinois.mitra.starl.motion.*;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;

public class TrafficSignApp extends LogicThread {
	public static final int ARRIVED_MSG = 22;
	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	Queue<ItemPosition> destinations = new LinkedList<ItemPosition>();
	ObstacleList obEnvironment;
	int robotIndex;
	RRTNode kdTree = new RRTNode();
	ItemPosition CS_A = new ItemPosition("CS_A", 2250, 2750, 0);
	ItemPosition CS_B = new ItemPosition("CS_B", 2750, 2750, 0);
	ItemPosition CS_C = new ItemPosition("CS_C", 2250, 2250, 0);
	ItemPosition CS_D = new ItemPosition("CS_D", 2750, 2250, 0);
	ItemPosition currentDestination, preDestination;
		
	private enum Stage {
		PICK, ENTRY, CS, GO, DONE, EXIT
	};

	private Stage stage = Stage.PICK;

	public TrafficSignApp(GlobalVarHolder gvh) {
		super(gvh);
		robotIndex = Integer.parseInt(name.substring(3,name.length()));
		MotionParameters.Builder settings = new MotionParameters.Builder();
		settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.STOP_ON_COLLISION);
		settings.GOAL_RADIUS(15);
		MotionParameters param = settings.build();
		gvh.plat.moat.setParameters(param);
		obEnvironment = gvh.gps.getObspointPositions();
		//set the destination for each robot, which are differed by their angle
		//angle does not serve much purpose here, therefore, we use it to identify the way points for each robot 
		for(ItemPosition i : gvh.gps.getWaypointPositions()){
			if(i.angle-1 == robotIndex){
				destinations.add(i);
			}
		}

		
		gvh.comms.addMsgListener(this, ARRIVED_MSG);
	}

	@Override
	public List<Object> callStarL() {
		while(true) {
			if((gvh.gps.getMyPosition().type == 0) || (gvh.gps.getMyPosition().type == 1)){
				
				switch(stage) {
				case PICK:
					if(destinations.isEmpty()) {
						stage = Stage.DONE;
					} else 
					{
						currentDestination = (ItemPosition)destinations.poll();
						if(withinCS(currentDestination))
							stage = Stage.ENTRY;
						else
							stage = Stage.GO;
					}
					break;
				case GO:
					if(!gvh.plat.moat.inMotion) {
						if(currentDestination != null){
							destinations.remove(currentDestination.getName());
							gvh.plat.moat.goTo(currentDestination);
						}
					}
						
					break;
				case ENTRY:
					
					break;	
					
				case CS:
					break;
					
				case EXIT:
					break;
					
				case DONE:
					gvh.plat.moat.motion_stop();
					//if does not return null, program will never halt
					//useful for debugging
					//return null;
					break;
				}
			}
			sleep(100);
		}
	}

	private boolean withinCS(ItemPosition current) {
		if(current.x == CS_A.x && current.y == CS_A.y)
			return true;
		if(current.x == CS_B.x && current.y == CS_B.y)
			return true;
		if(current.x == CS_C.x && current.y == CS_C.y)
			return true;
		if(current.x == CS_D.x && current.y == CS_D.y)
			return true;
		return false;
	}

	@Override
	protected void receive(RobotMessage m) {
		String posName = m.getContents(0);

		
	}
}