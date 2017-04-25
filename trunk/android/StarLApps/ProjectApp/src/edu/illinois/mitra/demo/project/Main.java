package edu.illinois.mitra.demo.project;


import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {
/*
 * This application make use of RRT path planning to navigate through a maze
 * No corrdination is implemented in this application
 */

    public static void main(String[] args) {
        SimSettings.Builder settings = new SimSettings.Builder();

        settings.N_IROBOTS(3);
        settings.N_GBOTS(3);
        //settings.N_DBOTS(1);

        //	settings.N_RBOTS(1);
        settings.TIC_TIME_RATE(10);
        settings.WAYPOINT_FILE("dest_square.wpt");
        settings.INITIAL_POSITIONS_FILE("start_square.wpt");
        settings.OBSPOINT_FILE("Obstacles_square.wpt");
        settings.THREE_D(false);
        settings.DRAW_WAYPOINTS(false);
        settings.DRAW_WAYPOINT_NAMES(false);
        settings.DRAWER(new ProjectDrawer());
        settings.Detect_Precision(10);
        settings.De_Radius(4);

        //settings.MSG_LOSSES_PER_HUNDRED(100);
//		settings.GPS_POSITION_NOISE(-5);
//		settings.GPS_ANGLE_NOISE(1);
//		settings.BOT_RADIUS(400);

        Simulation sim = new Simulation(ProjectApp.class, settings.build());
        sim.start();


        //for (SimApp b : sim.getBots()) {
          //
        //}

        //sim.shutdown();
    }

}
