//Written by Lucas Buccafusca
//v1.0 -06-1-2012
//v1.1 -6-5-2012
//What the App does: 
//The robots form a V shape and travel to a series of waypoints, adjusting their path and velocities as they go
//Use the following waypoints (for best results) for the destinations:
/*
		WAY,2500,3000,0,DEST0
       WAY,2500,3500,0,DEST1
       WAY,2600,4000,0,DEST2
       WAY,2700,4500,0,DEST3
       WAY,2900,5000,0,DEST4
       WAY,3300,5500,0,DEST5
       WAY,3800,6000,0,DEST6
       WAY,4400,6500,0,DEST7
       WAY,5300,7000,0,DEST8
*/



package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.PrimitiveDenseStore;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.quadratic.QuadraticSolver;

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



public class NewApp extends LogicThread implements MessageListener {

	SortedSet<String> toVisit = new TreeSet<String>();
	SortedSet<String> toVisit2 = new TreeSet<String>();
	SortedSet<String> arrived = new TreeSet<String>();

	private static final String TAG = "Logic";
	
	private String destname = null;
	private String leader = null;
	
	private boolean running = true;

	private boolean iamleader = false;
	private LeaderElection le;
	private Synchronizer sync;
	private MotionParameters M;
	private MotionParameters DEFAULT;

	private int d_d=500; //Some distance that each robot will be from the nearest robot 
	private double theta_r=Math.PI/4; //0<theta_r<2*PI, theta_r != PI/2, 3*PI/2... Theta_r>PI=Arrow points down Be wary of angles <PI/2.7
	private int d_r=550;//Thickness of Tube
	ItemPosition leaderstart=new ItemPosition("goHere",SimSettings.GRID_XSIZE/2, SimSettings.GRID_YSIZE/2, 0);
	
	String robotName = gvh.id.getName();
    Integer robotNum = Integer.parseInt(robotName.substring(3));
	private int numbots=gvh.id.getParticipants().size();

	
	
	
	PositionList posList = new PositionList();
	private final static String SYNC_START = "1";
	
	
	private enum STAGE { START, SYNC, LE, MOVE,WAYPOINT_CALC, WAYPOINT_TRAVEL, WAIT_TO_ARRIVE, DONE };
	private STAGE stage= STAGE.START;
	
	public NewApp(GlobalVarHolder gvh) {
		super(gvh);
		
		
		// Get the list of position to travel to
		for(ItemPosition ip : gvh.gps.getWaypointPositions().getList()) {
			toVisit.add(ip.name);
		}
		
		// Progress messages are broadcast with message ID 99
		gvh.comms.addMsgListener(99, this);

		// Make sure waypoints were provided
		if(gvh.gps.getWaypointPositions().getNumPositions() == 0) System.out.println("This application requires waypoints to travel to!");
	
		sync = new BarrierSynchronizer(gvh);
		le = new PickedLeaderElection(gvh);
		for(int i=0; i<numbots;i++)
			arrived.add("bot"+i);
		
		M=new MotionParameters();
		DEFAULT=new MotionParameters();
		
		
		
	
	}

	
	@Override
	public List<Object> callStarL() {
		
			//Declares leader
	    	while (running){
				gvh.sleep(100);
				gvh.plat.setDebugInfo(gvh.id.getParticipants().toString());
				switch (stage) {
				case START:
					sync.barrier_sync(SYNC_START);
					stage = STAGE.SYNC;
					gvh.log.d(TAG, "Syncing...");
					System.out.println("Syncing..." + name);
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
						stage = STAGE.MOVE;
					
					}
					break;
					
				case MOVE:
					
					gvh.plat.moat.goTo(startpoint());
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
						stage = STAGE.MOVE;
						
					}
					else if (!toVisit2.isEmpty()) {
					toVisit2.remove("START POINT" + robotNum);
					}
					
					if(toVisit2.isEmpty()) { 
						stage = STAGE.WAIT_TO_ARRIVE;
					} else {
						stage = STAGE.MOVE;
					}
					break;
					
				case WAIT_TO_ARRIVE:
					
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
						arrived.remove("bot"+robotNum);
						}
					
					
						stage = STAGE.WAYPOINT_CALC;
						
					
					break;
					
