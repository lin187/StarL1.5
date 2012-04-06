package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimApp;
import edu.illinois.mitra.starlSim.main.SimSettings;

public class LineFollowingApp extends SimApp {

	private enum STAGE { START, MOVE, DONE }
	private STAGE stage = STAGE.START;
	private static final int WAYPOINTS_TO_FOLLOW = 5;

	private RobotMotion moat;
	
	private int cur_waypoint = 0;
	
	private Random rand = new Random();
	
	public LineFollowingApp(String name, HashMap<String,String> participants, SimComChannel channel, IdealSimGpsProvider gps, ItemPosition initpos) {
		super(name, participants, channel, gps, initpos, "C:\\");
		gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		
		moat = gvh.plat.moat;
	}

	public List<String> call() throws Exception {
		while(true) {			
			switch (stage) {
			case START:
				gvh.trace.traceSync("LAUNCH");
				Thread.sleep(rand.nextInt(1500)+1000);
				stage = STAGE.MOVE;
				moat.goTo(generatePoint());
				System.out.println("Starting motion!");
				break;
			
			case MOVE:
				if(!moat.inMotion) {
					if(cur_waypoint < WAYPOINTS_TO_FOLLOW) {
						cur_waypoint ++;
						moat.goTo(generatePoint());
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
	
	private ItemPosition generatePoint() {
		int x = rand.nextInt(500) - 250;
		int y = rand.nextInt(500) - 250;
		ItemPosition mypos = gvh.gps.getMyPosition();
		return new ItemPosition("goHere", mypos.getX() + x, mypos.getY() + y, 0);
	}
}
