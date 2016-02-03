package edu.illinois.mitra.demo.traffic;

import java.util.*;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.*;
import edu.illinois.mitra.starl.motion.*;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;

public class TrafficApp extends LogicThread {
	ItemPosition CS_A = new ItemPosition("CS_A", 2250, 2750, 0);
	ItemPosition CS_B = new ItemPosition("CS_B", 2750, 2750, 0);
	ItemPosition CS_C = new ItemPosition("CS_C", 2250, 2250, 0);
	ItemPosition CS_D = new ItemPosition("CS_D", 2750, 2250, 0);
	public static final int REQUEST_MSG = 22;
	public static final int REPLY_MSG = 23;
	public static final int REGISTER_MSG = 24;
	public static final int REGISTER_R_MSG = 25;
	public static final int UNREGISTER_MSG = 26;
	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	private volatile MotionParameters param = DEFAULT_PARAMETERS;
	Queue<ItemPosition> destinations = new LinkedList<ItemPosition>();
	ObstacleList obEnvironment;
	int timeStamp;
	int robotIndex;
	List<String> ListOfCars = new ArrayList<String>();
	List<String> sections = new ArrayList<String>();
	List<RobotMessage> msgQueue = new ArrayList<RobotMessage>();
	List<RobotMessage> toremoveQueue = new ArrayList<RobotMessage>();
	TreeMap<Integer, String> R_msgQueue = new TreeMap<Integer, String>();
	TreeMap<String, String> R_msgQueue2 = new TreeMap<String, String>();
	ItemPosition currentDestination, preDestination, myPos;
	private long timeNow;
		
	private enum Stage {
		PICK, GO, REGISTER, WAIT, REQUEST, ENTRY, CS, EXIT, DONE
	};

	private Stage stage = Stage.PICK;

