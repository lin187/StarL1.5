//package edu.illinois.mitra.test;
package edu.illinois.mitra.starlSim.simapps.deere_fardin;




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
	
	private static final long cycleLength  = 800 ;
	private static final int CHECK_WINDOW = 50;


	private static final int MSG_SENDING_NUMBER = 1 ;
	
	
	/////////// SHARED VARIABLES BETWEEN LEADER AND FOLLOWERS //////////
	private LinkedList <WayPoint> currentPath = new LinkedList <WayPoint>(); 
	private ArrayList <WayPoint> newPath = new ArrayList <WayPoint>();
	ArrayList <String> participantsList = new ArrayList<String>() ;
	
	private RobotMotion motion;
	private int robotId; // robot 0 = leader
	private String robotName;
	private int numRobots;
	private long initialTime; 
	private String leaderName ;
	
	WayPoint movingFrom = null;
	WayPoint movingTo = null;
	int pathID = 0 ;	
	
	//////////// LEADER VARIABLES /////////////////

	ArrayList <WayPoint> interPath = new ArrayList <WayPoint>() ;
	ArrayList<Line2D.Double> oldNewWptConnectors = new ArrayList<Line2D.Double>();
	private enum LEADERSTAGE {FOLLOW_PATH, PATH_UPDATE, START, SEND_PATH, PATH_GENERATION, ACK_CHECK, RESEND_PATH, DONE, FOLLOW_PATH_WITHOUT_ACK} ;  
	private LEADERSTAGE leaderStage = LEADERSTAGE.START ; 
	Path[] paths ; 
	Path[] interPathsTemp ;
	AckReport[] ackReport;  
	long startingCycle ; 
	
	int syncId = 0; // used to make sure the ready message correspond to the most recent sync
	int readyCount = 0; // number of robots which responded with the correct sync id
	
	
	
	/////////// FOLLOWER VARIABLES ///////////////
	private WayPoint desiredDistance = new WayPoint(400, 0, 0);
	
	boolean recievedStart = false ; 
	boolean readyToStart = false;
	
	
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
		interPathsTemp = new Path[numRobots] ;
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
		
		createInitialRobotPath();
		
		// assign initial position
		ItemPosition pos = gvh.gps.getMyPosition();
		movingFrom = movingTo = new WayPoint(pos.x, pos.y, 0);
		
		for ( WayPoint wpt:currentPath){
			newPath.add(new WayPoint(wpt.x, wpt.y, wpt.time));
		}	
		
	}
	
	private int getRobotId() {
		System.out.println("Simulator only: using hardcoded robot names to extract robot ids...");
		String robotNumStr = name.substring(SimSettings.BOT_NAME.length());
		
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
			int time = n * INIT_PATH_DELTA_TIME;
			
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
		// go to the first waypoint
		WayPoint first = currentPath.removeFirst();
		goToWayPoint(first);
		
		// send synchronize messages until all followers are in position
		while (readyCount != numRobots - 1)
		{
			readyCount = 0;
			
			gvh.comms.addOutgoingMessage(new RobotMessage("ALL", robotName, MSG_SYNC_TIME, "" + (++syncId)));
			initialTime = gvh.time();
			
			gvh.sleep(500);
		}
		
		// send a start message and begin
		gvh.comms.addOutgoingMessage(new RobotMessage("ALL", robotName, MSG_START, ""));

		while (true) {
		
			switch ( leaderStage) { 
			
			case START:
				
				for (int j = 0 ; j < nBot ; j ++) {
					paths[j] = new Path() ;
					interPathsTemp[j] = new Path() ; 
				}
				
				leaderStage = LEADERSTAGE.PATH_UPDATE ;
				break; 
	
			case PATH_UPDATE:
		
				
				
				startingCycle = GetCycleNumber() ;  
				
				 int flag = 0 ; 

			
				connectorLineGenerator();
						
			
				int incPercent = PERCENT ; 
				
				for(int k = 0 ; k < 11 ; k++){
					
					//TODO 
					
					interPathGenerator(incPercent) ;

					LinkedList <WayPoint> interPathLinkedList = new LinkedList<WayPoint>() ;
					
					for (WayPoint wpt:interPath)
						interPathLinkedList.add(wpt) ; 
					
					pathGeneration(interPathsTemp, desiredDistance, interPathLinkedList)  ;
					

					if (interferenceCheck() == true ) 
					
					{
						
						incPercent = incPercent * 9/10 ;
				//		System.out.println("true! " + k + " " + incPercent);
						
						interPath.clear();
						interPath.addAll(currentPath);
						
					//	if ( k > 8) {
						//	flag =  1 ; 
						//	break ; 
					//	}
						

					}
					
					else {
						
						connectionLineGenerator() ; 
			//			System.out.println("false! " + k);
						break ;
						
					}
						 
					
				}
					
				
						currentPathUpdate() ;
				
				leaderStage = LEADERSTAGE.PATH_GENERATION ;
				
//				if (  flag == 1)
//					leaderStage = LEADERSTAGE.DONE ; 
				
				
				break ;
				
				
				
			case DONE: 
				
						currentPathUpdate() ;
						
					pathGeneration(paths, desiredDistance, currentPath)  ;
				
				for(int k = 0 ; k < nBot ; k++) {
			
					String msgContent = Integer.toString(pathID) ;
					
					for (WayPoint interWpt : paths[k].path){
						msgContent = msgContent +"," + interWpt.x  + ":" + interWpt.y  + ":" + interWpt.time ; 						
					}
										 
					RobotMessage path = new RobotMessage( participantsList.get(k) ,  gvh.id.getName(), MSG_PATH_UPDATE, msgContent);
					sendMsg(path) ;

					synchronized(ackReport){			
						ackReport[k].lastPathId = pathID ; 
						ackReport[k].received = false ; 	
					}
				}
				
				
				while(true){
					gvh.sleep(1000) ;
			//		System.out.println("Done");
				}
					
				
				
			case PATH_GENERATION:
 
				pathGeneration(paths, desiredDistance, currentPath)  ;
				
				leaderStage = LEADERSTAGE.SEND_PATH ; 
				break ; 
				
			case SEND_PATH:

				//put all the wpts together in a string starting with the message ID

				
				for(int k = 0 ; k < nBot ; k++) {
			

					//pathID ++ ; 
					
					String msgContent = Integer.toString(pathID) ;
					
					for (WayPoint interWpt : paths[k].path){
						msgContent = msgContent +"," + interWpt.x  + ":" + interWpt.y  + ":" + interWpt.time ; 						
					}
										 
					RobotMessage path = new RobotMessage( participantsList.get(k) ,  gvh.id.getName(), MSG_PATH_UPDATE, msgContent);
					sendMsg(path) ;
		//			System.out.println(msgContent);

					synchronized(ackReport){			
						ackReport[k].lastPathId = pathID ; 
						ackReport[k].received = false ; 	
					}
				}

				leaderStage = LEADERSTAGE.ACK_CHECK ; 
				break ; 
				
				
				
				
			case ACK_CHECK:
				//TODO check for all the acks and recalc or stop sneding!
				
				gvh.sleep(45); // was 15
		//		System.out.println("leader state is ACK_CHECK" + " " + allAcksRecieved(pathID) + " " + startingCycle + " " + GetCycleNumber());
				
				if ( allAcksRecieved(pathID) == true && GetCycleNumber() == startingCycle){
						
					leaderStage = LEADERSTAGE.PATH_UPDATE ;
		//			System.out.println("go to path update " );
				}
				else if(allAcksRecieved(pathID) == false && GetCycleNumber() == startingCycle) {

					leaderStage = LEADERSTAGE.RESEND_PATH;
		//			System.out.println("RESEND" );
					
				}
									
				else if ( GetCycleNumber() != startingCycle){

		//			System.out.println("follow path without Ack");
					startingCycle = GetCycleNumber() ; 
					leaderStage = LEADERSTAGE.FOLLOW_PATH_WITHOUT_ACK;
					
				}


				break;
	
			case RESEND_PATH:

				// generate and send the path to the followers who have not sent any ack back.
				
				
				for(int k = 0 ; k < nBot ; k++) {
					if (ackReport[k].received == false){
						
						//pathID ++ ; 
						
						String msgContent = Integer.toString(pathID) ;
						
						for (WayPoint interWpt : paths[k].path){
							msgContent = msgContent +"," + interWpt.x  + ":" + interWpt.y  + ":" + interWpt.time ; 						
						}
											 
						RobotMessage path = new RobotMessage( participantsList.get(k) ,  gvh.id.getName(), MSG_PATH_UPDATE, msgContent);
						sendMsg(path) ;

						synchronized(ackReport){			
							ackReport[k].lastPathId = pathID ; 
							ackReport[k].received = false ; 	
						}						
					}
				}

				leaderStage = LEADERSTAGE.ACK_CHECK ; 
				break; 
				
			case FOLLOW_PATH:
				//System.out.println(leaderStage) ;
				newPath.remove(0);
				
				WayPoint next = currentPath.removeFirst();
				
				if (next != null)
				{
					goToWayPoint(next);
					
					ItemPosition nextPos = new ItemPosition("leader waypoint", next.x, next.y, 0);
					
					motion.goTo(nextPos);
				}

				leaderStage = LEADERSTAGE.PATH_UPDATE ;
				break ;

			case FOLLOW_PATH_WITHOUT_ACK:
				//System.out.println(leaderStage) ;
				newPath.remove(0);
				
				next = currentPath.removeFirst();
				
				if (next != null)
				{
					goToWayPoint(next);
					
					ItemPosition nextPos = new ItemPosition("leader waypoint", next.x, next.y, 0);
					
					motion.goTo(nextPos);
				}

				leaderStage = LEADERSTAGE.ACK_CHECK ;
				break ;
			}
		}
		
	}
	
	public void runFollowerLogic()
	{
		// go to the first waypoint
		WayPoint first = currentPath.removeFirst();
		goToWayPoint(first);
		readyToStart = true;
		
		// wait until the start message is received
		while (!recievedStart)
			gvh.sleep(60); // was 20
		
		while (currentPath.size() > 0)
		{
			WayPoint next = currentPath.removeFirst();
					
			startingCycle = GetCycleNumber() ;
			
			while(startingCycle == GetCycleNumber())
				gvh.sleep(30); // was 10
			
			while (next.time < startingCycle*1000){
				next = currentPath.removeFirst();
				System.out.println("Next " + next.time + " " + startingCycle) ; 
			}
			
			System.out.println(next.time + " " + startingCycle );
			
			ItemPosition nextPos = new ItemPosition("followed waypoint", next.x, next.y, 0);
			
			goToWayPoint(next);
			
			motion.goTo(nextPos);
		}
	}

	private void connectionLineGenerator() {
		
		for (int i = 0 ; i < oldNewWptConnectors.size() ; i++){
//	//		System.out.println("salam: " + oldNewWptConnectors.size() + " " + interPath.size()) ;
			oldNewWptConnectors.get(i).x1 = interPath.get(i).x ; 
			oldNewWptConnectors.get(i).y1 = interPath.get(i).y ;

		}
		
	}

	private boolean interferenceCheck() {
			
		// returns true if there is any interference between the paths
		WayPoint currentWpt1 = null ; 
		WayPoint interWpt1 = null ; 
	//	System.out.println("interferenece check");
		
		

			
		
//		System.out.println("salam");
		
		for (int j = 0 ; j < numRobots ; j++)
			for (int k = j ; k < numRobots ; k++)
				if(j != k)
					for(int i = 0 ; i < paths[j].path.size() ; i++){

						 
						WayPoint currentWpt2 = paths[j].path.get(i) ; 

						int starting = Math.max(0, i-CHECK_WINDOW);
						int end  = Math.min(i+CHECK_WINDOW, interPathsTemp[j].path.size()) ;
						
							//System.out.println("starting: " + starting + " end: " + end) ;
							for(int p = starting ; p < end ; p ++ )
							{
								
								//System.out.println("i: " + i + " p: " + p) ;
								
								WayPoint intertWpt2 = interPathsTemp[k].path.get(p)  ;				
								if (currentWpt1 != null && interWpt1 != null){			
									MinDist minDist = new MinDist(currentWpt1.x, currentWpt1.y, currentWpt2.x, currentWpt2.y, interWpt1.x, interWpt1.y, intertWpt2.x, intertWpt2.y) ;
									double dist = minDist.returnMinDit() ;

							
									
									if (dist < SAFE_DISTANCE) {
					//					System.out.println("Interference!! dist: " + dist + " " + " j: " + j + " k: " + k + " "  + " i: " + i + " p: " + p + " "  +currentWpt1.x+ " " +  currentWpt1.y+ " " +  currentWpt2.x+ " " +  currentWpt2.y+ " " +  interWpt1.x+ " " +  interWpt1.y+ " " +  intertWpt2.x+ " " +  intertWpt2.y + " " + "starting: " + starting + " end: " + end) ;
		
										return true ; 
									}
										
	
								}
							
							if (p == end-1)
								interWpt1 = null ;
							else
								interWpt1 = intertWpt2 ;
						}	
							if (i == paths[j].path.size() - 1)
								currentWpt1 = null ;
							else
								currentWpt1 = currentWpt2 ;
					}
		
		return false ; 
		
	}



	private boolean allAcksRecieved(int pathID2) {
		
		int counter = 0 ; 
		for(int j = 1 ; j < numRobots ; j++){

			if(ackReport[j].received == true && ackReport[j].lastPathId == pathID2)
				counter++ ; 
		}

		
		
		if (counter == numRobots - 1) 
			return true;

		return false;
	}


	private void goToWayPoint(WayPoint next) 
	{
		int angle = 0; 
		ItemPosition ip = new ItemPosition("waypoint", next.x, next.y, angle);
		
		movingFrom = movingTo;
		movingTo = next;
		
		motion.goTo(ip);
		
		while (motion.inMotion)
			gvh.sleep(50);
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
		
//		System.out.println("right " + lastIdealRight + " " + position);
		
		
		return position;
		
	}
	
	
	public WayPoint NextLeft(WayPoint desiredDistance, long time, WayPoint lastIdealLeft, int angle){
		
		WayPoint position = new WayPoint(0,0,0); 	
		position.x = (int) ( (double) lastIdealLeft.x - (double)(desiredDistance.x) * (double)Math.sin(Math.toRadians((double)angle)) -desiredDistance.y*Math.cos(Math.toRadians((double) angle)) );
		position.y = (int) ( (double) lastIdealLeft.y + (double)(desiredDistance.x) * Math.cos(Math.toRadians((double) angle))  - desiredDistance.y*Math.sin(Math.toRadians((double) angle))) ;
		position.time = (int)time ;
		
	//	System.out.println("left " + lastIdealLeft + " " + position);
		//System.out.println(( (double) lastIdealLeft.x - (double)(desiredDistance.x) * (double)Math.cos(Math.toRadians((double)angle)) +desiredDistance.y*Math.sin(Math.toRadians((double) angle)) ));
		return position ; 
	}

	public void connectorLineGenerator() 
	{		
		//generate the connector lines
		oldNewWptConnectors.clear() ; 
		
		int middle ; 
		
		// TODO Stan check if this is redundant code with just a different middle
		
		if (currentPath.size()%2 == 0 ) {
			middle  = currentPath.size()/2 - 1 ; 
//			System.out.println("middle: " + middle);
			
			for (int j = 0 ; j <= middle ;  j ++) {  
				oldNewWptConnectors.add( new Line2D.Double(currentPath.get(j).x, currentPath.get(j).y, newPath.get(j).x, newPath.get(j).y)) ;

//				System.out.println(j); 
			}

//					System.out.println("salam1");
			//all the extra waypoints on the new path are connected to the last waypoint on the old path
			if (currentPath.size() < newPath.size()){ 
				for (int j = middle +1 ; j < newPath.size() - middle - 1  ;  j ++)
				{
					oldNewWptConnectors.add(new Line2D.Double(currentPath.get(middle).x, currentPath.get(middle).y , newPath.get(j).x, newPath.get(j).y)) ;
//					System.out.println(j);
				}
				
			}
//					System.out.println("salam2");
			for (int j =  middle +1 ; j < currentPath.size() ;  j ++) {
				
				int k = newPath.size() - 2 * middle -2+ j ; 
//				System.out.println(j + " " + k );
				oldNewWptConnectors.add( new Line2D.Double(currentPath.get( j).x, currentPath.get(j).y, newPath.get(newPath.size() - 2 * middle -2+ j).x, newPath.get(newPath.size() - 2 * middle -2 +  j).y)) ;
				
			}

			
		}
		else {
			
			middle  = currentPath.size()/2  ; 
			
			
//			System.out.println("middle: " + middle);
			for (int j = 0 ; j < middle ;  j ++) {  
				oldNewWptConnectors.add( new Line2D.Double(currentPath.get(j).x, currentPath.get(j).y, newPath.get(j).x, newPath.get(j).y)) ;

//				System.out.println(j); 
			}
			
			
			
			
//			System.out.println("salam1");
			//all the extra waypoints on the new path are connected to the last waypoint on the old path
			if (currentPath.size() <= newPath.size()){
				for (int j = middle ; j < newPath.size() - (middle + 1)  ;  j ++)
				{
					oldNewWptConnectors.add(new Line2D.Double(currentPath.get(middle).x, currentPath.get(middle).y , newPath.get(j).x, newPath.get(j).y)) ;
//					System.out.println(j);
				}
				
			}
			
//			System.out.println("salam2");
			for (int j = middle ; j < currentPath.size() ;  j ++) {
				int k  = newPath.size() - 2*middle - 1  + j ; 
	//			System.out.println(j + " " + k);
				oldNewWptConnectors.add( new Line2D.Double(currentPath.get(j).x, currentPath.get(j).y, newPath.get(newPath.size() - 2*middle - 1  + j).x, newPath.get(newPath.size() -2* middle - 1  + j).y)) ;
									
			
			}
			
		}
	}
	
	
	public void interPathGenerator(int increasePercent) {
		
//System.out.println("interPathGen " + currentPath.size() + " " + newPath.size()) ; 
		
		interPath.clear();
		
		for (int j = 0 ; j < newPath.size() ; j ++ ) {
			
			interPath.add(new WayPoint( (int) (oldNewWptConnectors.get(j).x1 + increasePercent *(oldNewWptConnectors.get(j).x2-oldNewWptConnectors.get(j).x1)/100) , (int) (oldNewWptConnectors.get(j).y1 + increasePercent * (oldNewWptConnectors.get(j).y2 - oldNewWptConnectors.get(j).y1)/100), newPath.get(j).time ));
		}
	}
	
	
	
	
	public void currentPathUpdate() 
	{
		currentPath.clear();
		currentPath.addAll(interPath);
	}
	
	
	public void sendMsg(RobotMessage msg) { 
		
		for (int j = 0 ; j < MSG_SENDING_NUMBER ; j ++) {
		
			gvh.comms.addOutgoingMessage(msg);
		}
		
		
	}
	
	public void pathGeneration(Path[] pathsArray, WayPoint desiredDistance, List <WayPoint> leaderPath ) 
	{
		// After the pathsArray[0] which is the path for leader is filled, this funtion will 
		// generate the pathsArray[1], pathsArray[2] , and ... based
		// on the leader's path
		
		
		
		int angleInit = (int) Math.toDegrees( Math.atan2( leaderPath.get(1).y - leaderPath.get(0).y , leaderPath.get(1).x - leaderPath.get(0).x ) );
		int angle ;
		
		double xOld =0, yOld=0, xNew, yNew ;
		
		xOld = leaderPath.get(0).x ; 
		yOld = leaderPath.get(0).y; 
		
//		System.out.println("init angle: " + angle +" " +  leaderPath.get(1).y +" " +  leaderPath.get(0).y  +" " + leaderPath.get(1).x  +" " + leaderPath.get(0).x) ; 
		
		for(int j = 0 ; j < nBot ; j ++ ) {
			pathsArray[j].path.clear() ; 
		}

		int counter = 0 ; 	
		for (WayPoint wpt : leaderPath)
		{
			pathsArray[0].path.add(wpt) ;
			xNew = (double) wpt.x ;
			yNew = (double) wpt.y ; 

			if(counter == 0)
				angle = angleInit ; 
			else 
				angle =(int) Math.toDegrees( Math.atan2( yNew - yOld , xNew - xOld ) );
				
		//	System.out.println("angle: " + angle) ; 


			
		
			
			WayPoint lastRight = new WayPoint(wpt.x, wpt.y, wpt.time);
			WayPoint lastLeft = new WayPoint(wpt.x, wpt.y, wpt.time); 
			

	//		System.out.println(wpt) ;
			for (int j = 0 ; (2*j+1)<nBot ; j++){
	//			System.out.println(2*j+1) ;   
				lastRight = NextRight(desiredDistance, (long) wpt.time, lastRight, angle );
				pathsArray[2*j+1].path.add(lastRight);
	//			System.out.println((2*j+1) + " " + lastRight + " " + angle ) ; 
				
			}
			
				
			for (int j = 1 ; (2*j)<nBot ; j++){
	//			System.out.println(2*j) ; 
				lastLeft  = NextLeft(desiredDistance, (long) wpt.time, lastLeft, angle );
				pathsArray[2*j].path.add(lastLeft);
	//			System.out.println((2*j) + " " + lastLeft + " " + angle) ; 
			}


			
			

			xOld = (double)wpt.x ; 
			yOld = (double)wpt.y ;		
			counter ++ ; 
			
		}
		
		
		
	}
	
	
	
	@Override
	public void draw(Graphics2D g)
	{
		if (robotId == 0)
			g.setColor(Color.BLACK);
		else
			g.setColor(Color.LIGHT_GRAY);
		
		if (movingFrom != null && movingTo != null)
		{
			// draw a dotted line showing the robot's current motion path
			 g.setStroke(new BasicStroke(20.0f, BasicStroke.CAP_BUTT,
					 BasicStroke.JOIN_MITER, 10.0f, new float[]{35.0f}, 0.0f));
			 
			g.drawLine(movingFrom.x, movingFrom.y, movingTo.x, movingTo.y);
		}
		
		g.setStroke(new BasicStroke(4));
		
		WayPoint last = movingTo;
		
		for (WayPoint p : currentPath)
		{
			if (last != null)
				g.drawLine(last.x, last.y, p.x, p.y);
			
			drawOval(g, p);
			
			last = p;
		}
 	
		if (robotId == 0)
		{
			g.setColor(Color.orange);
			
			last = null;
			
			for (WayPoint p : newPath)
			{
				if (last != null)
					g.drawLine(last.x, last.y, p.x, p.y);
				
				drawOval(g, p);
				
				last = p;
			}
		}
			
		g.setColor(Color.red);
		
		last = null;
		
		for (WayPoint p : interPath)
		{
			if (last != null)
				g.drawLine(last.x, last.y, p.x, p.y);
			
			drawOval(g, p);
			
			last = p;
		}
	}

	private void drawOval(Graphics2D g, WayPoint p) 
	{
		final int POINT_DRAW_SIZE = 10;
		
		g.fillOval((int)(p.x - POINT_DRAW_SIZE), (int)(p.y - POINT_DRAW_SIZE), 
				2 * POINT_DRAW_SIZE + 1, 2 * POINT_DRAW_SIZE + 1);
	}
		
	public void ackProcess(ArrayList <WayPoint> receivedPath, int pathId)
	{
		// TODO determine the situations in which the ACK should be sent
		
		gvh.comms.addOutgoingMessage(new RobotMessage(leaderName, gvh.id.getName(), MSG_PATH_ACK, robotId + "," + Integer.toString(pathId))) ;
		
		// we MIGHT have to protect currentPath with a synchronized block here!
		currentPath.clear();
		currentPath.addAll(receivedPath);
	}
	
	@Override
	public void messageReceied(RobotMessage m) {

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
		else if (m.getMID() == MSG_PATH_UPDATE && robotId != 0 )
		{
			
			String[] waypoints = m.getContents(0).split(",") ;
			String[] wptData ; 
			ArrayList <WayPoint> receivedPath = new ArrayList <WayPoint>() ;
							
			pathID =  Integer.valueOf(waypoints[0]) ; 
			
			for(int j = 1 ; j <waypoints.length; j ++) { 
				
				String wpt = waypoints[j];
				wptData = wpt.split(":") ; 	
				receivedPath.add(new WayPoint( Integer.valueOf(wptData[0] ),Integer.valueOf(wptData[1]) , Integer.valueOf(wptData[2]) )) ; 
			}

			ackProcess(receivedPath,pathID) ; 

		}
		
		else if( m.getMID() == MSG_PATH_ACK ){
			
			String[] ackContent = m.getContents(0).split(",");
			int ackSenderId = Integer.valueOf(ackContent[0]) ; 
			int ackId = Integer.valueOf(ackContent[1]);

			synchronized(ackReport){
				
				ackReport[ackSenderId].lastPathId = ackId ; 
				ackReport[ackSenderId].received = true; 
			}
			
			//System.out.println( m.get);
		}
		
		
	}
	@Override
	public void receivedPointInput(Point p)
	{
	//	System.out.println("Point Received!");
	
		if (robotId == 0)
		{
			double ANCHOR_DISTANCE = 1000;
			double SEPARATION = 100;
			
			ArrayList <WayPoint> oldPath = new ArrayList <WayPoint>();
			oldPath.addAll(currentPath);
			Point2D.Double detourPoint = new Point2D.Double(p.x, p.y);
			
				
			ArcCreator curveGenerator = new ArcCreator();
			ArrayList <WayPoint> newPath = curveGenerator.createNewPath(oldPath, detourPoint, ANCHOR_DISTANCE, SEPARATION) ;
			
			
//				This part works instead of zhenqi's
/*				
				ArrayList <WayPoint> newPath = new ArrayList <WayPoint>() ; 
				ArrayList<Line2D.Double> oldNewWptConnectors = new ArrayList<Line2D.Double>();
				System.out.println("New point recieved.");


				
				for(int j = 0 ; j < currentPath.size() ; j++){
					newPath.add(new WayPoint(currentPath.get(j).x , currentPath.get(j).y + j* 30 , currentPath.get(j).time )) ;
				}
*/				
			if (newPath != null)
			{
				this.newPath = newPath;
			}
		}
	//	System.out.println("Point Received!");
	}

}





