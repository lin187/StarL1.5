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
	public double TRACE_CLOCK_SKEW_MAX = 0.00000015f;

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

	public static SimSettings defaultSettings() {
		Builder builder = new Builder();
		return builder.build();
	}

	public static class Builder {
		private int N_BOTS = 4;
		private String WAYPOINT_FILE;
		private String INITIAL_POSITIONS_FILE;
		private boolean IDEAL_MOTION = false;
		private double TIC_TIME_RATE = 5;
		private int GRID_XSIZE = 3000;
		private int GRID_YSIZE = 3000;
		private long GPS_PERIOD = 75;
		private double GPS_ANGLE_NOISE = 0;
		private double GPS_POSITION_NOISE = 0;
		private int TRACE_CLOCK_DRIFT_MAX = 100;
		private double TRACE_CLOCK_SKEW_MAX = 0.00000015;
		private int MSG_MEAN_DELAY = 15;
		private int MSG_STDDEV_DELAY = 5;
		private int MSG_LOSSES_PER_HUNDRED = 0;
		private int MSG_RANDOM_SEED = 0;
		private String BOT_NAME = "bot";
		private int BOT_RADIUS = 165;
		private boolean USE_DISTANCE_PREDICATE = false;
		private int PREDICATE_RADIUS = 0;
		private int N_PREDICATE_ROBOTS = 2;
		private String PREDICATE_OUT_DIR;
		private String TRACE_OUT_DIR;
		private boolean USE_GLOBAL_LOGGER = false;
		private boolean DRAW_TRACE = false;
		private int DRAW_TRACE_LENGTH = 128;
		private int MAX_FPS = 40;

		public Builder N_BOTS(int N_BOTS) {
			this.N_BOTS = N_BOTS;
			return this;
		}

		public Builder WAYPOINT_FILE(String WAYPOINT_FILE) {
			this.WAYPOINT_FILE = WAYPOINT_FILE;
			return this;
		}

		public Builder INITIAL_POSITIONS_FILE(String INITIAL_POSITIONS_FILE) {
			this.INITIAL_POSITIONS_FILE = INITIAL_POSITIONS_FILE;
			return this;
		}

		public Builder IDEAL_MOTION(boolean IDEAL_MOTION) {
			this.IDEAL_MOTION = IDEAL_MOTION;
			return this;
		}

		public Builder TIC_TIME_RATE(double TIC_TIME_RATE) {
			this.TIC_TIME_RATE = TIC_TIME_RATE;
			return this;
		}

		public Builder GRID_XSIZE(int GRID_XSIZE) {
			this.GRID_XSIZE = GRID_XSIZE;
			return this;
		}

		public Builder GRID_YSIZE(int GRID_YSIZE) {
			this.GRID_YSIZE = GRID_YSIZE;
			return this;
		}

		public Builder GPS_PERIOD(long GPS_PERIOD) {
			this.GPS_PERIOD = GPS_PERIOD;
			return this;
		}

		public Builder GPS_ANGLE_NOISE(double GPS_ANGLE_NOISE) {
			this.GPS_ANGLE_NOISE = GPS_ANGLE_NOISE;
			return this;
		}

		public Builder GPS_POSITION_NOISE(double GPS_POSITION_NOISE) {
			this.GPS_POSITION_NOISE = GPS_POSITION_NOISE;
			return this;
		}

		public Builder TRACE_CLOCK_DRIFT_MAX(int TRACE_CLOCK_DRIFT_MAX) {
			this.TRACE_CLOCK_DRIFT_MAX = TRACE_CLOCK_DRIFT_MAX;
			return this;
		}

		public Builder TRACE_CLOCK_SKEW_MAX(double TRACE_CLOCK_SKEW_MAX) {
			this.TRACE_CLOCK_SKEW_MAX = TRACE_CLOCK_SKEW_MAX;
			return this;
		}

		public Builder MSG_MEAN_DELAY(int MSG_MEAN_DELAY) {
			this.MSG_MEAN_DELAY = MSG_MEAN_DELAY;
			return this;
		}

		public Builder MSG_STDDEV_DELAY(int MSG_STDDEV_DELAY) {
			this.MSG_STDDEV_DELAY = MSG_STDDEV_DELAY;
			return this;
		}

		public Builder MSG_LOSSES_PER_HUNDRED(int MSG_LOSSES_PER_HUNDRED) {
			this.MSG_LOSSES_PER_HUNDRED = MSG_LOSSES_PER_HUNDRED;
			return this;
		}

		public Builder MSG_RANDOM_SEED(int MSG_RANDOM_SEED) {
			this.MSG_RANDOM_SEED = MSG_RANDOM_SEED;
			return this;
		}

		public Builder BOT_NAME(String BOT_NAME) {
			this.BOT_NAME = BOT_NAME;
			return this;
		}

		public Builder BOT_RADIUS(int BOT_RADIUS) {
			this.BOT_RADIUS = BOT_RADIUS;
			return this;
		}

		public Builder USE_DISTANCE_PREDICATE(boolean USE_DISTANCE_PREDICATE) {
			this.USE_DISTANCE_PREDICATE = USE_DISTANCE_PREDICATE;
			return this;
		}

		public Builder PREDICATE_RADIUS(int PREDICATE_RADIUS) {
			this.PREDICATE_RADIUS = PREDICATE_RADIUS;
			return this;
		}

		public Builder N_PREDICATE_ROBOTS(int N_PREDICATE_ROBOTS) {
			this.N_PREDICATE_ROBOTS = N_PREDICATE_ROBOTS;
			return this;
		}

		public Builder PREDICATE_OUT_DIR(String PREDICATE_OUT_DIR) {
			this.PREDICATE_OUT_DIR = PREDICATE_OUT_DIR;
			return this;
		}

		public Builder TRACE_OUT_DIR(String TRACE_OUT_DIR) {
			this.TRACE_OUT_DIR = TRACE_OUT_DIR;
			return this;
		}

		public Builder USE_GLOBAL_LOGGER(boolean USE_GLOBAL_LOGGER) {
			this.USE_GLOBAL_LOGGER = USE_GLOBAL_LOGGER;
			return this;
		}

		public Builder DRAW_TRACE(boolean DRAW_TRACE) {
			this.DRAW_TRACE = DRAW_TRACE;
			return this;
		}

		public Builder DRAW_TRACE_LENGTH(int DRAW_TRACE_LENGTH) {
			this.DRAW_TRACE_LENGTH = DRAW_TRACE_LENGTH;
			return this;
		}

		public Builder MAX_FPS(int MAX_FPS) {
			this.MAX_FPS = MAX_FPS;
			return this;
		}

		public SimSettings build() {
			return new SimSettings(this);
		}
	}

	private SimSettings(Builder builder) {
		this.N_BOTS = builder.N_BOTS;
		this.WAYPOINT_FILE = builder.WAYPOINT_FILE;
		this.INITIAL_POSITIONS_FILE = builder.INITIAL_POSITIONS_FILE;
		this.IDEAL_MOTION = builder.IDEAL_MOTION;
		this.TIC_TIME_RATE = builder.TIC_TIME_RATE;
		this.GRID_XSIZE = builder.GRID_XSIZE;
		this.GRID_YSIZE = builder.GRID_YSIZE;
		this.GPS_PERIOD = builder.GPS_PERIOD;
		this.GPS_ANGLE_NOISE = builder.GPS_ANGLE_NOISE;
		this.GPS_POSITION_NOISE = builder.GPS_POSITION_NOISE;
		this.TRACE_CLOCK_DRIFT_MAX = builder.TRACE_CLOCK_DRIFT_MAX;
		this.TRACE_CLOCK_SKEW_MAX = builder.TRACE_CLOCK_SKEW_MAX;
		this.MSG_MEAN_DELAY = builder.MSG_MEAN_DELAY;
		this.MSG_STDDEV_DELAY = builder.MSG_STDDEV_DELAY;
		this.MSG_LOSSES_PER_HUNDRED = builder.MSG_LOSSES_PER_HUNDRED;
		this.MSG_RANDOM_SEED = builder.MSG_RANDOM_SEED;
		this.BOT_NAME = builder.BOT_NAME;
		this.BOT_RADIUS = builder.BOT_RADIUS;
		this.USE_DISTANCE_PREDICATE = builder.USE_DISTANCE_PREDICATE;
		this.PREDICATE_RADIUS = builder.PREDICATE_RADIUS;
		this.N_PREDICATE_ROBOTS = builder.N_PREDICATE_ROBOTS;
		this.PREDICATE_OUT_DIR = builder.PREDICATE_OUT_DIR;
		this.TRACE_OUT_DIR = builder.TRACE_OUT_DIR;
		this.USE_GLOBAL_LOGGER = builder.USE_GLOBAL_LOGGER;
		this.DRAW_TRACE = builder.DRAW_TRACE;
		this.DRAW_TRACE_LENGTH = builder.DRAW_TRACE_LENGTH;
		this.MAX_FPS = builder.MAX_FPS;
	}
}