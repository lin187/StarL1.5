package edu.illinois.mitra.demo.search;

import java.util.*;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.*;
import edu.illinois.mitra.starl.motion.*;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;

public class DistributedSearchApp extends LogicThread {
	ItemPosition CS_A = new ItemPosition("CS_A", 2250, 2750, 0);
	ItemPosition CS_B = new ItemPosition("CS_B", 2750, 2750, 0);
	ItemPosition CS_C = new ItemPosition("CS_C", 2250, 2250, 0);
	ItemPosition CS_D = new ItemPosition("CS_D", 2750, 2250, 0);
	public static final int REQUEST_MSG = 22;
	public static final int REPLY_MSG = 23;
	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	private volatile MotionParameters param = DEFAULT_PARAMETERS;
	Queue<ItemPosition> destinations = new LinkedList<ItemPosition>();
	
	// used to find path through obstacles
	Stack<ItemPosition> pathStack;	
	RRTNode kdTree = new RRTNode();
	
	ObstacleList obEnvironment;
	int timeStamp;
	int robotIndex;
	List<String> ListOfCars = new ArrayList<String>();
	List<String> sections = new ArrayList<String>();
	List<RobotMessage> msgQueue = new ArrayList<RobotMessage>();
	List<RobotMessage> toremoveQueue = new ArrayList<RobotMessage>();
	TreeMap<Integer, String> R_msgQueue = new TreeMap<Integer, String>();
	TreeMap<String, String> R_msgQueue2 = new TreeMap<String, String>();
	ItemPosition currentDestination, preDestination;
		
	private enum Stage {
		PICK, GO, DONE
	};

	private Stage stage = Stage.PICK;

	public DistributedSearchApp(GlobalVarHolder gvh) {
		super(gvh);
		
		robotIndex = Integer.parseInt(name.substring(3,name.length()));
		MotionParameters.Builder settings = new MotionParameters.Builder();
		settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLBACK);
		//settings.GOAL_RADIUS(15);
		param = settings.build();
		gvh.plat.moat.setParameters(param);
		obEnvironment = gvh.gps.getObspointPositions();
		//set the destination for each robot, which are differed by their angle
		//angle does not serve much purpose here, therefore, we use it to identify the way points for each robot 
		for(ItemPosition i : gvh.gps.getWaypointPositions()){
			if(i.angle == robotIndex){
				ItemPosition toAdd = new ItemPosition(i);
				toAdd.angle = 0;
				destinations.add(toAdd);
			}
		}
		gvh.comms.addMsgListener(this, REQUEST_MSG);
		gvh.comms.addMsgListener(this, REPLY_MSG);
		
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
						
						gvh.plat.moat.goTo(currentDestination);	
						stage = Stage.GO;
						
					}
					break;
				case GO:
					if(!gvh.plat.moat.inMotion) {
						if(currentDestination != null){
							destinations.remove();
						}
						stage = Stage.PICK;
					}
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


	@Override
	protected void receive(RobotMessage m) {
		if(m.getTo().equals(name) || m.getTo().equals("ALL")){
			if(m.getMID() == REQUEST_MSG){
				replyToRequest(m);
				return;
			}
		}
	}
	
	private void replyToRequest(RobotMessage m2) {
//		System.out.println(name + " replying to "+m2.getFrom());
		if(msgQueue.contains(m2)){
			toremoveQueue.add(m2);
	//		System.out.println("adding reply to "+m2);
		}
		String id = m2.getFrom();
		MessageContents sections_msg = new MessageContents("OK");
		RobotMessage request = new RobotMessage(id, name, REPLY_MSG, sections_msg);
		gvh.comms.addOutgoingMessage(request);
	}




}