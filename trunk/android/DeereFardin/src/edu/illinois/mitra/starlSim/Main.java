package edu.illinois.mitra.starlSim;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main
{
	
	public static void main(String[] args) 
	{
		SimSettings settings = SimSettings.defaultSettings();

		settings.N_BOTS = 5;
		settings.IDEAL_MOTION = true;
		settings.DRAWER = new FlockDrawer();
		settings.TIC_TIME_RATE = 200;
		settings.INITIAL_POSITIONS_FILE = "C:/gpsTest/initPos.wpt"; 

		Simulation sim = new Simulation(DeereFlockingWithDetours.class, settings);
		sim.start();
	}
}