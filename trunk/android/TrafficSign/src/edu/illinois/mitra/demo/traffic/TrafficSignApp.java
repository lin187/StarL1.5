package edu.illinois.mitra.demo.traffic;

import java.util.*;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.*;
import edu.illinois.mitra.starl.motion.*;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;

public class TrafficSignApp extends LogicThread {
	public static final int REQUEST_MSG = 22;
	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	private volatile MotionParameters param = DEFAULT_PARAMETERS;
	Queue<ItemPosition> destinations = new LinkedList<ItemPosition>();
	ObstacleList obEnvironment;
	int robotIndex;
	Hashtable<String, Boolean> ListOfCars = new Hashtable<String, Boolean>();
	List<String> sections = new ArrayList<String>();
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
		param = settings.build();
		gvh.plat.moat.setParameters(param);
		obEnvironment = gvh.gps.getObspointPositions();
		//set the destination for each robot, which are differed by their angle
		//angle does not serve much purpose here, therefore, we use it to identify the way points for each robot 
		for(ItemPosition i : gvh.gps.getWaypointPositions()){
			if(i.angle-1 == robotIndex){
				destinations.add(i);
			}
		}
		

		
		gvh.comms.addMsgListener(this, REQUEST_MSG);
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
						currentDestination = (ItemPosition)destinations.peek();
						if(withinCS(currentDestination))
							stage = Stage.ENTRY;
						else{
							gvh.plat.moat.goTo(currentDestination);	
							stage = Stage.GO;
						}
					}
					break;
				case GO:
					if(!gvh.plat.moat.inMotion) {
						if(currentDestination != null)
							destinations.remove();
						stage = Stage.PICK;
					}
					break;
				case ENTRY:
					
					getRegisterList();
					getWanted();
					String[] section_string = new String[sections.size()];
					for(int i = 0; i< sections.size();i++){
						section_string[i] = sections.get(i);
					}
					MessageContents sections_msg = new MessageContents(section_string);
					RobotMessage request = new RobotMessage("ALL", name, REQUEST_MSG, sections_msg);
					gvh.comms.addOutgoingMessage(request);
					//send message, stay in ENTRY, when received all messages, go to CS
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
	
	@Override
	protected void receive(RobotMessage m) {
		String id = m.getFrom();
		int id_num = Integer.parseInt(id.substring(3,name.length()));
		MessageContents msg_content = m.getContents();
		List<String> R_request = msg_content.getContents();
		if(stage == Stage.ENTRY){
			boolean intersect = false;
			for(int i = 0; i<sections.size(); i++){
				if(R_request.contains(sections.get(i))){
					intersect = true;
				}
			}
			if(intersect && (id_num>robotIndex))
				QueueMSG(m);
			else
				replyToRequest(id);
		}
		if(stage == Stage.CS ||stage == Stage.EXIT ){
			boolean intersect = false;
			for(int i = 0; i<sections.size(); i++){
				if(R_request.contains(sections.get(i))){
					intersect = true;
				}
			}
			if(intersect)
				QueueMSG(m);
			else
				replyToRequest(id);
		}
	}
	
	
	
	private void replyToRequest(String id) {
		// TODO Auto-generated method stub
		
	}

	private void QueueMSG(RobotMessage m) {
		// queue the message
		
	}

	/**
	 * get the section wanted by finding following critical sections in the destinations
	 * modify the variable sections and return
	**/
	private void getWanted() {
		sections.clear();
		sections.add(CSname(currentDestination));
		Iterator<ItemPosition> iterator = destinations.iterator();
		while(iterator.hasNext()){
		  ItemPosition temp = (ItemPosition) iterator.next();
		  if(withinCS(temp)){
			  sections.add(CSname(temp));
		  }
		  else
			  break;
		}
		return;
	}
	
	private void getRegisterList() {
		ListOfCars.clear();
		ListOfCars.put("bot1", false);
		ListOfCars.put("bot2", false);
		ListOfCars.put("bot3", false);
		ListOfCars.put("bot4", false);
		ListOfCars.put("bot5", false);
		return;
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
	
	private String CSname(ItemPosition current) {
		if(current.x == CS_A.x && current.y == CS_A.y)
			return "CS_A";
		if(current.x == CS_B.x && current.y == CS_B.y)
			return "CS_B";
		if(current.x == CS_C.x && current.y == CS_C.y)
			return "CS_C";
		if(current.x == CS_D.x && current.y == CS_D.y)
			return "CS_D";
		return "error";
	}


}