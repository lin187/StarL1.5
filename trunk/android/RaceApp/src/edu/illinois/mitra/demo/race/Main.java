package edu.illinois.mitra.demo.race;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
		
		settings.N_BOTS(2);
		settings.N_GBOTS(1);
		settings.N_DBOTS(1);
	//	settings.N_RBOTS(1);
		settings.TIC_TIME_RATE(1);
		settings.WAYPOINT_FILE("waypoints/four.wpt");

		settings.OBSPOINT_FILE("waypoints/Obstacles.wpt");
		
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.DRAWER(new RaceDrawer());
		settings.Detect_Precision(10);
		settings.De_Radius(4);
		settings.MSG_LOSSES_PER_HUNDRED(100);
//		settings.GPS_POSITION_NOISE(-5);
//		settings.GPS_ANGLE_NOISE(1);
//		settings.BOT_RADIUS(400);
		Simulation sim = new Simulation(RaceApp.class, settings.build());
		sim.start();
	}

}
