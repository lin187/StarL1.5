package edu.illinois.mitra.demo.race;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
		
		settings.N_BOTS(2);
		settings.TIC_TIME_RATE(2.5);
		settings.WAYPOINT_FILE("waypoints/four.wpt");
		
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.DRAWER(new RaceDrawer());
		
		Simulation sim = new Simulation(RaceApp.class, settings.build());
		sim.start();
	}

}
