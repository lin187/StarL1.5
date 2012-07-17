//Written by Lucas Buccafusca
//05-25-2012
//What the App does: 
//At the beginning of implementation, the robots are synced together to allow for communication between them.
//The user must provide a list of waypoints that outnumber the number of robots
//At the completion of the program, each robot will be an a unique waypoint

package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
//import java.util.Random;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;



public class Waypointselect extends LogicThread implements MessageListener {

	SortedSet<String> toVisit = new TreeSet<String>();
	private static final String TAG = "Logic";
	
	private String destname = null;
	
	private boolean running = true;
	
	private Random rand = new Random();
	private Synchronizer sync;
	
	private int n_waypoints;
	
	
	
	PositionList posList = new PositionList();
	private final static String SYNC_START = "1";
	
	
	private enum STAGE { START, SYNC,GO, PICK, DONE };
	private STAGE stage = STAGE.START;	
	
	public Waypointselect(GlobalVarHolder gvh) {
		super(gvh);
		
		
		for(ItemPosition ip : gvh.gps.getWaypointPositions().getList()) {
			toVisit.add(ip.name);
		}
		
		gvh.comms.addMsgListener(99, this);

		if(gvh.gps.getWaypointPositions().getNumPositions() == 0) System.out.println("This application requires waypoints to travel to!");
	
		sync = new BarrierSynchronizer(gvh);
		
		n_waypoints = gvh.gps.getWaypointPositions().getNumPositions();

	
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
					stage = STAGE.PICK;
					gvh.log.d(TAG, "Synced!");
				}
				break;
				
			case GO:
				
				stage = STAGE.PICK;
				break;
				
			case PICK:
				//Selects a random waypoint
				int go_to = rand.nextInt(n_waypoints);
				destname="DEST"+go_to;
				gvh.plat.moat.goTo(gvh.gps.getWaypointPosition(destname));
				//Checks to see if the robot actually visited the waypoint I wants to go to
				boolean reached_destination = true;
				while(gvh.plat.moat.inMotion) { 
					gvh.sleep(1);
					if(!toVisit.contains(destname)) {
						reached_destination = false;
						break;
					}				
				}
				
				// If this robot got to the destination before any other robot
				if(reached_destination) {
					System.out.println(name + " got to " + destname + ". No other robots can go to this waypoint.");
					
					// Send a message to all other robots informing them that they must reselect a waypoint
					//if they had chosen it at the beginning
					RobotMessage inform = new RobotMessage("ALL", name, 99, destname);
					gvh.comms.addOutgoingMessage(inform);
					toVisit.remove(destname);
					stage = STAGE.PICK;
				}
				
				if(toVisit.isEmpty()&&reached_destination) { 
					stage = STAGE.PICK;
				} else if (toVisit.isEmpty()&&!reached_destination){
					stage = STAGE.DONE;
				}
				else if (!toVisit.isEmpty()&&reached_destination){ 
					stage = STAGE.DONE;
				} 
				else if (!toVisit.isEmpty()&&!reached_destination){ 
					stage = STAGE.PICK;
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
			toVisit.remove(m.getContents(0));
						
		}
		
		synchronized(stage) {
			if(toVisit.isEmpty()) { 
				stage = STAGE.PICK;
			} else {
				stage = STAGE.DONE;
			}
			
		
	}
	}
	
		
	
	
	
	
}