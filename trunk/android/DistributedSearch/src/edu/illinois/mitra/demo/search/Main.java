package edu.illinois.mitra.demo.search;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
		
		settings.N_BOTS(5);
		settings.TIC_TIME_RATE(1);
		settings.WAYPOINT_FILE("waypoints/four.wpt");
		settings.INITIAL_POSITIONS_FILE("waypoints/start.wpt");
		settings.OBSPOINT_FILE("waypoints/Obstacles.wpt");
		
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.DRAWER(new DistributedSearchDrawer());
//		settings.MSG_LOSSES_PER_HUNDRED(20);
//		settings.GPS_POSITION_NOISE(-5);
//		settings.GPS_ANGLE_NOISE(1);
//		settings.BOT_RADIUS(400);
		Simulation sim = new Simulation(DistributedSearchApp.class, settings.build());
		sim.start();
	}

}
