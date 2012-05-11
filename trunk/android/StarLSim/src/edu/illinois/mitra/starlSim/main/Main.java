package edu.illinois.mitra.starlSim.main;

import java.util.Date;
import java.text.*;

public class Main {
	
	public static void main(String[] args) {
		int n_bots_lower = 4;
		int n_bots_upper = 20;
		int n_bots_inc = 4;
		
		Date now = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
		
		for (int n = n_bots_lower; n <= n_bots_upper; n += n_bots_inc)
		{
			SimSettings.N_BOTS = n;
			SimSettings.TRACE_OUT_DIR = "C:\\Users\\tjohnson\\Desktop\\starl\\traces\\" + df.format(now) + "\\bots" + n + "\\";
			
			Simulation sim = new Simulation(SimSettings.N_BOTS, SimSettings.waypoint_file, SimSettings.initial_positions, SimSettings.app);
			sim.enableDistacePredicate(SimSettings.PREDICATE_RADIUS, SimSettings.PREDICATE_OUT_DIR);
			sim.start();

			// TODO: close window on finish...
		}
	}	
}