package edu.illinois.mitra.demo.search;

import java.util.*;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.objects.*;
import edu.illinois.mitra.starl.motion.*;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;

public class DistributedSearchApp extends LogicThread {
	ItemPosition CS_A = new ItemPosition("CS_A", 2250, 2750, 0);
	ItemPosition CS_B = new ItemPosition("CS_B", 2750, 2750, 0);
	ItemPosition CS_C = new ItemPosition("CS_C", 2250, 2250, 0);
	ItemPosition CS_D = new ItemPosition("CS_D", 2750, 2250, 0);
	public static final int TASK_MSG = 21;
	public static final int ASSIGN_MSG = 22;
	public static final int FAILED_MSG = 23;
	public static final int DONE_MSG = 24;
	public static final int FOUND_MSG = 25;
	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	private volatile MotionParameters param = DEFAULT_PARAMETERS;
	Queue<ItemPosition> destinations = new LinkedList<ItemPosition>();
	Queue<ItemPosition> Alldest;
	
	private LeaderElection le;
	private boolean iamleader=false;
	private long Start_time;
	
	// used to find path through obstacles
	Stack<ItemPosition> pathStack;	
	RRTNode kdTree = new RRTNode();
	
	ObstacleList obEnvironment;
	int robotIndex;
	ItemPosition currentDestination, preDestination;
		
	private enum Stage {
		ELECT, ASSIGN, PICK, GO, HOLD, SEARCH, DONE
	};

	private Stage stage = Stage.ELECT;

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

		le = new RandomLeaderElection(gvh);
		gvh.comms.addMsgListener(this, TASK_MSG);
		gvh.comms.addMsgListener(this, ASSIGN_MSG);
		gvh.comms.addMsgListener(this, DONE_MSG);
		gvh.comms.addMsgListener(this, FAILED_MSG);
		gvh.comms.addMsgListener(this, FOUND_MSG);
		
		
	}

	@Override
	public List<Object> callStarL() {
		int itr = 0;
		while(true) {
			if((gvh.gps.getMyPosition().type == 0) || (gvh.gps.getMyPosition().type == 1)){
				
				switch(stage) {
				case ELECT:
					if(itr == 0){
						le.elect();
					}
					if(le.getLeader() != null) {
						iamleader = le.getLeader().equals(name);
						if(iamleader){
							System.out.println(name);
						}
						stage = Stage.ASSIGN;
					}
					break;
				case ASSIGN:
					if(iamleader){
						int size = gvh.gps.getWaypointPositions().getNumPositions();
						int fleet = gvh.gps.getPositions().getNumPositions();
						int mul = (Integer)size/fleet;
						int remain = size%fleet;
						int index = 0;
						Alldest = new LinkedList<ItemPosition>();
						for(int i=0; i < size; i+=mul){
							for(int j = 0; j < mul; j++){
								ItemPosition toAdd = new ItemPosition(gvh.gps.getWaypointPositions().getList().get(i+j));
								//keep angle same as who it was assigned to
								toAdd.angle = index;
								Alldest.add(toAdd);
								assign(toAdd, index);
							}
							if(remain>0){
								ItemPosition toAdd = new ItemPosition(gvh.gps.getWaypointPositions().getList().get(i+mul));
								toAdd.angle = index;
								Alldest.add(toAdd);
								remain--;
								i++;
								assign(toAdd, index);
							}
							index ++;
						}
						//assign finished, tell individual to start
						MessageContents content = new MessageContents("20000");
						RobotMessage start_task_msg = new RobotMessage("ALL", name, TASK_MSG, content);
						gvh.comms.addOutgoingMessage(start_task_msg);
						Start_time = 20000*robotIndex + gvh.time();
						stage = Stage.PICK;
					}
					break;	
				case PICK:
					if(gvh.time()>Start_time){
						if(destinations.isEmpty()) {
							stage = Stage.DONE;
						} else 
						{
							currentDestination = (ItemPosition)destinations.peek();
							
							RRTNode path = new RRTNode(gvh.gps.getPosition(name).x, gvh.gps.getPosition(name).y);
							pathStack = path.findRoute(currentDestination, 5000, obEnvironment, 12330, 8500, (gvh.gps.getPosition(name)), (int) (gvh.gps.getPosition(name).radius));
							kdTree = RRTNode.stopNode;
							//wait when can not find path
							if(pathStack == null){
								stage = Stage.HOLD;	
							}					
							else{
								preDestination = null;
								stage = Stage.GO;
							}
						}
						break;
					}
				case HOLD:
					break;
				case GO:
					if(!gvh.plat.moat.inMotion) {
						if(!pathStack.empty()){
							//if did not reach last midway point, go back to path planning
							if(preDestination != null){
								if((gvh.gps.getPosition(name).distanceTo(preDestination)>param.GOAL_RADIUS)){
									pathStack.clear();
									stage = Stage.PICK;
									break;
								}
								preDestination = pathStack.peek();
							}
							else{
								preDestination = pathStack.peek();
							}
							ItemPosition goMidPoint = pathStack.pop();
							gvh.plat.moat.goTo(goMidPoint);
						}
						else{
							if((gvh.gps.getPosition(name).distanceTo(currentDestination)>param.GOAL_RADIUS)){
								pathStack.clear();
								stage = Stage.PICK;
							}
							else{
								if(currentDestination != null){
									destinations.remove(currentDestination.getName());
								//reached the point, go on searching
									stage = Stage.SEARCH;
								}
							}
						}
					}
					break;
				case SEARCH:
					break;
				case DONE:
					gvh.plat.moat.motion_stop();
				
					//if does not return null, program will never halt
					//useful for debugging
					//return null;
					break;
				}
				itr ++;
			}
			sleep(100);
		}
	}


	private void assign(ItemPosition toAdd, int index) {
		//send the assign message to indexed bot
		String botS = "bot"+index;
		if(botS.equals(name)){
			destinations.add(toAdd);
			return;
		}
		String[] temp = new String[3];
		temp[0] = toAdd.name;
		temp[1] = Integer.toString(toAdd.x);
		temp[2] = Integer.toString(toAdd.y);
		MessageContents content = new MessageContents(temp);
		RobotMessage assign_msg = new RobotMessage(botS, name, ASSIGN_MSG, content);
		gvh.comms.addOutgoingMessage(assign_msg);
		System.out.println("Assign "+ content+ " to "+botS);
	}

	@Override
	protected void receive(RobotMessage m) {
		if(m.getTo().equals(name) || m.getTo().equals("ALL")){
			if(m.getMID() == ASSIGN_MSG){
			//	System.out.println("bot "+name + " got point " + m.getContents(0));
				MessageContents msg_content = m.getContents();
				List<String> assignedP = new ArrayList<String>(msg_content.getContents());
				ItemPosition point = new ItemPosition(assignedP.get(0), Integer.parseInt(assignedP.get(1)), Integer.parseInt(assignedP.get(2)), 0);
				destinations.add(point);
			}
			if(m.getMID() == TASK_MSG){
				//	System.out.println("bot "+name + " got point " + m.getContents(0));
					MessageContents msg_content = m.getContents();
					Start_time = Integer.parseInt(msg_content.getContents().get(0))*robotIndex + gvh.time();
					stage = Stage.PICK;
				}
		}
	}
	




}