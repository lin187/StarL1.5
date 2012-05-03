package edu.illinois.mitra.starlSim.main;

import edu.illinois.mitra.starlSim.simapps.*;

public final class SimSettings {
	// General simulation settings
	public static final int 		N_BOTS 				= 50;				// The number of robots to simulate
	public static final String	 	waypoint_file		= "C:/gpstest.wpt";		// Path to a .wpt file with waypoints
	public static final String 		initial_positions 	= null;				// Path to a .wpt file with positions or null to enable random starting locations
	public static final Class<?> 	app 				= CommsTestApp.class;	// The application to be simulated
	public static final boolean		IDEAL_MOTION		= false;			// True enables ideal movement, false uses the simulated motion automaton
	public static final double		TIC_TIME_RATE		= 0.0;				// The desired rate of time passing. 0 = no limit, 0.5 = half real-time, 1.0 = real-time, etc. 
	
	// Grid size (when generating random robot positions)
	public static final int			GRID_XSIZE	= 5000;
	public static final int			GRID_YSIZE	= 5000;
	
	// Position calculation period and noise options
	public static final long 		GPS_PERIOD 				= 75;	// Milliseconds
	public static final double 		GPS_ANGLE_NOISE 		= 0.5;	// Degrees
	public static final double 		GPS_POSITION_NOISE 		= 1;	// Millimeters (distance units)
	
	// Clock skew settings
	public static final int			TRACE_CLOCK_DRIFT_MAX	= 100; // Milliseconds
	public static final float		TRACE_CLOCK_SKEW_MAX	= 0.00000015f;

	// Initial start delay
	public static final int			START_DELAY_MAX	= 5;			// Milliseconds
	
	// Message delay and loss options
	public static final int 		MSG_MEAN_DELAY 			= 15;	// Milliseconds
	public static final int 		MSG_STDDEV_DELAY 		= 5;	// Milliseconds
	public static final int 		MSG_LOSSES_PER_HUNDRED	= 0;
	public static final int			MSG_RANDOM_SEED			= 0;
	
	// Robot specific settings
	public static final String		BOT_NAME	= "bot";
	public static final int			BOT_RADIUS	= 170;
	
	// Distance predicate (truth data) settings. THIS IS CURRENTLY UNIMPLEMENTED
	public static final int			PREDICATE_RADIUS	= 0;		// Zero disables distance predicate checking and truth output
	public static final int			N_PREDICATE_ROBOTS	= 2;		// (UNIMPLEMENTED) The number of robots that must be within radius to trigger a violation
	public static final String		PREDICATE_OUT_DIR	= "C:/";
	public static final String		TRACE_OUT_DIR		= "C:/";
}