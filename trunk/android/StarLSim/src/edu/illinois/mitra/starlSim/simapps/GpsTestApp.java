package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimApp;
import edu.illinois.mitra.starlSim.main.SimSettings;

public class GpsTestApp extends SimApp {

	private enum STAGE { START, MOVE, DONE }
	private STAGE stage = STAGE.START;

	private RobotMotion moat;
	
	private int n_waypoints;
	private int cur_waypoint = 0;
	
	public GpsTestApp(String name, HashMap<String,String> participants, SimComChannel channel, IdealSimGpsProvider gps, ItemPosition initpos) {
		super(name, participants, channel, gps, initpos, "C:\\");
		gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		
		moat = gvh.plat.moat;
		n_waypoints = gvh.gps.getWaypointPositions().getNumPositions()-1;
	}

	public List<String> call() throws Exception {
		while(true) {			
			switch (stage) {
			case START:
				Thread.sleep(2000 + (long) (Math.random()*SimSettings.START_DELAY_MAX));
				gvh.trace.traceSync("LAUNCH");
				stage = STAGE.MOVE;
				moat.goTo(gvh.gps.getWaypointPosition("DEST"+cur_waypoint));
				System.out.println("Starting motion!");
				break;
			
			case MOVE:
				if(!moat.inMotion) {
					if(cur_waypoint < n_waypoints) {
						cur_waypoint ++;
						moat.goTo(gvh.gps.getWaypointPosition("DEST"+cur_waypoint));
					} else {
						stage = STAGE.DONE;
					}
				}
				break;

			case DONE:
				System.out.println("Done");
				gvh.trace.traceEnd();
				return Arrays.asList(results);
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}
}
