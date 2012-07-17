package edu.illinois.mitra.starlSim.simapps;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.PickedLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;
import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starl.motion.MotionParameters;
//import edu.illinois.mitra.starlSim.main.messageQueue;

import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.PrimitiveDenseStore;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.quadratic.QuadraticSolver;

public class LucasApp1 extends LogicThread implements MessageListener{// implements MessageListener {
	SortedSet<String> toVisit = new TreeSet<String>();
	SortedSet<String> toVisit2 = new TreeSet<String>();

	
	private static final String TAG = "Logic";
	private final static String SYNC_START = "1";
	private int LeaderDestNum;
	private ItemPosition NextLeaderPositin;
	
	private String destname = null;
	private String leader = null;
	
	private boolean iamleader = false;
	private LeaderElection le;
	private Synchronizer sync;
	
	private int d_d=500; //Some distance that each robot will be from the nearest robot 
	private double theta_r=Math.PI/4; //0<theta_r<2*PI, theta_r != PI/2, 3*PI/2... Theta_r>PI=Arrow points down Be wary of angles <PI/2.7
	private int d_r=550;//Thickness of Tube
	ItemPosition leaderstart=new ItemPosition("goHere",SimSettings.GRID_XSIZE/2, SimSettings.GRID_YSIZE/2, 0);
	
	String robotName = gvh.id.getName();
    Integer robotNum = Integer.parseInt(robotName.substring(3));
	private int numbots=gvh.id.getParticipants().size();
	
	PositionList posList = new PositionList();
	
	private enum STAGE {INIT, SYNC, LE,MOVE_TO_INITIAL_POS,WAIT_TO_ARRIVE_AT_INIT_POS, START_COMM, NAMING, LEADER_START, FOLLOWER_START, DONE, GET_NEXT_POSITION, FOLLOWER_WPT_CALC, GET_POSITION_INIT, INIT_POS_ACK, TEST_POSITION_INIT }
	private STAGE stage = STAGE.INIT;
	

	//messageQueue mQueue = new messageQueue();
	public BlockingQueue<RobotMessage> mQueue = new LinkedBlockingQueue<>();
	private boolean add; 

	
	
	public LucasApp1(GlobalVarHolder gvh) {
		super(gvh);
		// Get the list of position to travel to
		for(ItemPosition ip : gvh.gps.getWaypointPositions().getList()) {
			toVisit.add(ip.name);
		}
		
		// Progress messages are broadcast with message ID 99 & 98
		gvh.comms.addMsgListener(99, this);
		gvh.comms.addMsgListener(98, this);

		
		// Make sure waypoints were provided
		if(gvh.gps.getWaypointPositions().getNumPositions() == 0) System.out.println("This application requires waypoints to travel to!");
	
		sync = new BarrierSynchronizer(gvh);
		le = new PickedLeaderElection(gvh);
		
	}
	
