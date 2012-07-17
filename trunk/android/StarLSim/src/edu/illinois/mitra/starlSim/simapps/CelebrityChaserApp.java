//Written by Lucas Buccafusca
//05-24-2012
//What the App does: 
//At the beginning of implementation, the robots are synced together to allow for communication between them.
//A "celebrity" is selected through the random leader selection process
//The celebrity travels to a variety of points (as selected through the .wpt file)
//The others follow the celebrity. 
//They first check her starting location (for the sake of the simulation, assume this is their house)
//Any time the celebrity is 'seen' they update their path to catch up 

package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;





public class CelebrityChaserApp extends LogicThread implements MessageListener {

	SortedSet<String> toVisit = new TreeSet<String>();
	private static final String TAG = "Logic";
	
	private String destname = null;
	private String leader = null;
	
	private boolean running = true;
	
	private boolean iamleader = false;
	private LeaderElection le;
	private Synchronizer sync;
	private ItemPosition targetLocation;
	
	
	
	
	
	PositionList posList = new PositionList();
	private final static String SYNC_START = "1";
	private final static String SYNC_BEGINFOLLOW = "2";
	
	
	private enum STAGE { START, SYNC, LE, GO, WAIT_TO_ARRIVE, DONE };
	private STAGE stage = STAGE.START;	
	
	public CelebrityChaserApp(GlobalVarHolder gvh) {
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
		le = new RandomLeaderElection(gvh);
		
		
	
	}

	
	@Override
	public List<Object> callStarL() {
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
					
					gvh.plat.moat.goTo(gvh.gps.getWaypointPosition(destname));		
					while (gvh.plat.moat.inMotion) 
					{
						gvh.sleep(1);
					}
				}
				else{
										
					targetLocation = gvh.gps.getPosition(leader);
					gvh.plat.moat.goTo(targetLocation);
					
				}
				sync.barrier_sync(SYNC_BEGINFOLLOW);
				stage = STAGE.WAIT_TO_ARRIVE;
				
				break;
				
			case WAIT_TO_ARRIVE:
				
				boolean motionSuccess = true;
				while(gvh.plat.moat.inMotion) { 
					gvh.sleep(10);
					if(!toVisit.contains(destname)) {
						motionSuccess = false;
						break;
					}				
				}
				
				// If Leader Arrives
				if(motionSuccess && iamleader) {
					RobotMessage inform = new RobotMessage("ALL", name, 99, destname);
					gvh.comms.addOutgoingMessage(inform);
					toVisit.remove(destname);
					System.out.println(leader + " reached "+ destname);
					stage = STAGE.GO;
				}
				
				if(toVisit.isEmpty()) { 
					stage = STAGE.DONE;
				} else {
					stage = STAGE.GO;
				}
				break;
				
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
		synchronized(toVisit) {
			// Remove the received waypoint from the list of waypoints to visit
			toVisit.remove(m.getContents(0));
						
		}
		
		synchronized(stage) {
			// If no waypoints remain, quit. Otherwise, go on to the next destination
			if(toVisit.isEmpty()) { 
				stage = STAGE.DONE;
			} else {
				stage = STAGE.GO;
			}
		
	}
	}
	
		
	
	
	
	
}