package edu.illinois.mitra.demo.traffic;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
		
		settings.N_BOTS(5);
		settings.TIC_TIME_RATE(2);
		settings.WAYPOINT_FILE("dest.wpt");
		settings.INITIAL_POSITIONS_FILE("start.wpt");
		settings.OBSPOINT_FILE("Obstacles.wpt");
		
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.DRAWER(new TrafficSignDrawer());
//		settings.MSG_LOSSES_PER_HUNDRED(20);
//		settings.GPS_POSITION_NOISE(-5);
//		settings.GPS_ANGLE_NOISE(1);
//		settings.BOT_RADIUS(400);
		Simulation sim = new Simulation(TrafficSignApp.class, settings.build());
		sim.start();
	}

}