	@Override
	public List<Object> callStarL() {
		 
		
		while(true) {
			gvh.sleep(100);
			gvh.plat.setDebugInfo(gvh.id.getParticipants().toString());
			switch(stage) {
			
			case INIT:
				//initialization stage
				LeaderDestNum = 0;
				sync.barrier_sync(SYNC_START);
				stage = STAGE.SYNC;
				gvh.log.d(TAG, "Syncing...");
				System.out.println("Syncing..." + name);
				stage = STAGE.SYNC ; 
			break;
			
			case SYNC:
				if (sync.barrier_proceed(SYNC_START)) {
					stage = STAGE.LE;
					le.elect();
					gvh.log.d(TAG, "Synced!");
				}
				break;
			case LE:
				if(le.getLeader() != null) {
					gvh.log.d(TAG, "Electing...");
					leader = le.getLeader();
					iamleader = leader.equals(name);
					System.out.println("Robot Leader? "+leader);
					stage = STAGE.MOVE_TO_INITIAL_POS;
				
				}
				break;
			
			case MOVE_TO_INITIAL_POS:
				
				gvh.plat.moat.goTo(startpoint_line());
				boolean motionSuccess = true;
				while(gvh.plat.moat.inMotion) { 
					gvh.sleep(10);
					if(!toVisit2.contains("START POINT" + robotNum)) {
						motionSuccess = false;
						break;
					}				
				}
				
				// If Arrival of the LAST robot
				if(motionSuccess && toVisit2.isEmpty()) {
					stage = STAGE.MOVE_TO_INITIAL_POS;
					
				}
				else if (!toVisit2.isEmpty()) {
				toVisit2.remove("START POINT" + robotNum);
				}
				
				if(toVisit2.isEmpty()) { 
					stage = STAGE.WAIT_TO_ARRIVE_AT_INIT_POS;
				} else {
					stage = STAGE.MOVE_TO_INITIAL_POS;
				}
				break;
				
			case WAIT_TO_ARRIVE_AT_INIT_POS:
				
				motionSuccess = true;
				while(gvh.plat.moat.inMotion) { 
					gvh.sleep(10);
					if(!toVisit2.contains(destname)) {
						motionSuccess = false;
						break;
					}				
				}
				
				// If Arrives
				if(motionSuccess) {
					RobotMessage inform = new RobotMessage("ALL", name, 99, "bot"+robotNum);
					gvh.comms.addOutgoingMessage(inform);
					}
				
				
					stage = STAGE.START_COMM;
					
				
				break;
				
				
			case START_COMM:
				System.out.print(name + "  " + iamleader + "\n");
				//start Communication Thread
				
				//Communication CommThread = new Communication(gvh, mQueue);
				//CommThread.start();
				
				stage=STAGE.NAMING;
				break;
				
			case NAMING :
				
				RobotMessage PresenceMessage = new RobotMessage("ALL", gvh.id.getName(), 98, "I'm in!");
				gvh.comms.addOutgoingMessage(PresenceMessage);

				if (iamleader)
					stage=STAGE.LEADER_START;
				else
					stage=STAGE.FOLLOWER_START;
				break;			

//The Code for LEADER which is always bot0
				
				//Be wary, since the roombas have different names
			case LEADER_START:
				
//				stage = STAGE.GET_POSITION_INIT ;
				stage = STAGE.TEST_POSITION_INIT;
				break ;
				
				
		
/*				
			case GET_POSITION_INIT :

				RobotMessage AskInitPos = new RobotMessage("All", gvh.id.getName(), 99, "Where are you?");			
				stage = STAGE.GET_NEXT_POSITION;
				
				break;
				
				
			case GET_NEXT_POSITION :
				//get the next position of leader from the leader's waypoints list
				for (ItemPosition ip : gvh.gps.getWaypointPositions().getList())
					if (ip.name.equals("DEST"+LeaderDestNum))
					{
						NextLeaderPositin = ip ;
						break;
					}
				LeaderDestNum++;
				
				System.out.println(NextLeaderPositin.name);
//				stage = STAGE.FOLLOWER_WPT_CALC ;
				
				

				System.out.println("MessageQueue size :"+ mQueue.size() );
				
				//System.out.println(mQueue.poll().getContentsList());
				
//				System.out.print("mqueu refer from main: " + mQueue);
			
				stage = STAGE.GET_NEXT_POSITION ;
				break; */
				
			
				
			
			
			
			case FOLLOWER_WPT_CALC :
				
				
				
				break;
				
		
				
//The code for Followers which are other robots other than BOT0				
			case FOLLOWER_START:
				
				
				
				
				stage = STAGE.INIT_POS_ACK ;
				break;
				
			case INIT_POS_ACK :
				
				
				break;
				
			
			
								

//when all the code is executed and finished!				
			case DONE:
				le.cancel();
				sync.cancel();
				gvh.comms.removeMsgListener(99);
				gvh.comms.removeMsgListener(98);
				return returnResults();
			}
			gvh.sleep(100);
		}
	}

