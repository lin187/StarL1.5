package edu.illinois.mitra.starlSim.simapps;

import java.util.List;
import java.util.Random;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.RobotMotion;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimSettings;

public class LineFollowingApp extends LogicThread {

	private enum STAGE { START, MOVE, DONE }
	private STAGE stage = STAGE.START;
	private static final int WAYPOINTS_TO_FOLLOW = 5;

	private RobotMotion moat;
	
	private int cur_waypoint = 0;
	
	private Random rand = new Random();
	
	public LineFollowingApp(GlobalVarHolder gvh) {
		super(gvh);
		gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		moat = gvh.plat.moat;
	}
	
	@Override
	public List<Object> callStarL() {
		while(true) {			
			switch (stage) {
			case START:
				gvh.trace.traceSync("LAUNCH");
				gvh.sleep(rand.nextInt(1500)+1000);
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
				return returnResults();
			}
			gvh.sleep(100);
		}
	}
	
	private ItemPosition generatePoint() {
		int x = rand.nextInt(500) - 250;
		int y = rand.nextInt(500) - 250;
		ItemPosition mypos = gvh.gps.getMyPosition();
		return new ItemPosition("goHere", mypos.x + x, mypos.y + y, 0);
	}
}
