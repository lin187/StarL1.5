package edu.illinois.mitra.starlSim;





import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.RobotMotion;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimSettings;

public class DeereFlockingWithDetours extends LogicThread implements MessageListener {

	//////////// CONSTANTS /////////////////
	private static final int PERCENT = 10 ; 
	private static final int SAFE_DISTANCE  = 100; 
	
		
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
	
	private static final long cycleLength  = 200 ;
	private static final int CHECK_WINDOW = 50;


	private static final int MSG_SENDING_NUMBER = 1 ;
	private static final int VELOCITY_CONSTANT = 40 ; 
	private static final int VELOCITY_MAX = 1000 ; 
	private static final int VELOCITY_MIN = 250 ; 
	
	
	
	/////////// SHARED VARIABLES BETWEEN LEADER AND FOLLOWERS //////////
	LinkedList <WayPoint> currentPath = new LinkedList <WayPoint>(); 
	
	ArrayList <String> participantsList = new ArrayList<String>() ;
	
	private RobotMotion motion;
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
		final int INIT_PATH_DELTA_DIST = 100; // distance per offline path waypoint
		final int INIT_PATH_DELTA_TIME = 1000; // time per offline path waypoint
		final int INIT_PATH_POINTS = 150;
		final Point INIT_PATH_START = new Point(0,500); // initial point for the offline path
		
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
				
				
				startingCycle = GetCycleNumber() ;

				currentDesiredPath = receivedDesiredPath ; 					
				
				connectorLineGenerator();
				//////  //System.out.println("before loop  current path size :" + currentPath.size() + " currentDesiredPath size: " + currentDesiredPath.size() + " oldNewWptConnectors size: " + oldNewWptConnectors.size()) ;
				
				int incPercent = PERCENT ;
				for(int k = 0 ; k < 11 ; k++){

					potentialLeaderPath = generatePotentialLeaderPath(incPercent);

					potentialIntermediatePaths = followerPathGeneration(desiredDistance, potentialLeaderPath)  ;

					if (interferes(potentialIntermediatePaths, potentialIntermediatePaths)){
						System.out.println("intereference! " + k);
						incPercent = incPercent * 9/10 ;
						gvh.sleep(30) ;
						
					}
					else {
						
						connectionLineGenerator(potentialLeaderPath);
						
						// new paths is okay! assign it to the current path
						currentPath.clear();
						currentPath.addAll(potentialLeaderPath);

						// broadcast potentialIntermediate paths to the other robots
						sendPathsToFollowers(potentialIntermediatePaths);
						
						break ;
					}
				}
				

				leaderStage = LEADERSTAGE.ACK_CHECK ; 
				break ; 
				
				
			case ACK_CHECK:

				
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
		
		int velocity  = VELOCITY_MIN; 
		int distance = (int) Math.sqrt( Math.abs((movingTo.x - movingFrom.x)^2 + (movingTo.y - movingFrom.y)^2 )) ;
		

		if(GetSystemTime() < movingFrom.time){
			//robot is early
	//		//  //System.out.println("early! : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());
			gvh.sleep(10);
		}
		
		else if ( GetSystemTime() < movingTo.time && GetSystemTime() >= movingFrom.time ){
			// if robot is on time!
	//		//  //System.out.println("on time : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());

			if (distance == 0 )
				velocity = VELOCITY_MIN ; 
			else {
				velocity =  VELOCITY_CONSTANT * distance ;
				if (velocity < VELOCITY_MIN)
					velocity = VELOCITY_MIN ; 
				else if (velocity > VELOCITY_MAX)
					velocity = VELOCITY_MAX ; 
			}
		}

		else
		{
			//If robot is not on time
	//		//  //System.out.println("late : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());
			velocity = VELOCITY_MAX ; 
		}

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
	//				//  //System.out.println("on time not moving : " +movingFrom.time + " " + movingTo.time + " " + GetSystemTime());				
					next = currentPath.removeFirst();

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

