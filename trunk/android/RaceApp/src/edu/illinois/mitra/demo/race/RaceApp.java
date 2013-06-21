package edu.illinois.mitra.demo.race;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.illinois.mitra.starl.motion.*;

public class RaceApp extends LogicThread {
	private static final boolean RANDOM_DESTINATION = false;
	public static final int ARRIVED_MSG = 22;

	final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
	ObstacleList obsList = new ObstacleList();
	ItemPosition currentDestination, currentDestination1;
	
//	private String temp;
//	ItemPosition currentDestination1 = new ItemPosition(temp, 1,1,1);
	
	private LeaderElection le;
//	private String leader = null;
	private boolean iamleader = false;
	
	private enum Stage {
		PICK, GO, DONE, ELECT, HOLD
	};

	private Stage stage = Stage.ELECT;

	public RaceApp(GlobalVarHolder gvh) {
		super(gvh);
		
		le = new RandomLeaderElection(gvh);
		
		
		MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
		MotionParameters param = settings.build();
		gvh.plat.moat.setParameters(param);
		
		for(ItemPosition i : gvh.gps.getWaypointPositions())
			destinations.put(i.getName(), i);
		obsList = gvh.gps.getObspointPositions();
		
		gvh.comms.addMsgListener(this, ARRIVED_MSG);
	}

	@Override
	public List<Object> callStarL() {
		while(true) {
			switch(stage) {
			case ELECT:
				le.elect();
				if(le.getLeader() != null) {
					results[1] = le.getLeader();
				}
				stage = Stage.PICK;

				break;
			case PICK:
				if(destinations.isEmpty()) {
					stage = Stage.DONE;
				} else 
				{
					RobotMessage informleader = new RobotMessage("ALL", name, 21, le.getLeader());
					gvh.comms.addOutgoingMessage(informleader);

					iamleader = le.getLeader().equals(name);
					iamleader = true;
					
					if(iamleader)
					{
					currentDestination = getRandomElement(destinations);
				
					gvh.plat.moat.goTo(currentDestination);
					stage = Stage.GO;
					}
					else
					{
					currentDestination = gvh.gps.getPosition(le.getLeader());	
					currentDestination1 = new ItemPosition(currentDestination); 
					int newx, newy;
					if(gvh.gps.getPosition(name).getX() < currentDestination1.getX())
					{
						newx = gvh.gps.getPosition(name).getX() - currentDestination1.getX()/8;
					}
					else
					{
						newx = gvh.gps.getPosition(name).getX() + currentDestination1.getX()/8;
					}
					if(gvh.gps.getPosition(name).getY() < currentDestination1.getY())
					{
						newy = gvh.gps.getPosition(name).getY() - currentDestination1.getY()/8;
					}
					else
					{
						newy = gvh.gps.getPosition(name).getY() + currentDestination1.getY()/8;
					}
					currentDestination1.setPos(newx, newy, (currentDestination1.getAngle())); 
	//				currentDestination1.setPos(currentDestination);
					gvh.plat.moat.goTo(currentDestination1);
					stage = Stage.HOLD;
					}
				}
				break;
			case GO:
				if(!gvh.plat.moat.inMotion) {
					if(currentDestination != null)
						destinations.remove(currentDestination.getName());
					RobotMessage inform = new RobotMessage("ALL", name, ARRIVED_MSG, currentDestination.getName());
					gvh.comms.addOutgoingMessage(inform);
					stage = Stage.PICK;
				}
				break;
			case HOLD:
				if(gvh.gps.getMyPosition().distanceTo(gvh.gps.getPosition(le.getLeader())) < 1000 )
				{
				stage = Stage.PICK;
			    }
				else
				{
				gvh.plat.moat.motion_stop();	
				}
				break;
			case DONE:
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