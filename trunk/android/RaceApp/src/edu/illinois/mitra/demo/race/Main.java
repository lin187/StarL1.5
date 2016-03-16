package edu.illinois.mitra.demo.race;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
		settings.N_BOTS(5);
		settings.TIC_TIME_RATE(2);
        settings.WAYPOINT_FILE("four.wpt");
		//settings.WAYPOINT_FILE(System.getProperty("user.dir")+"\\trunk\\android\\RaceApp\\waypoints\\four1.wpt");
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.DRAWER(new RaceDrawer());

        settings.DRAW_ROBOT_STROKE_SIZE(25);
		
		Simulation sim = new Simulation(RaceApp.class, settings.build());
		sim.start();
	}

}
