package edu.illinois.mitra.starlSim.simapps;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimSettings;

//UP HERE


public class RaceApp extends LogicThread implements MessageListener {

	SortedSet<String> toVisit = new TreeSet<String>();
	
	private enum STAGE { START, GO, WAIT_TO_ARRIVE, DONE };
	private STAGE stage = STAGE.START;
	
	private String destname = null;
	private boolean run = true;
	
	public RaceApp(GlobalVarHolder gvh) {
		super(gvh);
		gvh.trace.traceStart();
		
		// Get the list of positions to travel to
		for(ItemPosition ip : gvh.gps.getWaypointPositions().getList()) {
			toVisit.add(ip.name);
		}
		
		// Race progress messages are broadcast with message ID 99
		gvh.comms.addMsgListener(99, this);

		// Make sure waypoints were provided
		if(gvh.gps.getWaypointPositions().getNumPositions() == 0) {
			System.err.println("The race application requires waypoints to race to!");
			run = false;
		}
	}

	@Override
	public List<Object> callStarL() {
		while(true) {
			gvh.sleep(100);
			switch(stage) {
			case START:
				if(run)
					stage = STAGE.GO;
				else
					stage = STAGE.DONE;
				break;
				
			case GO:
				destname = toVisit.first();
				gvh.plat.moat.goTo(gvh.gps.getWaypointPosition(destname));
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
				
				// If this robot got to the destination before any other robot
				if(motionSuccess) {
					System.out.println(name + " got to " + destname + " first!");
					
					// Send a message to all other robots informing them that they lost
					RobotMessage inform = new RobotMessage("ALL", name, 99, destname);
					gvh.comms.addOutgoingMessage(inform);
					toVisit.remove(destname);
					
					// Penalty for winning: sleep for almost a full second
					gvh.sleep(800);
				}
				
				if(toVisit.isEmpty()) { 
					stage = STAGE.DONE;
				} else {
					stage = STAGE.GO;
				}
				break;
				
			case DONE:
				gvh.plat.moat.motion_stop();
				return null;
			}
		}
	}

	@Override
	public void messageReceied(RobotMessage m) {
		// Called whenever a message with ID 99 is received
		
		
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
