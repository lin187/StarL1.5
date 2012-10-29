package edu.illinois.mitra.test.rounds;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SimSettings settings = new SimSettings.Builder().N_BOTS(4).build();
		Simulation sim = new Simulation(RoundsApp.class, settings);
		sim.start();
	}

}
