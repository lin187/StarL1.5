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
	private static final int WAYPOINTS_TO_FOLLOW = 10;

	private RobotMotion moat;
	
	private int cur_waypoint = 0;
	
	private Random rand = new Random();
	
	public LineFollowingApp(GlobalVarHolder gvh) {
		super(gvh);
		gvh.trace.traceStart();
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
				moat.goTo(generateLinePoint());
				System.out.println("Starting motion!");
				break;
			
			case MOVE:
				if(!moat.inMotion) {
					if(cur_waypoint < WAYPOINTS_TO_FOLLOW) {
						cur_waypoint ++;
						moat.goTo(generateLinePoint());
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
	
	private ItemPosition generateRandomPoint() {
		int x = rand.nextInt(5000) - 250;
		int y = rand.nextInt(5000) - 250;
		ItemPosition mypos = gvh.gps.getMyPosition();
		return new ItemPosition("goHere", mypos.x + x, mypos.y + y, 0);
	}
	
	private ItemPosition generateLinePoint() {
		int x = 50;
		int y = 0;
		ItemPosition mypos = gvh.gps.getMyPosition();
		String robotName = gvh.id.getName();
        Integer robotNum = Integer.parseInt(robotName.substring(3)); // assumes: botYYY
		return new ItemPosition("goHere", robotNum * 500, SimSettings.GRID_YSIZE/2, 0);
        //return new ItemPosition("goHere", robotNum * 500, robotNum * 500, 0);
	}
}
