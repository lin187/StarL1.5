package edu.illinois.mitra.starlSim.main;

import java.util.Date;
import java.text.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
	
	public static void main(String[] args) {
		Date now = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		
		// use relative directories for input and output files, if paths unspecified
		String pwd = System.getProperty("user.dir") + "/";
		System.out.println("Simulation base directory:" + pwd);
		
		// use relative paths if null
		if (SimSettings.waypoint_path == null) {
			SimSettings.waypoint_path = pwd + "../"; // in parent directory of StarLSim
		}
		
		// use relative paths if null
		if (SimSettings.initial_path == null) {
			SimSettings.initial_path = pwd + "../";
		}
		
		// concat path to file
		if (SimSettings.waypoint_file != null) {
			SimSettings.waypoint_file = SimSettings.waypoint_path + SimSettings.waypoint_file;
		}
		
		// concat path to file
		if (SimSettings.initial_file != null) {
			SimSettings.initial_file = SimSettings.initial_path + SimSettings.initial_file;
		}
		
		// set output directories based on machine name
		try {
				String computerName = InetAddress.getLocalHost().getHostName();
				SimSettings.TRACE_OUT_DIR = pwd + "../../../traces/" + computerName + "/" + df.format(now) + "/"; // in top-level repos directory
				System.out.println(SimSettings.TRACE_OUT_DIR);
			}
		catch (UnknownHostException e) {
			  e.printStackTrace();
			  }
		
		// If N_BOTS is negative or zero, automate testing for a range of robots otherwise, use the specified value
		if(SimSettings.N_BOTS <= 0) {
			int n_bots_lower = 4;
			int n_bots_upper = 20;
			int n_bots_inc = 4;
			
			for (int n = n_bots_lower; n <= n_bots_upper; n += n_bots_inc)
			{
				SimSettings.N_BOTS = n;
				SimSettings.TRACE_OUT_DIR += "bots" + n + "/"; // concat to make a folder for each number of bots
				
				Simulation sim = new Simulation(SimSettings.N_BOTS, SimSettings.waypoint_file, SimSettings.initial_file, SimSettings.app);
				sim.enableDistacePredicate(SimSettings.PREDICATE_RADIUS, SimSettings.PREDICATE_OUT_DIR);
				sim.start();

				sim.shutdown(); // sometimes necessary to kill all the threads (arose in Geocast batch simulations)
				sim = null;
	
				// TODO: close window on finish...
			}
		} else {
			Simulation sim = new Simulation(SimSettings.N_BOTS, SimSettings.waypoint_file, SimSettings.initial_file, SimSettings.app);
			sim.enableDistacePredicate(SimSettings.PREDICATE_RADIUS, SimSettings.PREDICATE_OUT_DIR);
			sim.start();
		}
	}	
}