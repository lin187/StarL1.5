package edu.illinois.mitra.demo.race;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;
import edu.illinois.mitra.starlSim.data.CsvWriter;

import java.io.*;
import java.util.Set;

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

        //settings.TIMEOUT(10);

        settings.DRAW_TRACE(true);
        settings.TRACE_OUT_DIR("test");
        settings.USE_GLOBAL_LOGGER(true);


        settings.DRAW_ROBOT_STROKE_SIZE(25);
		
		Simulation sim = new Simulation(RaceApp.class, settings.build());
		sim.start();


        /*CsvWriter writer = null;
        try {
            writer = new CsvWriter("test5.csv", "N Robots",
                    "Execution duration", "Requests made", "Assignments made",
                    "Unpainted lines", "Total lines");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object[] res = sim.getResults().get(0).toArray();
        writer.commit(5, sim.getSimulationDuration() / 1000.0,
                res[1], res[2], res[3]);

        writer.close();*/

    }

}
