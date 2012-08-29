package edu.illinois.mitra.starlSim.main;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starlSim.simapps.*;
import edu.illinois.mitra.starlSim.simapps.deere_fardin.DeereFlockingWithDetours;

public class SimSettings {
	// General simulation settings
	public static int 			N_BOTS 				= 5;					// The number of robots to simulate

	public static String	 	waypoint_path		= null;					// path for waypoint files; if null, uses relative paths		
	public static String	 	waypoint_file		= null;	// filename for a .wpt file with waypoints (should be in waypoint_path)
	public static String 		initial_path		= null;					// path for initial condition waypoint files; if null, uses relative paths
	public static String 		initial_file		= "init.wpt";			// filename for a .wpt file with positions or null to enable random starting locations (should be in initial_path)
	public static Class<? extends LogicThread> 		app 				= DeereFlockingWithDetours.class;	// The application to be simulated
	
	public static boolean		IDEAL_MOTION		= true;			// True enables ideal movement, false uses the simulated motion automaton
	public static double		TIC_TIME_RATE		= 0;				// The desired rate of time passing. 0 = no limit, 0.5 = half real-time, 1.0 = real-time, etc. 
	
	// Grid size (when generating random robot positions)
	public static int			GRID_XSIZE	= 3000;
	public static int			GRID_YSIZE	= 3000;
	
	// Position calculation period and noise options
	public static long 			GPS_PERIOD 				= 75;	// Milliseconds
	public static double 		GPS_ANGLE_NOISE 		= 0;	// Degrees
	public static double 		GPS_POSITION_NOISE 		= 0;	// Millimeters (distance units)
	
	// Clock skew settings
	public static int			TRACE_CLOCK_DRIFT_MAX	= 100; // Milliseconds
	public static float			TRACE_CLOCK_SKEW_MAX	= 0.00000015f;

	// Initial start delay
	public static int			START_DELAY_MAX	= 0;			// Milliseconds
	
	// Message delay and loss options
	public static int 			MSG_MEAN_DELAY 			= 15;	// Milliseconds
	public static int 			MSG_STDDEV_DELAY 		= 5;	// Milliseconds
	public static int 			MSG_LOSSES_PER_HUNDRED	= 0;
	public static int			MSG_RANDOM_SEED			= 0;
	
	// Robot specific settings
	public static String		BOT_NAME	= "bot";
	public static int			BOT_RADIUS	= 165;
	
	// Distance predicate (truth data) settings. THIS IS CURRENTLY NON-FUNCTIONAL.
	public static int			PREDICATE_RADIUS	= 0;		// Zero disables distance predicate checking and truth output
	public static int			N_PREDICATE_ROBOTS	= 2;		// (UNIMPLEMENTED) The number of robots that must be within radius to trigger a violation
	public static String		PREDICATE_OUT_DIR	= "C:/";
	public static String		TRACE_OUT_DIR		= "C:/";
	
	// trace drawing
	public static boolean		DRAW_TRACE			= false;
	public static int			DRAW_TRACE_LENGTH	= 128; // points to save for each robot
}