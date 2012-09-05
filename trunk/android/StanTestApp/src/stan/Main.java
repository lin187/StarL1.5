package stan;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main
{
	public static void main(String[] args) 
	{
		SimSettings settings = SimSettings.defaultSettings();
		
		settings.N_BOTS = 3;
		settings.IDEAL_MOTION = true;
		settings.DRAWER = new StanDrawer();
		settings.TIC_TIME_RATE = 5;

		Simulation sim = new Simulation(StanLogicThread.class, settings);
		sim.start();
	}
}