	public TrafficApp(GlobalVarHolder gvh) {
		super(gvh);
		robotIndex = Integer.parseInt(name.substring(3,name.length()));
		MotionParameters.Builder settings = new MotionParameters.Builder();
		settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.STOP_ON_COLLISION);
		//settings.GOAL_RADIUS(15);
		param = settings.build();
		gvh.plat.moat.setParameters(param);
		obEnvironment = gvh.gps.getObspointPositions();
		//set the destination for each robot, which are differed by their angle
		//angle does not serve much purpose here, therefore, we use it to identify the way points for each robot 
		for(ItemPosition i : gvh.gps.getWaypointPositions()){
			if(i.index == robotIndex){
				ItemPosition toAdd = new ItemPosition(i);
				toAdd.index = 0;
				destinations.add(toAdd);
			}
		}
		gvh.comms.addMsgListener(this, REQUEST_MSG);
		gvh.comms.addMsgListener(this, REPLY_MSG);
		gvh.comms.addMsgListener(this, REGISTER_MSG);
		gvh.comms.addMsgListener(this, REGISTER_R_MSG);
		gvh.comms.addMsgListener(this, UNREGISTER_MSG);
		
	}

	@Override
	public List<Object> callStarL() {
		while(true) {
			myPos = gvh.gps.getMyPosition();
			if((gvh.gps.getMyPosition().type == 0) || (gvh.gps.getMyPosition().type == 1)){
				
				switch(stage) {
				case PICK:
					if(destinations.isEmpty()) {
						stage = Stage.DONE;
					} else 
					{
						currentDestination = (ItemPosition)destinations.peek();
						if(withinCS(currentDestination)){
                            getWanted();
							msgQueue.clear();
							toremoveQueue.clear();
							R_msgQueue.clear();
							R_msgQueue2.clear();
							timeNow = 	gvh.time();
							requestRegisterList();
							stage = Stage.REGISTER;
						}
						else{
							gvh.plat.moat.goTo(currentDestination);	
							stage = Stage.GO;
						}
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
					
				case REGISTER:
					if(timeNow+2000<gvh.time())
						stage = Stage.WAIT;
					break;
				case WAIT:
					getRegisterList();
					if(ListOfCars.size() == 0){
						stage = Stage.REGISTER;
						timeNow = System.currentTimeMillis();
						requestRegisterList();
					}
					else
						stage = Stage.REQUEST;
					break;
				case REQUEST:
                    checkQueue();
					String[] section_string = new String[sections.size()+1];
					for(int i = 0; i< sections.size();i++){
						section_string[i] = sections.get(i);
					}
					section_string[sections.size()] = ((String) ("" + timeStamp));
					//attach the timeStamp at the end of the message
					MessageContents sections_msg = new MessageContents(section_string);
					RobotMessage request = new RobotMessage("ALL", name, REQUEST_MSG, sections_msg);
					gvh.comms.addOutgoingMessage(request);
					ListOfCars.remove(name);
					stage = Stage.ENTRY;
					break;
					
				case ENTRY:
					//just wait for others to reply
					//just a wait stage
					//send message, stay in ENTRY, when received all messages, go to CS
					if(ListOfCars.isEmpty()){
						gvh.plat.moat.goTo(currentDestination);	
						stage = Stage.CS;
						//everyone replies, go to CS
					}
					break;	
					
				case CS:
					if(!gvh.plat.moat.inMotion) {
						// it has reached the previous point
						
						if(currentDestination != null){
							if(preDestination != null){
								//release the last CS section
								release(CSname(preDestination));
							}
							preDestination = new ItemPosition(currentDestination);
							destinations.remove();
						}
						currentDestination = (ItemPosition)destinations.peek();
						if(withinCS(currentDestination)){
							stage = Stage.CS;
							gvh.plat.moat.goTo(currentDestination);	
						}
						else{
							gvh.plat.moat.goTo(currentDestination);	
							stage = Stage.EXIT;
						}
					}
					break;
					
				case EXIT:
					if(!gvh.plat.moat.inMotion) {
						releaseAll();
						preDestination = null;
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
				String id = m.getFrom();
				int id_num = Integer.parseInt(id.substring(3,name.length()));
				MessageContents msg_content = m.getContents();
				List<String> R_request = new ArrayList<String>(msg_content.getContents());
				int tStamp = Integer.parseInt(R_request.remove(R_request.size()-1));
				//get the sections and the timeStamp
				if(stage == Stage.ENTRY||stage == Stage.REQUEST){
					boolean intersect = false;
					for(int i = 0; i<sections.size(); i++){
						if(R_request.contains(sections.get(i))){
							intersect = true;
						}
					}
					if(intersect && ((tStamp>timeStamp) || ((tStamp == timeStamp) && id_num>robotIndex)))
						//if(intersect and (m.timeStamp,m.id)>(timeStamp,id))
						QueueMSG(m);
					else
						replyToRequest(m);
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
						replyToRequest(m);
				}
				if(stage == Stage.GO ||stage == Stage.DONE || stage == Stage.PICK){
					replyToRequest(m);
				}
				if(stage == Stage.REGISTER || stage == Stage.WAIT ){
					//still working on the register list, should deal with message after register
					QueueMSG(m);
				}
				return;
				
			}
			if(m.getMID() == REPLY_MSG){
                if(ListOfCars.contains(m.getFrom())){
                    ListOfCars.remove(m.getFrom());
                }
                else{
                    QueueMSG(m);
                }

                return;
			}
			if(m.getMID() == REGISTER_MSG){
				if(stage == Stage.REGISTER){
					MessageContents register_R_msg = new MessageContents(-1);
					RobotMessage register_R = new RobotMessage(m.getFrom(), name, REGISTER_R_MSG, register_R_msg);
					//System.out.println(name + " adding register reply to "+ m.getFrom() + " with Stamp -1");
					gvh.comms.addOutgoingMessage(register_R);
					
					if(!R_msgQueue2.containsKey(m.getFrom())){
						R_msgQueue2.put(m.getFrom(), m.getFrom());
					}
					
				}
				else{
					if(stage != Stage.GO && stage != Stage.PICK){
						MessageContents register_R_msg = new MessageContents(timeStamp);
						RobotMessage register_R = new RobotMessage(m.getFrom(), name, REGISTER_R_MSG, register_R_msg);
						//System.out.println(name + " adding register reply to "+ m.getFrom() + " with Stamp " + timeStamp);
						gvh.comms.addOutgoingMessage(register_R);
					}
				}
			}
			if(m.getMID() == REGISTER_R_MSG){
				int position = Integer.parseInt(m.getContents().get(0));
				if(position>=0)
					R_msgQueue.put(position,m.getFrom());
				else{
					if(!R_msgQueue2.containsKey(m.getFrom())){
						R_msgQueue2.put(m.getFrom(), m.getFrom());
						/*
						MessageContents register_R_msg = new MessageContents(-1);
						RobotMessage register_R = new RobotMessage(m.getFrom(), name, REGISTER_R_MSG, register_R_msg);
						System.out.println(name + " adding register reply to "+ m.getFrom() + " with Stamp -1");
						gvh.comms.addOutgoingMessage(register_R);
						*/
					}
				}
			}
			if(m.getMID() == UNREGISTER_MSG){
				String tmp = m.getFrom();
				if(stage == Stage.REGISTER){
					R_msgQueue.remove(tmp);
				}
				else{
					if(stage != Stage.GO && stage != Stage.PICK){
						ListOfCars.remove(tmp);
					}
				}
				return;
			}
		}
	}
	
	private void release(String CSname) {
		sections.remove(0);
		//System.out.println(name + " releasing "+ CSname);
		checkQueue();
	}
	
	private void checkQueue() {
		if(!msgQueue.isEmpty()){
			for (RobotMessage temp : msgQueue) {
			    receive(temp);
			}
		}
		while(!toremoveQueue.isEmpty()){
			RobotMessage temp2 = toremoveQueue.remove(0);
			msgQueue.remove(temp2);
		}
	}

	private void releaseAll() {
//		System.out.println("releasing");
		/*
		while(!msgQueue.isEmpty()){
			replyToRequest(msgQueue.remove(0));
		}
		*/
		MessageContents sections_msg = new MessageContents("unregister");
		RobotMessage unregister = new RobotMessage("ALL", name, UNREGISTER_MSG, sections_msg);
		gvh.comms.addOutgoingMessage(unregister);
		return;
	}
	
	private void replyToRequest(RobotMessage m2) {
		
		//System.out.println(name + " replying to "+m2.getFrom() + " at Stage " + stage);
		if(msgQueue.contains(m2)){
			toremoveQueue.add(m2);
	//		System.out.println("adding reply to "+m2);
		}
		String id = m2.getFrom();
		MessageContents sections_msg = new MessageContents("OK");
		RobotMessage request = new RobotMessage(id, name, REPLY_MSG, sections_msg);
		gvh.comms.addOutgoingMessage(request);
	}

	private void QueueMSG(RobotMessage m) {
		if(msgQueue.contains(m)){
	//		System.out.println(name + " queueing "+m);
			return;
		}
		// queue the message
		msgQueue.add(m);
	}

	/**
	 * get the section wanted by finding following critical sections in the destinations
	 * modify the variable sections and return
	**/
	private void getWanted() {
		sections.clear();
		Iterator<ItemPosition> iterator = destinations.iterator();
		while(iterator.hasNext()){
		  ItemPosition temp = (ItemPosition) iterator.next();
		  if(withinCS(temp)){
			  sections.add(CSname(temp));
	//		  System.out.println(name +" wants "+CSname(temp));
		  }
		  else
			  break;
		}
		return;
	}
	
	private void requestRegisterList(){
		MessageContents register_msg = new MessageContents("0");
		RobotMessage register = new RobotMessage("ALL", name, REGISTER_MSG, register_msg);
		gvh.comms.addOutgoingMessage(register);
		
	}
	
	
	private void getRegisterList() {
			//construct the Register List from received messages
		/*
			boolean complete = true;
			System.out.println("Reply queue is "+R_msgQueue);
			for(int i = 0; i<R_msgQueue.size(); i++){
				if(R_msgQueue.containsKey(i+1))
					ListOfCars.add(R_msgQueue.remove(i+1));
				else{
					complete = false;
					break;
				}
			}
			if(complete){
				R_msgQueue2.put(name, name);
				while(!R_msgQueue2.isEmpty()){
					ListOfCars.add(R_msgQueue2.remove(R_msgQueue2.firstKey()));
				}
				timeStamp = ListOfCars.indexOf(name);
				System.out.println(ListOfCars);
				return;
				//everything works out, return
			}
			else{
				R_msgQueue.clear();
				R_msgQueue2.clear();
				ListOfCars.clear();
			}
			*/
//		System.out.println(name +" message queue is "+R_msgQueue);
//		System.out.println(name + " register queue is "+R_msgQueue2);
		int offset = 0;
		if(!R_msgQueue.isEmpty())
			offset = R_msgQueue.lastKey();
		while(!R_msgQueue.isEmpty()){
			ListOfCars.add(R_msgQueue.remove(R_msgQueue.firstKey()));
			
		}
		R_msgQueue2.put(name, name);
		while(!R_msgQueue2.isEmpty()){
			ListOfCars.add(R_msgQueue2.remove(R_msgQueue2.firstKey()));
		}
		//the timeStamp offset need to be added
		timeStamp = ListOfCars.indexOf(name)+offset;
		System.out.println(name + ListOfCars);
		return;
		//everything works out, return
	

		
		/*
		ListOfCars.clear();
		ListOfCars.add("bot3");
		ListOfCars.add("bot2");
		ListOfCars.add("bot1");
		ListOfCars.add("bot0");
		timeStamp = ListOfCars.indexOf(name);
		ListOfCars.remove(name);
		//ListOfCars.add("bot5");
		return;
		*/
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