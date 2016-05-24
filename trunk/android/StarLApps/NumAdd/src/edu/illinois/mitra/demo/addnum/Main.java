package edu.illinois.mitra.demo.addnum;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
		settings.N_IROBOTS(6);
		settings.TIC_TIME_RATE(1);
        settings.WAYPOINT_FILE("four.wpt");
		//settings.WAYPOINT_FILE(System.getProperty("user.dir")+"\\trunk\\android\\RaceApp\\waypoints\\four1.wpt");
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.DRAWER(new AddNumDrawer());
		//settings.MSG_MEAN_DELAY(100);
		Simulation sim = new Simulation(AddNumApp.class, settings.build());
		sim.start();
	}

}
