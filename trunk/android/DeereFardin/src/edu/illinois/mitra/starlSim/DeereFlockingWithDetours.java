package edu.illinois.mitra.starlSim;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.Gps;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.RobotMotion;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class DeereFlockingWithDetours extends LogicThread implements MessageListener {

	//////////// CONSTANTS /////////////////
	private static final int PERCENT = 10 ; 
	private static final int SAFE_DISTANCE  = 100; 
	private static final int SAFE_DISTANCE_SELF_CHECK =  30 ; 
	
	final int INIT_PATH_DELTA_DIST = 100; // distance per offline path waypoint
	final int INIT_PATH_DELTA_TIME = 1000; // time per offline path waypoint
	final int INIT_PATH_POINTS = 300;
	final Point INIT_PATH_START = new Point(0,500); // initial point for the offline path
	
		
	// assume flock is facing towards the right, what are the positional offsets?
	private static final Point[] FLOCK_OFFSETS = 
	{
		new Point(0,0), // robot 0 is leader
		new Point(-500,-250),
		new Point(-500, 250),
		new Point(-1000,-500),
		new Point(-1000, 500),
	};
	
	// leader periodically sends timer sync at start
	private static final int MSG_SYNC_TIME = 190; 
	
	// sent by followers when a timer sync is received and the robot is in position
	private static final int MSG_READY = 191;
	
	// sent by leader when all followers are in position
	private static final int MSG_START = 192; 
	
	// sent by leader, new path
	private static final int MSG_PATH_UPDATE = 193;
	
	// sent by followers when they're on the new path
	private static final int MSG_PATH_ACK = 194;
	
	private static final long CYCLE_LENGTH  = 200 ;
	private static final int CHECK_WINDOW = 50;
	private static final int SELF_INTEREFERENCE_CHECK_WINDOW = 30 ;
	private static final int SELF_INTEREFERENCE_CHECK_DELAY = 3 ; 


	private static final int MSG_SENDING_NUMBER = 1 ;
	private static final int VELOCITY_CONSTANT = 40 ; 
	private static final int VELOCITY_MAX = 1000 ; 
	private static final int VELOCITY_MIN = 50 ; 
	
	
	
	
	
	/////////// SHARED VARIABLES BETWEEN LEADER AND FOLLOWERS //////////
	LinkedList <WayPoint> currentPath = new LinkedList <WayPoint>(); 
	
	ArrayList <String> participantsList = new ArrayList<String>() ;
	
	RobotMotion motion;
	Gps gps;
	int robotId; // robot 0 = leader
	private String robotName;
	private int numRobots;
	private long initialTime; 
	private String leaderName ;
	
	WayPoint movingFrom = null;
	WayPoint movingTo = null;
		
	
	//////////// LEADER VARIABLES /////////////////

	// the desired path is the one that goes through the detour point
	ArrayList <WayPoint> currentDesiredPath = new ArrayList <WayPoint>();
	private ArrayList <WayPoint> receivedDesiredPath = new ArrayList <WayPoint>();
		
	ArrayList<Line2D.Double> oldNewWptConnectors = new ArrayList<Line2D.Double>();
	private enum LEADERSTAGE {FOLLOW_PATH, PATH_UPDATE, START, SEND_PATH, PATH_GENERATION, ACK_CHECK, RESEND_PATH, DONE, FOLLOW_PATH_WITHOUT_ACK} ;  
	private LEADERSTAGE leaderStage = LEADERSTAGE.START ; 
	Path[] paths ; 
	AckReport[] ackReport;  
	long startingCycle ; 
	
	int syncId = 0; // used to make sure the ready message correspond to the most recent sync
	int readyCount = 0; // number of robots which responded with the correct sync id
	List <WayPoint> potentialLeaderPath = new ArrayList <WayPoint>();
	int pathID = 0 ;
	Point newPoint = new Point() ; 
	int missedAckCounter = 0 ;
	boolean allAckRec = true ; 
	
	
	
	
	/////////// FOLLOWER VARIABLES ///////////////
	private WayPoint desiredDistance = new WayPoint(400, 0, 0);
	
	boolean recievedStart = false ; 
	boolean readyToStart = false;
	WayPoint to , from ; 
	int currentPathID = -1 ;
	int newPathID = -1 ; 
	boolean pathAcked = false ; 
	
	
	public LinkedList<RobotMessage> mQueue = new LinkedList<RobotMessage>();
//	public BlockingQueue<RobotMessage> outQ = new LinkedBlockingQueue<RobotMessage>();


	
	static int vector;
	
	//this is the number of robots and should be modified everytime!
	static int nBot = 5;
	
	
	public DeereFlockingWithDetours(GlobalVarHolder gvh) {
		super(gvh);
		
		gps = gvh.gps;
		motion = gvh.plat.moat;
		robotName = gvh.id.getName();
		robotId = getRobotId();
		numRobots = gvh.id.getParticipants().size();
		leaderName = "bot0" ; 
		
		paths = new Path[numRobots]; 
		ackReport = new AckReport[numRobots];
		
		 
		for (String followerId:gvh.id.getParticipants() ) {
			participantsList.add(followerId) ; 
		}
		
		
		for (int j = 0 ; j < numRobots ; j++) 
			ackReport[j] = new AckReport() ; 
			
		
		if (numRobots > FLOCK_OFFSETS.length)
			throw new RuntimeException("number of participating robots exceeds defined FLOCK_OFFSETS");
		
		// register packet handlers
		if (robotId == 0)
		{
			gvh.comms.addMsgListener(MSG_SYNC_TIME, this);
			gvh.comms.addMsgListener(MSG_READY, this);
			gvh.comms.addMsgListener(MSG_PATH_ACK, this);


		}
		else
		{
			gvh.comms.addMsgListener(MSG_START, this);
			gvh.comms.addMsgListener(MSG_SYNC_TIME, this);
			gvh.comms.addMsgListener(MSG_PATH_UPDATE, this);
		}
		
		
		// assign initial position
		ItemPosition pos = gvh.gps.getMyPosition();
		movingFrom = movingTo = new WayPoint(pos.x, pos.y, 0);
		
		// each robot knows its offline path
		createInitialRobotPath();
		
		// the leader's desired path is initially the current path
		if (robotId == 0)
		{
			for ( WayPoint wpt:currentPath)
				receivedDesiredPath.add(new WayPoint(wpt.x, wpt.y, wpt.time));
		}			
		
	}
	
	private int getRobotId() {
		////////  //
		System.out.println("Warning: Simulator only implementation - using hardcoded robot names to extract robot ids...");
		
		String botName = "bot";
		
		String robotNumStr = name.substring(botName.length());
		
		return Integer.parseInt(robotNumStr);
	}

	private void createInitialRobotPath() 
	{
		// create the initial path

		
		currentPath.clear();
		
		for (int n = 0; n < INIT_PATH_POINTS; ++n)
		{
			int x = INIT_PATH_START.x + n * INIT_PATH_DELTA_DIST + FLOCK_OFFSETS[robotId].x; 
			int y = INIT_PATH_START.y + FLOCK_OFFSETS[robotId].y;
			int time = n * INIT_PATH_DELTA_TIME ;
			
			WayPoint wp = new WayPoint(x,y,time);
			currentPath.add(wp);
		}
	}

	@Override
	public List<Object> callStarL() {
		 
		if (robotId == 0)
			runLeaderLogic();
		else
			runFollowerLogic();
		

		return null;
	}
		
	public void runLeaderLogic()
	{
		Path[] potentialIntermediatePaths = null;
		Path[] currentPaths =new Path[numRobots] ;
		
		for(int k = 0 ; k < numRobots ; k ++){
			currentPaths[k] = new Path() ; 
		}
		


		// go to the first waypoint
	//	////////  //System.out.println(robotId +" " +currentPath.size());
		 
		WayPoint first = currentPath.removeFirst();
		WayPoint next = first ; 
		movingTo = next ; 
		goToWayPoint(first, VELOCITY_MAX);
		
		
		// send synchronize messages until all followers are in position
		while (readyCount != numRobots - 1)
		{
			//  //System.out.println("wait for everyone!");
			readyCount = 0;
			
			gvh.comms.addOutgoingMessage(new RobotMessage("ALL", robotName, MSG_SYNC_TIME, "" + (++syncId)));
			initialTime = gvh.time();
			
			gvh.sleep(500);
		} 
		//  //System.out.println("done" + readyCount);		
		// send a start message and begin
		gvh.comms.addOutgoingMessage(new RobotMessage("ALL", robotName, MSG_START, ""));
	//	////////  //System.out.println("Salam!");
		

		while (true) {
		
			switch ( leaderStage) { 
			
			case START:
				
				for (int j = 0 ; j < nBot ; j ++) {
					paths[j] = new Path() ;
				}
				
				leaderStage = LEADERSTAGE.PATH_UPDATE ;
				break; 
	
			case PATH_UPDATE:
		//		System.out.println(leaderStage);
//TODO
	/*			
				// this part of the code is for testing purposes!
				System.out.println("cycle: " + GetCycleNumber());
				
				if (GetCycleNumber()  == 25 ) // > 10 && GetCycleNumber() < 20 ) 
					testPointInsert(9519, 3095) ;
				
				
				if (GetCycleNumber()  == 60 || GetCycleNumber()  == 61 ) // > 10 && GetCycleNumber() < 20 ) 
					testPointInsert(12487, -2133) ;
				
				
				if (GetCycleNumber()  == 100 || GetCycleNumber()  == 101 ) // > 10 && GetCycleNumber() < 20 ) 
					testPointInsert(3487, 4133) ;
		*/		
				
				//if (GetCycleNumber() > 10) 
					//testPointInsert(9519, 3095) ; 
				
				startingCycle = GetCycleNumber() ;

				currentDesiredPath = receivedDesiredPath ;
				int firstTime = receivedDesiredPath.get(0).time ; 
				for(WayPoint wpt:currentDesiredPath){
					wpt.time = firstTime ; 
					firstTime += INIT_PATH_DELTA_TIME ; 
				}
				
				
				
				oldNewWptConnectors = connectorLineGenerator(oldNewWptConnectors, currentPath, currentDesiredPath) ;
				//////  //System.out.println("before loop  current path size :" + currentPath.size() + " currentDesiredPath size: " + currentDesiredPath.size() + " oldNewWptConnectors size: " + oldNewWptConnectors.size()) ;
				
				int incPercent = PERCENT ;
				for(int k = 0 ; k < 11 ; k++){

					potentialLeaderPath = generatePotentialLeaderPath(incPercent,currentDesiredPath ,oldNewWptConnectors) ;
					potentialIntermediatePaths = followerPathGeneration(desiredDistance, potentialLeaderPath)  ;
					
					if (interferes(potentialIntermediatePaths, currentPaths)){
						//System.out.println("intereference! " + k);
						incPercent = incPercent * 9/10 ;
						gvh.sleep(10) ;
						
					}
					else {
				//		System.out.println("no interference!");
						oldNewWptConnectors = connectionLineGenerator(potentialLeaderPath, oldNewWptConnectors);
						
						// new paths is okay! assign it to the current path
						currentPath.clear();
						currentPath.addAll(potentialLeaderPath);
						
						for (int r = 0 ; r < numRobots ; r++){
							currentPaths[r].path.clear();
							currentPaths[r].path.addAll(potentialIntermediatePaths[r].path) ; 
						}

						// broadcast potentialIntermediate paths to the other robots
						sendPathsToFollowers(potentialIntermediatePaths);
						
						break ;
					}
				}
				

				leaderStage = LEADERSTAGE.ACK_CHECK ; 
				break ; 
				
				
			case ACK_CHECK:
		//		System.out.println(leaderStage);

				
				gvh.sleep(100); // was 15
				////  //System.out.println("leader state is ACK_CHECK" +  " " + startingCycle + " " + GetCycleNumber());
				
				// if the leader is on the correct cycle
				if ( GetCycleNumber() == startingCycle)
				{
					if ( allAcksRecieved())							
						leaderStage = LEADERSTAGE.PATH_UPDATE;
					else 
						leaderStage = LEADERSTAGE.RESEND_PATH;
				}									
				else // leader is not on the current cycle, let leader move to next waypoint
				{
					////  //System.out.println("follow path without Ack");
					startingCycle = GetCycleNumber() ; 
					
					leaderStage = LEADERSTAGE.FOLLOW_PATH;					
				}


				break;
	
			case RESEND_PATH:
		//		System.out.println(leaderStage);

				// generate and send the path to the followers who have not sent any ack back.
				
				
				for(int k = 1 ; k < nBot ; k++) {
					if (ackReport[k].received == false){
						
						//pathID ++ ; 
						
						String msgContent = makePathMessageString(potentialIntermediatePaths[k]); 
											 
						RobotMessage path = new RobotMessage( participantsList.get(k) ,  gvh.id.getName(), MSG_PATH_UPDATE, msgContent);
						sendMsg(path) ;	
						ackReport[k].received = false ;
						ackReport[k].lastPathIdSent = pathID ; 
					}
				}
				
				
				leaderStage = LEADERSTAGE.ACK_CHECK ; 
				break; 
				
			case FOLLOW_PATH:
			//	System.out.println(leaderStage);
				 
				
				int velocity = VELOCITY_MIN; 
				
//				//  //System.out.println("follow path start" + movingFrom.time + " " + GetSystemTime()) ;
		
				if (motion.inMotion){
					//If still in motion just change the velocity
					
					velocity = setVelocity() ; 		
				}
				else {
					//If robot is done with motion get the next waypoint
					
					if (GetSystemTime() < movingTo.time){
						//if robot is early
		//				//  //System.out.println("early not moving : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());
						gvh.sleep(10) ; 
					}
					else if ( GetSystemTime() > movingTo.time ){
						// if robot is not early
		//				//  //System.out.println("on time not moving : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());
						currentDesiredPath.remove(0);
						potentialLeaderPath.remove(0) ; 
						next = currentPath.removeFirst();

						movingFrom = movingTo;
						movingTo = next;
						velocity = setVelocity() ;
						
					}
				}
					
				if (next != null)
				{

	//				//  //System.out.println("final : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());					
	//				//  //System.out.println(" VElocity:" + velocity) ;
					goToWayPoint(next, velocity);
				}
				leaderStage = LEADERSTAGE.ACK_CHECK ;
				break ;

			}
		}
		
	}
	
	
	private int setVelocity(){
		//TODO
		int velocity  = VELOCITY_MIN; 
		int dx = movingTo.x - movingFrom.x ; 
		int dy = movingTo.y - movingFrom.y ; 
		int distance = (int) Math.sqrt( dx*dx + dy*dy) ;
		

		if(GetSystemTime() < movingFrom.time){
		//	System.out.println("robot is early!");
			//robot is early
			gvh.sleep(10);
		}
		
		else if ( GetSystemTime() < movingTo.time && GetSystemTime() >= movingFrom.time ){
			// if robot is on time!
		//	System.out.println("robot is on time!");

			
			
			if (distance == 0 )
				velocity = 1 ; 
			else {
				velocity = (int)( Math.abs(distance) * 1000/ CYCLE_LENGTH );
				
				if (velocity < VELOCITY_MIN)
					velocity = VELOCITY_MIN ; 
				
				else if (velocity > VELOCITY_MAX)
					velocity = VELOCITY_MAX ; 
			}
		}

		else
		{
			//If robot is not on time
			velocity = VELOCITY_MAX ; 
		}
	//	System.out.println("distance: " + distance + " velocity: " + velocity);
		
		return velocity;
	}
	
	private void sendPathsToFollowers(Path[] pathsToSend) {
		pathID++ ; 
		// send each robot the path
		for(int k = 0 ; k < nBot ; k++) {
			
			String msgContent = makePathMessageString(pathsToSend[k]); 					 
			RobotMessage path = new RobotMessage( participantsList.get(k) ,  
					gvh.id.getName(), MSG_PATH_UPDATE, msgContent);
			sendMsg(path) ;
			synchronized(ackReport){			
				ackReport[k].lastPathIdSent = pathID ; 
				ackReport[k].received = false ; 	
			}
		}
		

	}

	private String makePathMessageString(Path p) {
		String msg = ""; 
				
		msg += pathID ;

		for (WayPoint interWpt : p.path)
			msg += "," + interWpt.x  + ":" + interWpt.y  + ":" + interWpt.time ; 						

		return msg;
	}

	public void runFollowerLogic()
	{
		//TODO
		// go to the first waypoint
		WayPoint first = currentPath.removeFirst();
		WayPoint next = first ; 
		movingTo = next ; 
		
		goToWayPoint(next, VELOCITY_MAX);

		while(motion.inMotion)
			gvh.sleep(20) ;
		
		readyToStart = true;
		// wait until the start message is received

		while (!recievedStart){
			gvh.sleep(60); // was 20
		}
		
		while (currentPath.size() > 0)
		{
			
			int velocity = VELOCITY_MIN; 	
			//  //System.out.println("follow path start" + movingFrom.time + " "  + movingTo.time+ " " + GetSystemTime()) ;
	
			if (motion.inMotion){
				//If still in motion just change the velocity
				//  //System.out.println("in motion");
				velocity = setVelocity() ;
				
			//	//System.out.println(gvh.id.getName() + " in motion " +velocity) ; 
			}
			
			else {
				//If robot is done with motion get the next waypoint

		//		if(robotId == 3)
		//			//System.out.println(gvh.id.getName() +"not moving") ;
				
				if (GetSystemTime() < movingTo.time){
					//if robot is early
	//				//  //System.out.println("early not moving : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());
					gvh.sleep(10) ; 
				}
				else if ( GetSystemTime() > movingTo.time ){
						
					// if robot is not early
					//System.out.println("on time not moving : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());				
					next = currentPath.removeFirst();
					while(next.time < movingTo.time){
						next = currentPath.removeFirst();
					}

					movingFrom = movingTo;
					movingTo = next;
					velocity = setVelocity() ;
				}
			}
			
		//	if(robotId == 3)	
		//		//System.out.println("final : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());					
			
		//	if(robotId == 3)
			
		//		//System.out.println(" VElocity:" + velocity) ;

				//Check if the current path has been acknosledged or not
				//if not acknowledged then check if the robot is on the schedule or not
				//if on the schedule send the ACK to leader

			if (!pathAcked)
			{
				//System.out.println("path not acked");
				if (GetSystemTime() < movingTo.time && GetSystemTime() > movingFrom.time){
					gvh.comms.addOutgoingMessage(new RobotMessage(leaderName, gvh.id.getName(), MSG_PATH_ACK, robotId + "," + Integer.toString(currentPathID))) ;
					pathAcked = true ; 
					//System.out.println("path ack sent");
				}
				
			}

				
			goToWayPoint(next, velocity);
			gvh.sleep(100) ;
			

		}
	}

	private ArrayList <Line2D.Double> connectionLineGenerator(List <WayPoint> potentialLeaderPath , ArrayList <Line2D.Double> oldNewWptConnectors) {
		
		for (int i = 0 ; i < oldNewWptConnectors.size() ; i++){
//			////////  //System.out.println("salam: " + oldNewWptConnectors.size() + " " + interPath.size()) ;
			oldNewWptConnectors.get(i).x1 = potentialLeaderPath.get(i).x ; 
			oldNewWptConnectors.get(i).y1 = potentialLeaderPath.get(i).y ;

		}
		
		return oldNewWptConnectors ; 
		
	}

	private boolean interferes(Path[] pathsToCheck, Path[] potentialIntermediatePaths) {
			
		// returns true if there is any interference between the paths
		WayPoint currentWpt1 = null ; 
		WayPoint interWpt1 = null ; 
	//	////////  //System.out.println("interferenece check");
		
		

			
		
//		////////  //System.out.println("salam");
		
		for (int j = 0 ; j < numRobots ; j++)
			for (int k = j ; k < numRobots ; k++)
				if(j != k)
					for(int i = 0 ; i < potentialIntermediatePaths[j].path.size() ; i++){

						 
						WayPoint currentWpt2 = potentialIntermediatePaths[j].path.get(i) ; 

						int starting = Math.max(0, i-CHECK_WINDOW);
						int end  = Math.min(i+CHECK_WINDOW, pathsToCheck[j].path.size()) ;
						
							//////////  //System.out.println("starting: " + starting + " end: " + end) ;
							for(int p = starting ; p < end ; p ++ )
							{
								
								//////////  //System.out.println("i: " + i + " p: " + p) ;
								
								WayPoint intertWpt2 = pathsToCheck[k].path.get(p)  ;				
								if (currentWpt1 != null && interWpt1 != null){
									
									MinDist minDist = new MinDist(currentWpt1.x, currentWpt1.y, currentWpt2.x, currentWpt2.y, interWpt1.x, interWpt1.y, intertWpt2.x, intertWpt2.y) ;
									double dist = minDist.returnMinDit() ;

							
			//						System.out.println("start dis");
		//							MinDist minDist = new MinDist(100, 200, 100, 200, 400, 500, 400, 500) ;
	//								double dist = minDist.returnMinDit() ;
									
//									System.out.println("distance: " + dist);
									
									if (dist < SAFE_DISTANCE) {
										System.out.println("Interference!! dist: " + dist + " " + " j: " + j + " k: " + k + " "  + " i: " + i + " p: " + p + " "  +currentWpt1.x+ " " +  currentWpt1.y+ " " +  currentWpt2.x+ " " +  currentWpt2.y+ " " +  interWpt1.x+ " " +  interWpt1.y+ " " +  intertWpt2.x+ " " +  intertWpt2.y + " " + "starting: " + starting + " end: " + end) ;
		
										return true ; 
									}
										
	
								}
							
							if (p == end-1)
								interWpt1 = null ;
							else
								interWpt1 = intertWpt2 ;
						}	
							if (i == potentialIntermediatePaths[j].path.size() - 1)
								currentWpt1 = null ;
							else
								currentWpt1 = currentWpt2 ;
					}
		
		return false ; 
		
	}



	private boolean allAcksRecieved() {
		
		
		int counter = 0 ; 
		for(int j = 1 ; j < numRobots ; j++){

			//System.out.println(j+ " last path ID Sent: " + ackReport[j].lastPathIdSent + " lastPathIdReceived: " + ackReport[j].lastPathIdReceived + " ackReport[j].received " +ackReport[j].received) ;
			if(ackReport[j].received == true && ackReport[j].lastPathIdSent == ackReport[j].lastPathIdReceived)
				counter++ ; 
		}

		
		
		if (counter == numRobots - 1){
			missedAckCounter = 0 ; 
			return true;
		}
			
		missedAckCounter++ ; 
		return false;
	}

	

	private boolean acceptNewDetourPoint(){

		if (missedAckCounter > 20)
			return false ; 
		else 
			return true ; 
	
	}

	private void goToWayPoint(WayPoint next, int velocity) 
	{
		int angle = 0; 
		ItemPosition ip = new ItemPosition("waypoint", next.x, next.y, angle);
		
		MotionParameters motionParam = new MotionParameters();

		motionParam.LINSPEED_MAX = velocity ; 
		motionParam.LINSPEED_MIN = velocity ; 

		//motion.goTo(ip);
		
		
		motion.goTo(ip, motionParam) ; 
		

	}
	
	public long GetSystemTime(){
		
		return (gvh.time() - initialTime ); 
	}
	
	public long GetCycleTime(){
		
		//returns the time that has elapsed of the current cycle
		return (GetSystemTime() % CYCLE_LENGTH) ;
		
	}
	
	public long GetCycleNumber(){
		return (long)Math.floor((GetSystemTime() / CYCLE_LENGTH));
	}
	
	
	public WayPoint NextRight(WayPoint desiredDistance, long time, WayPoint lastIdealRight, int angle ){
		

		
		WayPoint position = new WayPoint(0,0,0);
		position.x = (int) ( (double) lastIdealRight.x  + (double)(desiredDistance.x) * (double)Math.sin(Math.toRadians((double)angle)) - desiredDistance.y*Math.cos(Math.toRadians((double) angle)) );
		position.y = (int) ( (double) lastIdealRight.y - (double)(desiredDistance.x) * Math.cos(Math.toRadians((double) angle)) - desiredDistance.y*Math.sin(Math.toRadians((double) angle))) ;
		position.time = (int)time ; 
		
//		////////  //System.out.println("right " + lastIdealRight + " " + position);
		
		
		return position;
		
	}
	
	
	public WayPoint NextLeft(WayPoint desiredDistance, long time, WayPoint lastIdealLeft, int angle){
		
		WayPoint position = new WayPoint(0,0,0); 	
		position.x = (int) ( (double) lastIdealLeft.x - (double)(desiredDistance.x) * (double)Math.sin(Math.toRadians((double)angle)) -desiredDistance.y*Math.cos(Math.toRadians((double) angle)) );
		position.y = (int) ( (double) lastIdealLeft.y + (double)(desiredDistance.x) * Math.cos(Math.toRadians((double) angle))  - desiredDistance.y*Math.sin(Math.toRadians((double) angle))) ;
		position.time = (int)time ;
		
	//	////////  //System.out.println("left " + lastIdealLeft + " " + position);
		//////////  //System.out.println(( (double) lastIdealLeft.x - (double)(desiredDistance.x) * (double)Math.cos(Math.toRadians((double)angle)) +desiredDistance.y*Math.sin(Math.toRadians((double) angle)) ));
		return position ; 
	}

	public ArrayList <Line2D.Double> connectorLineGenerator(
			ArrayList <Line2D.Double> oldNewWptConnectors1,
			LinkedList<WayPoint> currentPath1,
			ArrayList<WayPoint> currentDesiredPath1) 
	{		
		
		
		//generate the connector lines
		oldNewWptConnectors1.clear() ; 
		
		int middle ; 
		
		////////  //System.out.println("currentpath size: " + currentPath.size());
		
		if (currentPath1.size() == currentDesiredPath1.size())
		{
			for(int j = 0 ; j < currentPath1.size() ; j++)
				oldNewWptConnectors1.add( new Line2D.Double(currentPath1.get(j).x, currentPath1.get(j).y, currentDesiredPath1.get(j).x, currentDesiredPath1.get(j).y)) ;				
		}
		else if (currentPath1.size() < currentDesiredPath1.size()){
			if (currentPath1.size()%2 == 0 ) {
				middle  = currentPath1.size()/2 - 1 ; 
//				////////  //System.out.println("middle: " + middle);
				
				for (int j = 0 ; j <= middle ;  j ++) {  
					oldNewWptConnectors1.add( new Line2D.Double(currentPath1.get(j).x, currentPath1.get(j).y, currentDesiredPath1.get(j).x, currentDesiredPath1.get(j).y)) ;

//					////////  //System.out.println(j); 
				}

//						////////  //System.out.println("salam1");
				//all the extra waypoints on the new path are connected to the last waypoint on the old path
				if (currentPath1.size() < currentDesiredPath1.size()){ 
					for (int j = middle +1 ; j < currentDesiredPath1.size() - middle - 1  ;  j ++)
					{
						oldNewWptConnectors1.add(new Line2D.Double(currentPath1.get(middle).x, currentPath1.get(middle).y , currentDesiredPath1.get(j).x, currentDesiredPath1.get(j).y)) ;
//						////////  //System.out.println(j);
					}
					
				}
//						////////  //System.out.println("salam2");
				for (int j =  middle +1 ; j < currentPath1.size() ;  j ++) {
					
					int k = currentDesiredPath1.size() - 2 * middle -2+ j ; 
//					////////  //System.out.println(j + " " + k );
					oldNewWptConnectors1.add( new Line2D.Double(currentPath1.get( j).x, currentPath1.get(j).y, currentDesiredPath1.get(currentDesiredPath1.size() - 2 * middle -2+ j).x, currentDesiredPath1.get(currentDesiredPath1.size() - 2 * middle -2 +  j).y)) ;
					
				}

				
			}
			else {
				
				middle  = currentPath1.size()/2  ; 
				
				
				////////  //System.out.println("middle: " + middle);
				for (int j = 0 ; j < middle ;  j ++) {  
					oldNewWptConnectors1.add( new Line2D.Double(currentPath1.get(j).x, currentPath1.get(j).y, currentDesiredPath1.get(j).x, currentDesiredPath1.get(j).y)) ;

					////////  //System.out.println(j); 
				}
				
				
				
				int counter = 0 ; 
				////////  //System.out.println("salam1");
				//all the extra waypoints on the new path are connected to the last waypoint on the old path
				if (currentPath1.size() < currentDesiredPath1.size()){
					for (int j = middle ; j < currentDesiredPath1.size() - (middle + 1)  ;  j ++)
					{
						oldNewWptConnectors1.add(new Line2D.Double(currentPath1.get(middle).x, currentPath1.get(middle).y , currentDesiredPath1.get(j).x, currentDesiredPath1.get(j).y)) ;
						////////  //System.out.println(j);
						counter ++ ; 
					}
					
				}
				
				int k  = middle + counter ; 
				////////  //System.out.println("salam2");
			
				for (int j = middle ; j < currentPath1.size() ;  j ++) {
					////////  //System.out.println(j + " " + k);
					oldNewWptConnectors1.add( new Line2D.Double(currentPath1.get(j).x, currentPath1.get(j).y, currentDesiredPath1.get(k).x, currentDesiredPath1.get(k).y)) ;
					k++ ; 
										
				
				}
				
			}
			
		}
		else {
			//when currentPath.size() > currentDesiredPath.size()
			//////  //System.out.println("here!") ;
			int step = (int) Math.ceil(  currentDesiredPath1.size()/(currentPath1.size() - currentDesiredPath1.size()) ) ;
			int counter = 0 ; 

			//////  //System.out.println("step: " + step) ; 
			//////  //System.out.println( currentDesiredPath.size()) ; 
			
			
			for(int j = 0 ; j < currentPath1.size() ; j++){
	
				//////  //System.out.println("counter: " + counter + "j: " + j) ;
				
				if ( j%step == 0 && counter != currentPath1.size() - currentDesiredPath1.size())
					counter++ ;
				
				else
					oldNewWptConnectors1.add( new Line2D.Double(currentPath1.get(j).x, currentPath1.get(j).y, currentDesiredPath1.get(j-counter).x, currentDesiredPath1.get(j-counter).y)) ;
				
			}
			
					
		}
			

		return oldNewWptConnectors1 ;
	//	////////  //System.out.println("current path size : "  + currentPath.size()) ; 
	}
	
	
	public ArrayList <WayPoint> generatePotentialLeaderPath(int increasePercent, ArrayList<WayPoint> currentDesiredPath1 ,ArrayList<Line2D.Double> oldNewWptConnectors1) {
		ArrayList <WayPoint> rv = new ArrayList <WayPoint>();
		
//		////////  //System.out.println("interPathGen currentpath size:" + currentPath.size() + " newPathsize: " + currentDesiredPath.size() + " old to new size: " + oldNewWptConnectors.size()) ;
		
		if (currentDesiredPath1.size() != oldNewWptConnectors1.size())
			throw new RuntimeException("size mismatch");
		
		for (int j = 0 ; j < currentDesiredPath1.size() ; j ++ ) {
			rv.add(new WayPoint( (int) (oldNewWptConnectors1.get(j).x1 + increasePercent *(oldNewWptConnectors1.get(j).x2-oldNewWptConnectors1.get(j).x1)/100) , (int) (oldNewWptConnectors1.get(j).y1 + increasePercent * (oldNewWptConnectors1.get(j).y2 - oldNewWptConnectors1.get(j).y1)/100), currentDesiredPath1.get(j).time ));
		}
		
		return rv;
	}
	
	
	public void sendMsg(RobotMessage msg) { 
		
		for (int j = 0 ; j < MSG_SENDING_NUMBER ; j ++) {
		
			gvh.comms.addOutgoingMessage(msg);
		}
		
		
	}
	
	/**
	 * Generate follower paths from leader path
	 * @param desiredDistance
	 * @param leaderPath
	 * @return an array of paths with each index = robotIndex
	 */
	public Path[] followerPathGeneration(WayPoint desiredDistance, List <WayPoint> leaderPath ) 
	{
		Path[] rv = new Path[numRobots];
		
		int angleInit = (int) 
				Math.toDegrees( Math.atan2( leaderPath.get(1).y - leaderPath.get(0).y , 
						leaderPath.get(1).x - leaderPath.get(0).x ) );
		int angle;
		int angle0 , angle1 , angle2 , angle3,  avgAngle =0;
		
		angle0 = (int) 
				Math.toDegrees( Math.atan2( leaderPath.get(1).y - leaderPath.get(0).y , 
						leaderPath.get(1).x - leaderPath.get(0).x ) );
		
		angle1 = (int) 
				Math.toDegrees( Math.atan2( leaderPath.get(2).y - leaderPath.get(1).y , 
						leaderPath.get(2).x - leaderPath.get(1).x ) );
		
		angle2 = (int) 
				Math.toDegrees( Math.atan2( leaderPath.get(3).y - leaderPath.get(2).y , 
						leaderPath.get(3).x - leaderPath.get(2).x ) );
		angle3 = (int) 
				Math.toDegrees( Math.atan2( leaderPath.get(4).y - leaderPath.get(3).y , 
						leaderPath.get(4).x - leaderPath.get(3).x ) );

		
		double xOld =0, yOld=0, xNew, yNew ;
		
		xOld = leaderPath.get(0).x; 
		yOld = leaderPath.get(0).y; 
		
//		////////  //System.out.println("init angle: " + angle +" " +  leaderPath.get(1).y +" " +  leaderPath.get(0).y  +" " + leaderPath.get(1).x  +" " + leaderPath.get(0).x) ; 
		
		for(int j = 0 ; j < nBot ; j ++ ) 
			rv[j] = new Path(); 

		int counter = 0 ; 	
		
		for (WayPoint wpt : leaderPath)
		{
			
			rv[0].path.add(wpt) ;
			xNew = (double) wpt.x ;
			yNew = (double) wpt.y ; 

			if(counter == 0)
				angle = angleInit ; 
			else 
				angle =(int) Math.toDegrees( Math.atan2( yNew - yOld , xNew - xOld ) );

			if (counter > 3)
				avgAngle  = (angle0 + angle1 + angle2 + angle3 + angle)/5 ; 
			
			WayPoint lastRight = new WayPoint(wpt.x, wpt.y, wpt.time);
			WayPoint lastLeft = new WayPoint(wpt.x, wpt.y, wpt.time); 
			
/*			System.out.println("counter: " + counter + " angle: " + angle ) ; 
			System.out.print(  yNew - yOld);
			System.out.print(" ") ; 
			System.out.print( xNew - xOld);
			System.out.print(" ") ;
*/			
			for (int j = 0 ; (2*j+1)<nBot ; j++){   
				lastRight = NextRight(desiredDistance, (long) wpt.time, lastRight, angle );
				rv[2*j+1].path.add(lastRight);
			}
			
			for (int j = 1 ; (2*j)<nBot ; j++){
				lastLeft  = NextLeft(desiredDistance, (long) wpt.time, lastLeft, angle );
				rv[2*j].path.add(lastLeft); 
			}
			xOld = (double)wpt.x ; 
			yOld = (double)wpt.y ;
			
			if (counter > 3 ){
				angle3 = angle ; 
				angle2 = angle3 ;
				angle1 = angle2 ; 
				angle0 = angle1 ; 
			}

			
			counter ++ ; 
		}
		
		
		
		//all the follower paths are in the rv
		WayPoint wptA1 = null ; 
		WayPoint wptB1 = null ; 
		
				for (int j = 1 ; j < numRobots ; j++) {
//TODO
							for(int i = 0 ; i < rv[j].path.size() - SELF_INTEREFERENCE_CHECK_WINDOW ; i++){
								 
								WayPoint wptA2 = rv[j].path.get(i) ; 

								int starting =  i + SELF_INTEREFERENCE_CHECK_DELAY ;
								int end  = 	i+SELF_INTEREFERENCE_CHECK_WINDOW ; 
								
							//	System.out.println("i: " + i + " start: " + starting + " end: " + end);
									//////////  //System.out.println("starting: " + starting + " end: " + end) ;
									for(int p = starting ; p < end ; p ++ )
									{
									
										//////////  //System.out.println("i: " + i + " p: " + p) ;
										
										WayPoint wptB2 = rv[j].path.get(p)  ;
										
										if (wptA1 != null && wptB1 != null){			
											MinDist minDist = new MinDist(wptA1.x, wptA1.y, wptA2.x, wptA2.y, wptB1.x, wptB1.y, wptB2.x, wptB2.y) ;
											double dist = minDist.returnMinDit() ;

												
											if (dist == 0 && i!= p) { // && minDist.doesIntersect()) { 

											//	System.out.println("internal interference " + j + " " + i + " " + p + " " + dist);
  

												for (int  k = i  ; k <= p ; k++){
											//		System.out.println(k + " "  + j);
											//		System.out.println(rv[j].path.get(k).x + " " + rv[j].path.get(k).y);
											//		System.out.println(rv[j].path.get(i).x + " " + rv[j].path.get(i).y);

													rv[j].path.get(k).x = rv[j].path.get(i).x;  
													rv[j].path.get(k).y = rv[j].path.get(i).y ;
													
													
												}
												
												if (p+1 < rv[j].path.size() )
													i = p + 1 ; 
												
										//		gvh.sleep(100000);
										}
								}
										
										if (p == end-1)
											wptB1 = null ;
										else
											wptB1 = wptB2 ;
										
									
									}
									if (i == rv[j].path.size() - 1)
										wptA1 = null ;
									else
										wptA1 = wptA2 ;
							}
				}
		
				
				
		return rv;
	}
		
	public void ackProcess(ArrayList <WayPoint> receivedPath)
	{
		
		
//		////////  //System.out.println(GetCycleNumber() + )

//			gvh.comms.addOutgoingMessage(new RobotMessage(leaderName, gvh.id.getName(), MSG_PATH_ACK, robotId + "," + Integer.toString(pathId))) ;
		
		pathAcked = false ; 
		
		if (currentPathID < newPathID) {
			currentPath.clear();
			currentPath.addAll(receivedPath);		
			currentPathID = newPathID ;
			
		}
	}
	
	@Override
	public void messageReceied(RobotMessage m) {
		//  //System.out.println( m.getFrom() + " " + m.getMID());

		if (m.getMID() == MSG_SYNC_TIME)
		{
			// followers received sync message
			initialTime = gvh.time();
			
			// send ready message if the robot is in position
			if (readyToStart)
			{
				int syncId = Integer.parseInt(m.getContents(0));
				
				gvh.comms.addOutgoingMessage(new RobotMessage(leaderName, robotName, MSG_READY, "" + syncId));
			}
		}
		
		else if (m.getMID() == MSG_READY)
		{
			int msgSyncId = Integer.parseInt(m.getContents(0));
			
			if (msgSyncId == syncId)
				++readyCount;
		}
		
		else if (m.getMID() == MSG_START)
		{
			// all robots are in position, start!
			recievedStart = true ;
		}
		else if (m.getMID() == MSG_PATH_UPDATE )
		{
			
			// follower received a new path
			currentPathID = newPathID ; 
			String[] waypoints = m.getContents(0).split(",") ;
			String[] wptData ; 
			ArrayList <WayPoint> receivedPath = new ArrayList <WayPoint>() ;
							
			newPathID =  Integer.valueOf(waypoints[0]) ; 
	//		System.out.println(m.getContents(0)); 
			
			for(int j = 1 ; j <waypoints.length; j ++) { 
				
				String wpt = waypoints[j];
				wptData = wpt.split(":") ; 	
				receivedPath.add(new WayPoint( Integer.valueOf(wptData[0] ),Integer.valueOf(wptData[1]) , Integer.valueOf(wptData[2]) )) ; 
			}
		//	System.out.println(receivedPath.size());

			ackProcess(receivedPath) ; 
			//System.out.println("new PAth " + robotId + " " + newPathID + " " +  currentPathID);

		}
		
		else if( m.getMID() == MSG_PATH_ACK ){
			// leader gets a path ack
			
			String[] ackContent = m.getContents(0).split(",");
			int ackSenderId = Integer.valueOf(ackContent[0]) ; 
			int ackId = Integer.valueOf(ackContent[1]);

			synchronized(ackReport){
				ackReport[ackSenderId].received = true; 
				if (ackId > ackReport[ackSenderId].lastPathIdReceived)
					ackReport[ackSenderId].lastPathIdReceived = ackId ; 
			}
			
			//System.out.println( m.getContents(0));
		}
		
		
	}

	private boolean fullCheck(ArrayList<WayPoint> currentDesiredPath1, LinkedList<WayPoint> currentPathOriginal) {
		// TODO Auto-generated method stub
		/**
		 * returns true if the detour point is feasibile,
		 */
		
		boolean rv = true ; 
	
		ArrayList <Line2D.Double> oldNewWptConnectors1 = new ArrayList<Line2D.Double>();;
		ArrayList <WayPoint> potentialLeaderPath1 ;
		Path[] potentialIntermediatePaths1 = null ; 

		LinkedList <WayPoint> currentPath1 = new LinkedList<WayPoint>() ; 
		currentPath1.addAll(currentPathOriginal) ;

		
		
		for (int c = 0 ; c < 12 ; c++){
			System.out.println("new calculation!");			
			oldNewWptConnectors1 = connectorLineGenerator(oldNewWptConnectors1, currentPath1, currentDesiredPath1) ; 

			int incPercent = PERCENT ;
			for(int k = 0 ; k < 11 ; k++){
				
				potentialLeaderPath1 = generatePotentialLeaderPath(incPercent,currentDesiredPath1 ,oldNewWptConnectors1) ;
				potentialIntermediatePaths1 = followerPathGeneration(desiredDistance, potentialLeaderPath1)  ;

				
				
				if (interferes(potentialIntermediatePaths1, potentialIntermediatePaths1)){
					//System.out.println("intereference! " + k);
					incPercent = incPercent * 9/10 ;
					
				}
				else {
					//		System.out.println("no interference!");
					oldNewWptConnectors1 = connectionLineGenerator(potentialLeaderPath1, oldNewWptConnectors1);				
					// new paths is okay! assign it to the current path
					currentPath1.clear();
					currentPath1.addAll(potentialLeaderPath1);

				}
				
				if (incPercent == 0 ){
					rv = false ;
				}
					 
			}
		}

		
		
		System.out.println("The value of rv is: " + rv);
		return rv;
	}

	@Override
	public void receivedPointInput(int x, int y)
	{
		//	////////  //System.out.println("Point Received!");
		
		System.out.println("position: " + x + " " + y + "System cycle: " + GetCycleNumber());
		
		if (robotId == 0)
			if(x > gvh.gps.getMyPosition().x + 100 &&  y < gvh.gps.getMyPosition().y + 3000 && y > gvh.gps.getMyPosition().y - 3000) 
				if( acceptNewDetourPoint() ){
					{
						double ANCHOR_DISTANCE = 1000;
						double SEPARATION = 100;
						
						ArrayList <WayPoint> oldPath = new ArrayList <WayPoint>();
						oldPath.addAll(currentPath);
						Point2D.Double detourPoint = new Point2D.Double(x, y);
						
						ArrayList <WayPoint> newPath = 	
								ArcCreator.createNewPath(oldPath, detourPoint, ANCHOR_DISTANCE, SEPARATION) ;
						
						//				This part works instead of zhenqi's
						/*				
						ArrayList <WayPoint> newPath = new ArrayList <WayPoint>() ; 
						ArrayList<Line2D.Double> oldNewWptConnectors = new ArrayList<Line2D.Double>();
						////////  //System.out.println("New point recieved.");
						
						for(int j = 0 ; j < currentPath.size() ; j++){
							newPath.add(new WayPoint(currentPath.get(j).x , currentPath.get(j).y + j* 30 , currentPath.get(j).time )) ;
						}
		 */	

						newPoint.x = x ; 
						newPoint.y = y ; 
						
//		if (fullCheck(newPath, currentPath) == true)
		
						if (newPath != null)
							this.receivedDesiredPath = newPath;				
					}
				
				}
				else
					System.out.println("not all acks are received!");

			else
				System.out.println("the Point is too close!");
	}

	
	
		public void testPointInsert(int x, int y)
		{
			//TODO Copy this to the original function
			if (robotId == 0)
				if( acceptNewDetourPoint()){
					if (true)
					{
						double ANCHOR_DISTANCE = 1000;
						double SEPARATION = 100;
						
						ArrayList <WayPoint> oldPath = new ArrayList <WayPoint>();
						oldPath.addAll(currentPath);
						Point2D.Double detourPoint = new Point2D.Double(x, y);
						
						ArrayList <WayPoint> newPath = 	
						ArcCreator.createNewPath(oldPath, detourPoint, ANCHOR_DISTANCE, SEPARATION) ;
						
						newPoint.x = x ; 
						newPoint.y = y ; 
						
						if (fullCheck(newPath, currentPath) == true)
							if (newPath != null)
								this.receivedDesiredPath = newPath;				
					}

				}
				else
					System.out.println("not all acks are received!");
	}
	
		
	
	
}