				case WAYPOINT_CALC:
					//Calculation of waypoints for other robots
					if (arrived.isEmpty()) 
					{
					stage=STAGE.WAYPOINT_TRAVEL;
					break;}
					else{
						stage=STAGE.WAYPOINT_CALC;
						}
					break;

					
				case WAYPOINT_TRAVEL:
					//Checks to see if all robots have arrived to their new waypoints before continuing on
					for (int i=0; i<numbots; i++)
					{
						int pos_x= gvh.gps.getPosition("bot" + i).x;
						int pos_y=gvh.gps.getPosition("bot" + i).y;
						gvh.sleep(15);
						while (pos_x != gvh.gps.getPosition("bot" +i).x ||
								pos_y != gvh.gps.getPosition("bot" +i).y)
						{
							gvh.sleep(15);
							pos_x= gvh.gps.getPosition("bot" + i).x;
							pos_y=gvh.gps.getPosition("bot" + i).y;
							gvh.sleep(50);
						}
					}
					
					
					
					if (arrived.isEmpty()) 
					{
						ItemPosition x = newpoint();
						gvh.sleep(10);
						

						for(int i=0; i<numbots;i++)
						{ 
							//distance(i, x); 
						
						
						}
						
						gvh.plat.moat.turnTo(x);
						
						//Figure out ratio of distances so that an appropriate velocity can be used
						double distance_to_next_loc_y= Math.abs((gvh.gps.getMyPosition().y-(x.y)));
						double distance_to_next_loc_x= Math.abs((gvh.gps.getMyPosition().x-(x.x)));
						distance_to_next_loc_y *=distance_to_next_loc_y;
						distance_to_next_loc_x *=distance_to_next_loc_x;
						double distance_to_next_point = Math.sqrt(distance_to_next_loc_y + distance_to_next_loc_x);	
						
						
						double distance_for_leader_y=Math.abs((gvh.gps.getPosition(leader).y-(gvh.gps.getWaypointPosition(toVisit.first()).y)));
						double distance_for_leader_x=Math.abs((gvh.gps.getPosition(leader).x-(gvh.gps.getWaypointPosition(toVisit.first()).x)));
						distance_for_leader_y*= distance_for_leader_y;
						distance_for_leader_x*=distance_for_leader_x;
						double distance_for_leader = Math.sqrt(distance_for_leader_y + distance_for_leader_x);	

						
						double ratio = distance_to_next_point / distance_for_leader;

						
						M.LINSPEED_MAX=(int) (ratio * DEFAULT.LINSPEED_MAX);
						M.LINSPEED_MIN=(int) (ratio * DEFAULT.LINSPEED_MAX);
						
						
						gvh.sleep(2100);
						gvh.plat.moat.goTo(x, M);		
						while (gvh.plat.moat.inMotion) 
						{					
							
							gvh.sleep(1);
							
						}
					toVisit.remove(toVisit.first());
					arrived.add("bot"+ robotNum);
					
					}
					else 
					{
					arrived.remove("bot"+ robotNum);
					}
					if(toVisit.isEmpty()) { 
						stage = STAGE.DONE;
						break;
					} else {
						stage = STAGE.WAYPOINT_TRAVEL;

						break;
					}
					
					
								
				case DONE:
					System.out.println(name + " is done.");
					gvh.plat.moat.motion_stop();
					return Arrays.asList(results);
					
					
				}
				
						
		
	    	}
		return null;
	}


	@Override
public void messageReceied(RobotMessage m) {
		synchronized(arrived) {
			// Remove the received waypoint from the list of waypoints to visit
			arrived.remove(m.getContents(0));
						
		}
		
		
	}
	
private ItemPosition startpoint() {
		
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
private ItemPosition newpoint() {
	
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