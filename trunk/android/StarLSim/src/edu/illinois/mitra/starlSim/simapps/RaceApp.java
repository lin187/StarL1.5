package edu.illinois.mitra.starlSim.simapps;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimSettings;

public class RaceApp extends LogicThread implements MessageListener {

	SortedSet<String> toVisit = new TreeSet<String>();
	
	private enum STAGE { START, GO, WAIT_TO_ARRIVE, DONE };
	private STAGE stage = STAGE.START;
	
	private String destname = null;
	
	public RaceApp(GlobalVarHolder gvh) {
		super(gvh);
		gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		
		// Get the list of positions to travel to
		for(ItemPosition ip : gvh.gps.getWaypointPositions().getList()) {
			toVisit.add(ip.name);
		}
		
		gvh.comms.addMsgListener(99, this);
		MotionParameters param = new MotionParameters();
		param.COLAVOID_MODE = MotionParameters.USE_COLAVOID;
		gvh.plat.moat.setParameters(param);
		
		if(gvh.gps.getWaypointPositions().getNumPositions() == 0) System.out.println("The race application requires waypoints to race to!");
	}

	@Override
	public List<Object> callStarL() {
		while(true) {
			gvh.sleep(100);
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
					gvh.sleep(10);
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