	private void connectionLineGenerator(List <WayPoint> potentialLeaderPath) {
		
		for (int i = 0 ; i < oldNewWptConnectors.size() ; i++){
//			////////  //System.out.println("salam: " + oldNewWptConnectors.size() + " " + interPath.size()) ;
			oldNewWptConnectors.get(i).x1 = potentialLeaderPath.get(i).x ; 
			oldNewWptConnectors.get(i).y1 = potentialLeaderPath.get(i).y ;

		}
		
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

							
									
									if (dist < SAFE_DISTANCE) {
					//					////////  //System.out.println("Interference!! dist: " + dist + " " + " j: " + j + " k: " + k + " "  + " i: " + i + " p: " + p + " "  +currentWpt1.x+ " " +  currentWpt1.y+ " " +  currentWpt2.x+ " " +  currentWpt2.y+ " " +  interWpt1.x+ " " +  interWpt1.y+ " " +  intertWpt2.x+ " " +  intertWpt2.y + " " + "starting: " + starting + " end: " + end) ;
		
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

		
		
		if (counter == numRobots - 1) 
			return true;

		return false;
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
		return (GetSystemTime() % cycleLength) ;
		
	}
	
	public long GetCycleNumber(){
		return (long)Math.floor((GetSystemTime() / cycleLength));
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

	public void connectorLineGenerator() 
	{		
		//generate the connector lines
		oldNewWptConnectors.clear() ; 
		
		int middle ; 
		
		////////  //System.out.println("currentpath size: " + currentPath.size());
		
		if (currentPath.size() == currentDesiredPath.size())
		{
			for(int j = 0 ; j < currentPath.size() ; j++)
				oldNewWptConnectors.add( new Line2D.Double(currentPath.get(j).x, currentPath.get(j).y, currentDesiredPath.get(j).x, currentDesiredPath.get(j).y)) ;				
		}
		else if (currentPath.size() < currentDesiredPath.size()){
			if (currentPath.size()%2 == 0 ) {
				middle  = currentPath.size()/2 - 1 ; 
//				////////  //System.out.println("middle: " + middle);
				
				for (int j = 0 ; j <= middle ;  j ++) {  
					oldNewWptConnectors.add( new Line2D.Double(currentPath.get(j).x, currentPath.get(j).y, currentDesiredPath.get(j).x, currentDesiredPath.get(j).y)) ;

//					////////  //System.out.println(j); 
				}

//						////////  //System.out.println("salam1");
				//all the extra waypoints on the new path are connected to the last waypoint on the old path
				if (currentPath.size() < currentDesiredPath.size()){ 
					for (int j = middle +1 ; j < currentDesiredPath.size() - middle - 1  ;  j ++)
					{
						oldNewWptConnectors.add(new Line2D.Double(currentPath.get(middle).x, currentPath.get(middle).y , currentDesiredPath.get(j).x, currentDesiredPath.get(j).y)) ;
//						////////  //System.out.println(j);
					}
					
				}
//						////////  //System.out.println("salam2");
				for (int j =  middle +1 ; j < currentPath.size() ;  j ++) {
					
					int k = currentDesiredPath.size() - 2 * middle -2+ j ; 
//					////////  //System.out.println(j + " " + k );
					oldNewWptConnectors.add( new Line2D.Double(currentPath.get( j).x, currentPath.get(j).y, currentDesiredPath.get(currentDesiredPath.size() - 2 * middle -2+ j).x, currentDesiredPath.get(currentDesiredPath.size() - 2 * middle -2 +  j).y)) ;
					
				}

				
			}
			else {
				
				middle  = currentPath.size()/2  ; 
				
				
				////////  //System.out.println("middle: " + middle);
				for (int j = 0 ; j < middle ;  j ++) {  
					oldNewWptConnectors.add( new Line2D.Double(currentPath.get(j).x, currentPath.get(j).y, currentDesiredPath.get(j).x, currentDesiredPath.get(j).y)) ;

					////////  //System.out.println(j); 
				}
				
				
				
				int counter = 0 ; 
				////////  //System.out.println("salam1");
				//all the extra waypoints on the new path are connected to the last waypoint on the old path
				if (currentPath.size() < currentDesiredPath.size()){
					for (int j = middle ; j < currentDesiredPath.size() - (middle + 1)  ;  j ++)
					{
						oldNewWptConnectors.add(new Line2D.Double(currentPath.get(middle).x, currentPath.get(middle).y , currentDesiredPath.get(j).x, currentDesiredPath.get(j).y)) ;
						////////  //System.out.println(j);
						counter ++ ; 
					}
					
				}
				
				int k  = middle + counter ; 
				////////  //System.out.println("salam2");
			
				for (int j = middle ; j < currentPath.size() ;  j ++) {
					////////  //System.out.println(j + " " + k);
					oldNewWptConnectors.add( new Line2D.Double(currentPath.get(j).x, currentPath.get(j).y, currentDesiredPath.get(k).x, currentDesiredPath.get(k).y)) ;
					k++ ; 
										
				
				}
				
			}
			
		}
		else {
			//when currentPath.size() > currentDesiredPath.size()
			//////  //System.out.println("here!") ;
			int step = (int) Math.ceil(  currentDesiredPath.size()/(currentPath.size() - currentDesiredPath.size()) ) ;
			int counter = 0 ; 

			//////  //System.out.println("step: " + step) ; 
			//////  //System.out.println( currentDesiredPath.size()) ; 
			
			
			for(int j = 0 ; j < currentPath.size() ; j++){
	
				//////  //System.out.println("counter: " + counter + "j: " + j) ;
				
				if ( j%step == 0 && counter != currentPath.size() - currentDesiredPath.size())
					counter++ ;
				
				else
					oldNewWptConnectors.add( new Line2D.Double(currentPath.get(j).x, currentPath.get(j).y, currentDesiredPath.get(j-counter).x, currentDesiredPath.get(j-counter).y)) ;
				
			}
			
					
		}
			

		
	//	////////  //System.out.println("current path size : "  + currentPath.size()) ; 
	}
	
	
	public ArrayList <WayPoint> generatePotentialLeaderPath(int increasePercent) {
		ArrayList <WayPoint> rv = new ArrayList <WayPoint>();
		
//		////////  //System.out.println("interPathGen currentpath size:" + currentPath.size() + " newPathsize: " + currentDesiredPath.size() + " old to new size: " + oldNewWptConnectors.size()) ;
		
		if (currentDesiredPath.size() != oldNewWptConnectors.size())
			throw new RuntimeException("size mismatch");
		
		for (int j = 0 ; j < currentDesiredPath.size() ; j ++ ) {
			rv.add(new WayPoint( (int) (oldNewWptConnectors.get(j).x1 + increasePercent *(oldNewWptConnectors.get(j).x2-oldNewWptConnectors.get(j).x1)/100) , (int) (oldNewWptConnectors.get(j).y1 + increasePercent * (oldNewWptConnectors.get(j).y2 - oldNewWptConnectors.get(j).y1)/100), currentDesiredPath.get(j).time ));
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

			WayPoint lastRight = new WayPoint(wpt.x, wpt.y, wpt.time);
			WayPoint lastLeft = new WayPoint(wpt.x, wpt.y, wpt.time); 
			

			
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
			

			
			counter ++ ; 
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
			
			for(int j = 1 ; j <waypoints.length; j ++) { 
				
				String wpt = waypoints[j];
				wptData = wpt.split(":") ; 	
				receivedPath.add(new WayPoint( Integer.valueOf(wptData[0] ),Integer.valueOf(wptData[1]) , Integer.valueOf(wptData[2]) )) ; 
			}

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
	@Override
	public void receivedPointInput(int x, int y)
	{
	//	////////  //System.out.println("Point Received!");
	
		if (robotId == 0)
			if(allAcksRecieved())
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
				if (newPath != null)
					this.receivedDesiredPath = newPath;				
			}
			else
				System.out.println("not all acks are received!");
	//////  //System.out.println("Point Received!");
	}

}





