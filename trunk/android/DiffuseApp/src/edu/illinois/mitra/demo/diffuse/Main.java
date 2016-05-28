package src.edu.illinois.mitra.demo.diffuse;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	public static void main(String[] args) {
		SimSettings.Builder settings = new SimSettings.Builder();
		settings.N_BOTS(8);
		settings.TIC_TIME_RATE(1.5);
        settings.WAYPOINT_FILE("four.wpt");
		settings.INITIAL_POSITIONS_FILE("four1.wpt");
		//settings.WAYPOINT_FILE(System.getProperty("user.dir")+"\\trunk\\android\\RaceApp\\waypoints\\four1.wpt");
		settings.DRAW_WAYPOINTS(false);
		settings.DRAW_WAYPOINT_NAMES(false);
		settings.DRAWER(new DiffuseDrawer());
		
		Simulation sim = new Simulation(DiffuseApp.class, settings.build());
		sim.start();
	}

}