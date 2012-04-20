package edu.illinois.mitra.starlSim.main;

import edu.illinois.mitra.starlSim.simapps.*;

public final class SimSettings {
	// General simulation settings
	public static final int 		N_BOTS 				= 5;
	public static final String	 	waypoint_file		= "C:/gpstest.wpt";
	public static final String 		initial_positions 	= "C:/ips.wpt";
	public static final Class<?> 	app 				= GeocastTestApp.class;
	
	public static final boolean		IDEAL_MOTION		= false;	// True enables ideal movement, false uses the simulated motion automaton
	
	// Distance predicate (truth data) settings
	public static final int			PREDICATE_RADIUS	= 200;		// Zero disables distance predicate checking and truth output
	public static final int			N_PREDICATE_ROBOTS	= 2;		// (UNIMPLEMENTED) The number of robots that must be within radius to trigger a violation
	public static final String		PREDICATE_OUT_DIR	= "C:/";
	public static final String		TRACE_OUT_DIR		= "C:/";
	
	// Clock skew settings
	public static final int			TRACE_CLOCK_DRIFT_MAX	= 100; // Milliseconds
	public static final float		TRACE_CLOCK_SKEW_MAX	= 0.00000015f;

	// Initial start delay
	public static final int			START_DELAY_MAX	= 5;			// Milliseconds
	
	// Message delay and loss options
	public static final int 		MSG_MEAN_DELAY 			= 15;	// Milliseconds
	public static final int 		MSG_STDDEV_DELAY 		= 5;	// Milliseconds
	public static final int 		MSG_LOSSES_PER_HUNDRED	= 0;
	
	// Position calculation period and noise options
	public static final long 		GPS_PERIOD 				= 75;	// Milliseconds
	public static final double 		GPS_ANGLE_NOISE 		= 3;	// Degrees
	public static final double 		GPS_POSITION_NOISE 		= 1;	// Millimeters (distance units)
}