	@Override
	public void messageReceied(RobotMessage m) {
		// TODO Auto-generated method stub
		
	}

	
private ItemPosition startpoint_V() {
		
		String robotName = gvh.id.getName();
        Integer robotNum = Integer.parseInt(robotName.substring(3)); // assumes: botYYY
    	toVisit2.add("START POINT" + robotNum);
        if(iamleader){
		return new ItemPosition("goHere",SimSettings.GRID_XSIZE/2, SimSettings.GRID_YSIZE/2, 0);
		}
        else {
        	if (robotNum % 2 == 0 && !iamleader)
    		{return new ItemPosition("goHere",(int) (SimSettings.GRID_XSIZE/2 +d_d*robotNum/2*Math.cos(theta_r)), (int) (SimSettings.GRID_YSIZE/2 -d_d*robotNum/2*Math.sin(theta_r)), 0);
    		}
        	
        	if (robotNum % 2 == 1)
    		{return new ItemPosition("goHere",(int) (SimSettings.GRID_XSIZE/2 -d_d*(robotNum+1)/2*Math.cos(theta_r)), (int) (SimSettings.GRID_YSIZE/2 -d_d*(robotNum+1)/2*Math.sin(theta_r)), 0);
    		}
        	else 
        	{return null;}

        }
	}
private ItemPosition startpoint_line() {
	
	String robotName = gvh.id.getName();
    Integer robotNum = Integer.parseInt(robotName.substring(3)); // assumes: botYYY
	toVisit2.add("START POINT" + robotNum);
    if(iamleader){
	return new ItemPosition("goHere",SimSettings.GRID_XSIZE/2, SimSettings.GRID_YSIZE/2, 0);
	}
    else {
    	
		{return new ItemPosition("goHere",(int) (SimSettings.GRID_XSIZE/2 +d_d*robotNum*Math.cos(theta_r)), (int) (SimSettings.GRID_YSIZE/2 -d_d*robotNum*Math.sin(theta_r)), 0);
		}
    	
    
    }
}
private ItemPosition newpoint_V() {
	
	String robotName = gvh.id.getName();
    Integer robotNum = Integer.parseInt(robotName.substring(3)); // assumes: botYYY
	destname = toVisit.first();
	
	ItemPosition New_leader_position =new ItemPosition("goHere", gvh.gps.getWaypointPosition(destname).x,  gvh.gps.getWaypointPosition(destname).y, 0);
	if(iamleader){
		gvh.plat.moat.turnTo(New_leader_position);
		gvh.sleep(100);
		gvh.plat.moat.motion_stop();
	}
	else{
		gvh.sleep(100);		
		gvh.plat.moat.motion_stop();
}
	
	
    if(iamleader){
		
		return New_leader_position;
			}
    else {
		int angle_of_leader = gvh.gps.getPosition(leader).angle;
		int new_angle_of_leader =gvh.gps.getPosition(leader).angleTo(New_leader_position);
    	double difference = new_angle_of_leader - angle_of_leader +90;
    	difference = Math.toRadians(difference);

    	if (robotNum % 2 == 0 && !iamleader)
    	{	

    		return new ItemPosition("goHere",(int) (gvh.gps.getWaypointPosition(destname).x +d_d*robotNum/2*Math.cos(theta_r+difference)), (int) (gvh.gps.getWaypointPosition(destname).y -d_d*robotNum/2*Math.sin(theta_r+difference)), 0);
				
    	}
    	
    	if (robotNum % 2 == 1)
    	{
    		return new ItemPosition("goHere",(int) (gvh.gps.getWaypointPosition(destname).x -d_d*(robotNum+1)/2*Math.cos(theta_r-difference)), (int) (gvh.gps.getWaypointPosition(destname).y -d_d*(robotNum+1)/2*Math.sin(theta_r-difference)), 0);
    			
    	}
    	else 
    	{return null;}

    }
}
private ItemPosition newpoint_line() {
	
	String robotName = gvh.id.getName();
    Integer robotNum = Integer.parseInt(robotName.substring(3)); // assumes: botYYY
	destname = toVisit.first();
	
	ItemPosition New_leader_position =new ItemPosition("goHere", gvh.gps.getWaypointPosition(destname).x,  gvh.gps.getWaypointPosition(destname).y, 0);
	if(iamleader){
		gvh.plat.moat.turnTo(New_leader_position);
		gvh.sleep(100);
		gvh.plat.moat.motion_stop();
	}
	else{
		gvh.sleep(100);		
		gvh.plat.moat.motion_stop();
}
	
	
    if(iamleader){
		
		return New_leader_position;
			}
    else {
		int angle_of_leader = gvh.gps.getPosition(leader).angle;
		int new_angle_of_leader =gvh.gps.getPosition(leader).angleTo(New_leader_position);
    	double difference = new_angle_of_leader - angle_of_leader +90;
    	difference = Math.toRadians(difference);

    	if (robotNum % 2 == 0 && !iamleader)
    	{	

    		return new ItemPosition("goHere",(int) (gvh.gps.getWaypointPosition(destname).x +d_d*robotNum*Math.cos(theta_r+difference)), (int) (gvh.gps.getWaypointPosition(destname).y -d_d*robotNum/2*Math.sin(theta_r+difference)), 0);
				
    	}
    	
    	else 
    	{return null;}

    }
}
private double distance (ItemPosition CurrentPositionA, ItemPosition NextPositionA, ItemPosition CurrentPositionB, ItemPosition NextPositionB){
	//Finds distance between current robot (robotNum) and bot i. New_point is the calculated new position of the first robot
	final PhysicalStore.Factory<Double, PrimitiveDenseStore> tmpFactory = PrimitiveDenseStore.FACTORY;
	
	final double[][] tmpA = new double[][] { { 1.0, 0.0 }, { -1.0, 0.0 }, 
			{ 0.0, 1.0 }, { 0.0, -1.0 } };
	final PrimitiveDenseStore A =  tmpFactory.rows(tmpA);
	
	final double[] tmpB = new double[] { 1.0, 0.0, 1.0, 0.0 };
	final PrimitiveDenseStore B = tmpFactory.columns(tmpB);
	
	
		if (CurrentPositionA==CurrentPositionB){return 0;}
		else{
		
		double x1= CurrentPositionA.x;
		double x2= NextPositionA.x;
		double x3= CurrentPositionB.x;
		double x4=NextPositionB.x;
		
		double y1= CurrentPositionA.y;
		double y2= NextPositionA.y;
		double y3= CurrentPositionB.y;
		double y4=NextPositionB.y;
		
		
		final double[][] tmpQ = new double[][] 
				{ { (Math.pow((x1-x2),2) +(Math.pow((y1-y2), 2))), 
				-(x1*x3)-(x1*x4)+(x2*x3)+(x2*x4)-(y1*y3)-(y1*y4)+(y2*y3)+(y2*y4) },
				{ -(x1*x3)-(x1*x4)+(x2*x3)+(x2*x4)-(y1*y3)-(y1*y4)+(y2*y3)+(y2*y4), 
				(Math.pow((x1+x2),2) +(Math.pow((y1+y2), 2))) } };
		
		final PrimitiveDenseStore Q = tmpFactory.rows(tmpQ);
		
		
		final double[] tmpC = new double[] 
				{ -1*((x1-x2)*(x2+x4)+2*(y1-y2)*(y2+y4)), 
				2*x2*x3+2*x2*x4 +2*x3*x4+2*x4*x4 + 2*y2*y3+2*y2*y4 +2*y3*y4+2*y4*y4};
		final PrimitiveDenseStore C = tmpFactory.columns(tmpC);
		
		
		QuadraticSolver.Builder tmpLP = new QuadraticSolver.Builder(Q,C);
		tmpLP = tmpLP.inequalities(A, B);
		QuadraticSolver LP = tmpLP.build();
		

		Optimisation.Result Res = LP.solve();
				

		
		final PrimitiveDenseStore Xstar = tmpFactory.makeZero(Res.size(),1);
		for(int j = 0; j < Res.size(); j++){
			Xstar.set(j, 0, Res.doubleValue(j));
		}
		
		
		double lamda1= Xstar.doubleValue(0);
		double lamda2 = Xstar.doubleValue(1);
		
		double answer= Math.sqrt(Math.pow(((lamda1*x1) +(1-lamda1)*x2)-(lamda2*x3) +(1-lamda2)*x4, 2.0) +   
				Math.pow(((lamda1*y1) +(1-lamda1)*y2)-(lamda2*y3) +(1-lamda2)*y4, 2.0)
				);
			

		
		
		return answer;

		}

	
	
	
	
}




	
	
	
	
}