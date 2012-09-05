package edu.illinois.mitra.starlSim.simapps;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	private static boolean circles = false;

	public static void main(String[] args) {
		SimSettings settings = SimSettings.defaultSettings();
		settings.N_BOTS = 1;

		if(circles) {
			Simulation sim = new Simulation(CircleMotion.class, settings);
			sim.start();
		} else {
			Simulation sim = new Simulation(StraightLineMotion.class, settings);
			sim.start();
		}
	}

}
