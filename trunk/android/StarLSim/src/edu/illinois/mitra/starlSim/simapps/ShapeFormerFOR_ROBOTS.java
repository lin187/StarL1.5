

package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.List;
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
//import edu.illinois.mitra.starlSim.main.SimSettings;



public class ShapeFormerFOR_ROBOTS extends LogicThread implements MessageListener {

	SortedSet<String> toVisit = new TreeSet<String>();
	private static final String TAG = "Logic";
	SortedSet<String> toVisit2 = new TreeSet<String>();
	SortedSet<String> arrived = new TreeSet<String>();

	private String destname = null;
	private String leader = null;
	
	private boolean running = true;
	
	private boolean iamleader = false;
	private LeaderElection le;
	private Synchronizer sync;
	private int d_r=500; //Some distance that each robot will be from the nearest robot 
	private double theta_r=Math.PI/4; //0<theta_r<2*PI, theta_r != PI/2, 3*PI/2... Be wary of angles <PI/2.7
	
	String robotName = gvh.id.getName();
 	private int numbots=gvh.id.getParticipants().size();
	
	
	
	PositionList posList = new PositionList();
	private final static String SYNC_START = "1";
	private final static String SYNC_BEGINFOLLOW = "2";
	
	
	private enum STAGE { START, SYNC, LE, MOVE, GO, WAIT_TO_ARRIVE1,WAIT_TO_ARRIVE,WAYPOINT_CALC, DONE };
	private STAGE stage = STAGE.START;	
	
	public ShapeFormerFOR_ROBOTS(GlobalVarHolder gvh) {
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
	
	}

	
	@Override
	public List<Object> callStarL() {
		for(ItemPosition ip : gvh.gps.getWaypointPositions().getList()) {
			toVisit.add(ip.name);
		}
		Integer robotNum=0;
        Object [] botarray=gvh.id.getParticipants().toArray();
        for(int i=0; i<gvh.id.getParticipants().size();i++)
        {
        if(name != (String) botarray[i]){}
        else 
        	{  	robotNum=i;
        	}
          }
		while (running) {
			
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
					stage = STAGE.GO;
					iamleader = leader.equals(name);
					System.out.println("Robot to chase? "+leader);
										
				}
							
				break;
				
								
			case GO:
				destname = toVisit.first();
				if (iamleader) 
				{
					gvh.plat.moat.goTo(startpoint());

					while (gvh.plat.moat.inMotion) 
					{
						gvh.sleep(1);
					}
				}
				else{
						
					
					gvh.plat.moat.goTo(startpoint());
					
				}
				sync.barrier_sync(SYNC_BEGINFOLLOW);
				stage = STAGE.WAIT_TO_ARRIVE;
				
				break;
				
			case WAIT_TO_ARRIVE:
				
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
					stage = STAGE.WAIT_TO_ARRIVE;
					
				}
				else if (!toVisit2.isEmpty()) {
				toVisit2.remove("START POINT" + robotNum);
				}
				
				if(toVisit2.isEmpty()) { 
					//IF ADDED NEW MOVEMENT CODE, CHANGE THIS STAGE TO FIRST STAGE OF NEW MOVEMENT CODE
					stage = STAGE.DONE;
				} else {
					stage = STAGE.WAIT_TO_ARRIVE;
				}
				break;
				
					
				//
				//
				//
				//
				//INSERT ALL MOVEMENT CODE HERE
				//
				//
				//
				
				
				
				
				
				
				
				
			case DONE:
				System.out.println(name + " is done.");
				gvh.plat.moat.motion_stop();
				le.cancel();
				sync.cancel();
				gvh.comms.removeMsgListener(99);
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
			
			synchronized(stage) {
				// If no waypoints remain, quit. Otherwise, go on to the next destination
				if(arrived.isEmpty()) { 
					stage = STAGE.DONE;
				} else {
					stage = STAGE.MOVE;
				}
			
		}
		}
	
		
	
	
	
private ItemPosition startpoint() {
		
        Integer robotNum=0;
        Object [] botarray=gvh.id.getParticipants().toArray();
        for(int i=0; i<gvh.id.getParticipants().size();i++)
        {
        if(name != (String) botarray[i]){}
        else 
        	{  	robotNum=i;
        	}
          }
    	toVisit2.add("START POINT" + robotNum);
        if(iamleader){
		return new ItemPosition("goHere",1500, 1500, 0);
		}
        else {
        	if (robotNum % 2 == 0 && !iamleader)
    		{return new ItemPosition("goHere",(int) (1500 +d_r*robotNum/2*Math.cos(theta_r)), (int) (1500 -d_r*robotNum/2*Math.sin(theta_r)), 0);
    		}
        	
        	if (robotNum % 2 == 1)
    		{return new ItemPosition("goHere",(int) (1500 -d_r*(robotNum+1)/2*Math.cos(theta_r)), (int) (1500 -d_r*(robotNum+1)/2*Math.sin(theta_r)), 0);
    		}
        	else 
        	{return null;}

        }
	}
	
	
	
	
}