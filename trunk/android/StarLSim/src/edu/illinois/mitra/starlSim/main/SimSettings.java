package edu.illinois.mitra.starlSim.main;

public class SimSettings {	
	// General simulation settings
	/**
	 * The number of robots to simulate.
	 */
	public int N_BOTS = 4;

	/**
	 * Filename for a .wpt file with waypoints.
	 */
	public String WAYPOINT_FILE = null;

	/**
	 * Filename for a .wpt file with initial positions for robots, or null to
	 * enable random starting locations.
	 */
	public String INITIAL_POSITIONS_FILE = null;

	/**
	 * Enable/disable ideal motion. False uses simulated motion automaton, true
	 * uses unrealistic motion model
	 */
	public boolean IDEAL_MOTION = false;

	/**
	 * The desired rate of time passing. 0 = no limit, 0.5 = half real-time, 1.0 = real-time, etc.
	 */
	public double TIC_TIME_RATE = 5;

	/**
	 * Simulated world width.
	 */
	public int GRID_XSIZE = 3000;
	/**
	 * Simulated world height.
	 */
	public int GRID_YSIZE = 3000;

	
	// Position calculation period and noise options
	/**
	 * Milliseconds. The time between simulated GPS position broadcasts.
	 */
	public long GPS_PERIOD = 75;
	/**
	 * Degrees. The maximum angular noise of simulated GPS positions. 
	 */
	public double GPS_ANGLE_NOISE = 0;
	/**
	 * Millimeters. The maximum position X and Y offset of simulated GPS positions.
	 */
	public double GPS_POSITION_NOISE = 0;

	// Clock skew settings
	/**
	 * Milliseconds. The maximum trace clock drift.
	 */
	public int TRACE_CLOCK_DRIFT_MAX = 100;
	/**
	 * The maximum trace clock skew.
	 */
	public float TRACE_CLOCK_SKEW_MAX = 0.00000015f;

	// Message delay and loss options
	/**
	 * Milliseconds. The average message transit time.
	 */
	public int MSG_MEAN_DELAY = 15;
	/**
	 * Milliseconds. The standard deviation of message transmission times.
	 */
	public int MSG_STDDEV_DELAY = 5;
	/**
	 * Number of messages to drop per hundred messages sent.
	 */
	public int MSG_LOSSES_PER_HUNDRED = 0;
	/**
	 * Seed for random number generator used by the communication channel.
	 */
	public int MSG_RANDOM_SEED = 0;

	// Robot specific settings
	/**
	 * Robot name prefix
	 */
	public String BOT_NAME = "bot";
	/**
	 * Millimeters. The radius of simulated robots.
	 */
	public int BOT_RADIUS = 165;

	// Distance predicate (truth data) settings. THIS IS CURRENTLY NON-FUNCTIONAL.
	/**
	 * Enable/disable the distance predicate
	 */
	public boolean USE_DISTANCE_PREDICATE = false;
	/**
	 * Millimeters. Distance predicate radius.
	 */
	public int PREDICATE_RADIUS = 0; // Zero disables distance predicate checking and truth output
	/**
	 * The number of robots that must be within PREDICATE_RADIUS of each other to trigger a predicate violation. CURRENTLY UNIMPLEMENTED.
	 */
	@Deprecated
	public int N_PREDICATE_ROBOTS = 2; // (UNIMPLEMENTED) The number of robots that must be within radius to trigger a violation
	/**
	 * Predicate output directory.
	 */
	public String PREDICATE_OUT_DIR = null;

	/**
	 * Trace output directory.
	 */
	public String TRACE_OUT_DIR = null;
	/**
	 * Enable/disable the global logger.
	 */
	public boolean USE_GLOBAL_LOGGER = false;

	// trace drawing
	/**
	 * Enable/disable trace drawing
	 */
	public boolean DRAW_TRACE = false;
	/**
	 * The trace length for each robot.
	 */
	public int DRAW_TRACE_LENGTH = 128;

	// drawing
	/**
	 * The maximum frames per second to draw at. If drawing takes too long, lower this.
	 */
	public int MAX_FPS = 40;

	public SimSettings() {
	}
}