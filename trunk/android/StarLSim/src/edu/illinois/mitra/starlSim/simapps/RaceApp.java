package edu.illinois.mitra.starlSim.simapps;

import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimApp;
import edu.illinois.mitra.starlSim.main.SimSettings;

public class RaceApp extends SimApp implements MessageListener {

	SortedSet<String> toVisit = new TreeSet<String>();
	
	private enum STAGE { START, GO, WAIT_TO_ARRIVE, DONE };
	private STAGE stage = STAGE.START;
	
	private String destname = null;
	
	
	public RaceApp(String name, HashMap<String, String> participants, SimComChannel channel, IdealSimGpsProvider gps, ItemPosition initpos) {
		super(name, participants, channel, gps, initpos, "C:/");
		gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		
		// Get the list of positions to travel to
		for(ItemPosition ip : gvh.gps.getWaypointPositions().getList()) {
			toVisit.add(ip.name);
		}
		
		gvh.comms.addMsgListener(99, this);
	}

	@Override
	public List<String> call() throws Exception {
		while(true) {
			Thread.sleep(100);
			switch(stage) {
			case START:
				stage = STAGE.GO;
				break;
				
			case GO:
				destname = toVisit.first();
				gvh.plat.moat.goTo(gvh.gps.getWaypointPosition(destname));
				stage = STAGE.WAIT_TO_ARRIVE;
				break;
				
			case WAIT_TO_ARRIVE:
				boolean motionSuccess = true;
				while(gvh.plat.moat.inMotion) { 
					Thread.sleep(1);
					if(!toVisit.contains(destname)) {
						motionSuccess = false;
						break;
					}				
				}
				
				if(motionSuccess) {
					System.out.println(name + " got to " + destname + " first!");
					RobotMessage inform = new RobotMessage("ALL", name, 99, destname);
					gvh.comms.addOutgoingMessage(inform);
					toVisit.remove(destname);
					Thread.sleep(800);
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
		synchronized(toVisit) {
			toVisit.remove(m.getContents(0));
		}
		
		synchronized(stage) {
			if(toVisit.isEmpty()) { 
				stage = STAGE.DONE;
			} else {
				stage = STAGE.GO;
			}
		}
	}
}
