package edu.illinois.mitra.demo.leaderelect;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
		settings.N_BOTS(5);
		settings.TIC_TIME_RATE(4);
        settings.WAYPOINT_FILE("four.wpt");
		//settings.WAYPOINT_FILE(System.getProperty("user.dir")+"\\trunk\\android\\RaceApp\\waypoints\\four1.wpt");
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.DRAWER(new LeaderElectDrawer());
		
		Simulation sim = new Simulation(LeaderElect.class, settings.build());
		sim.start();
	}

}
