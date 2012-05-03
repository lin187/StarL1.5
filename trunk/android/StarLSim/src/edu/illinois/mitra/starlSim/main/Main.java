package edu.illinois.mitra.starlSim.main;

public class Main {
	
	public static void main(String[] args) {		
		Simulation sim = new Simulation(SimSettings.N_BOTS, SimSettings.waypoint_file, SimSettings.initial_positions, SimSettings.app);
		sim.enableDistacePredicate(SimSettings.PREDICATE_RADIUS, SimSettings.PREDICATE_OUT_DIR);
		sim.start();
	}	
}