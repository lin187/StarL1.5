package edu.illinois.mitra.starlSim.main;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.harness.RealisticSimGpsProvider;
import edu.illinois.mitra.starl.harness.SimGpsProvider;
import edu.illinois.mitra.starl.harness.SimulationEngine;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;
import edu.illinois.mitra.starlSim.draw.DrawFrame;
import edu.illinois.mitra.starlSim.draw.RobotData;

public class Simulation {
	private Collection<SimApp> bots = new HashSet<SimApp>();
	private HashMap<String, String> participants = new HashMap<String, String>();
	private SimGpsProvider gps;
	private SimulationEngine simEngine;

	private ExecutorService executor;

	private final SimSettings settings;

	public Simulation(Class<? extends LogicThread> app, final SimSettings settings) {
		if(settings.N_BOTS <= 0)
			throw new IllegalArgumentException("Must have more than zero robots to simulate!");

		// Create set of robots whose wireless is blocked for passage between
		// the GUI and the simulation communication object
		Set<String> blockedRobots = new HashSet<String>();

		// Create participants and instantiate SimApps
		for(int i = 0; i < settings.N_BOTS; i++) {
			// Mapping between robot name and IP address
			participants.put(settings.BOT_NAME + i, "192.168.0." + i);
		}

		// Initialize viewer
		final DrawFrame drawFrame = new DrawFrame(participants.keySet(), blockedRobots, settings);
		drawFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Start the simulation engine
		LinkedList<LogicThread> logicThreads = new LinkedList<LogicThread>();
		simEngine = new SimulationEngine(settings.MSG_MEAN_DELAY, settings.MSG_STDDEV_DELAY, settings.MSG_LOSSES_PER_HUNDRED, settings.MSG_RANDOM_SEED, settings.TIC_TIME_RATE, blockedRobots, participants, drawFrame.getPanel(), logicThreads);

		// Create the sim gps
		if(settings.IDEAL_MOTION) {
			gps = new IdealSimGpsProvider(simEngine, settings.GPS_PERIOD, settings.GPS_ANGLE_NOISE, settings.GPS_POSITION_NOISE);
		} else {
			gps = new RealisticSimGpsProvider(simEngine, settings.GPS_PERIOD, settings.GPS_ANGLE_NOISE, settings.GPS_POSITION_NOISE, settings.BOT_RADIUS);
		}

		// Load waypoints
		if(settings.WAYPOINT_FILE != null)
			gps.setWaypoints(WptLoader.loadWaypoints(settings.WAYPOINT_FILE));

		this.settings = settings;
		simEngine.setGps(gps);
		gps.start();

		// Load initial positions
		PositionList initialPositions;
		if(settings.INITIAL_POSITIONS_FILE != null)
			initialPositions = WptLoader.loadWaypoints(settings.INITIAL_POSITIONS_FILE);
		else
			initialPositions = new PositionList();

		Random rand = new Random();

		// Create each robot
		for(int i = 0; i < settings.N_BOTS; i++) {
			String botName = settings.BOT_NAME + i;

			ItemPosition initialPosition = initialPositions.getPosition(botName);

			// If no initial position was supplied, randomly generate one
			if(initialPosition == null) {
				int retries = 0;
				while(retries++ < 1000 && !acceptableStart(initialPosition))
					initialPosition = new ItemPosition(botName, rand.nextInt(settings.GRID_XSIZE), rand.nextInt(settings.GRID_YSIZE), rand.nextInt(360));
			}

			SimApp sa = new SimApp(botName, participants, simEngine, initialPosition, settings.TRACE_OUT_DIR, app, drawFrame, settings.TRACE_CLOCK_DRIFT_MAX, settings.TRACE_CLOCK_SKEW_MAX);

			bots.add(sa);

			logicThreads.add(sa.logic);
		}

		// initialize debug drawer class if it was set in the settings
		if(settings.DRAWER != null)
			drawFrame.addPredrawer(settings.DRAWER);

		// GUI observer updates the viewer when new positions are calculated
		Observer guiObserver = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				ArrayList<ItemPosition> pos = ((PositionList) arg).getList();
				ArrayList<RobotData> rd = new ArrayList<RobotData>();
				// Add robots
				for(ItemPosition ip : pos) {
					RobotData nextBot = new RobotData(ip.name, ip.x, ip.y, ip.angle);
					nextBot.radius = settings.BOT_RADIUS;
					rd.add(nextBot);
				}
				// Add waypoints
				if(settings.DRAW_WAYPOINTS) {
					for(ItemPosition ip : gps.getWaypointPositions().getList()) {
						RobotData waypoint = new RobotData((settings.DRAW_WAYPOINT_NAMES ? ip.name : ""), ip.x, ip.y, ip.angle);
						waypoint.radius = 5;
						waypoint.c = new Color(255, 0, 0);
						rd.add(waypoint);
					}
				}
				drawFrame.updateData(rd, simEngine.getTime());
			}
		};
		gps.addObserver(guiObserver);

		if(settings.USE_GLOBAL_LOGGER)
			gps.addObserver(createGlobalLogger(settings));

		// show viewer
		drawFrame.setVisible(true);
	}

	private static final double BOT_SPACING_FACTOR = 2.6;
	private Map<String, ItemPosition> startingPositions = new HashMap<String, ItemPosition>();

	private boolean acceptableStart(ItemPosition pos) {
		if(pos == null)
			return false;
		startingPositions.put(pos.getName(), pos);
		for(Entry<String, ItemPosition> entry : startingPositions.entrySet()) {
			if(!entry.getKey().equals(pos.getName())) {
				if(entry.getValue().distanceTo(pos) < (BOT_SPACING_FACTOR * settings.BOT_RADIUS))
					return false;
			}
		}
		return true;
	}

	/**
	 * Add an Observer to the list of GPS observers. This Observer's update
	 * method will be passed a PositionList object as the argument. This must be
	 * called before the simulation is started!
	 * 
	 * @param o
	 */
	public void addPositionObserver(Observer o) {
		if(executor == null)
			gps.addObserver(o);
	}

	private Observer createGlobalLogger(final SimSettings settings) {
		final GlobalLogger gl = new GlobalLogger(settings.TRACE_OUT_DIR, "global.txt");

		// global logger observer updates the log file when new positions are calculated
		Observer globalLogger = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				ArrayList<ItemPosition> pos = ((PositionList) arg).getList();
				ArrayList<RobotData> rd = new ArrayList<RobotData>();
				// Add robots
				for(ItemPosition ip : pos) {
					RobotData nextBot = new RobotData(ip.name, ip.x, ip.y, ip.angle, ip.receivedTime);
					nextBot.radius = settings.BOT_RADIUS;
					rd.add(nextBot);
				}
				gl.updateData(rd, simEngine.getTime());
			}
		};
		return globalLogger;
	}

	/**
	 * Begins executing a simulation. This call will block until the simulation completes.
	 */
	public void start() {
		executor = Executors.newFixedThreadPool(participants.size());
		System.out.println("Starting with " + participants.size() + " robots");

		// Save settings to JSON file
		if(settings.TRACE_OUT_DIR != null)
			SettingsWriter.writeSettings(settings);

		// Invoke all simulated robots
		List<Future<List<Object>>> results = null;
		try {
			if(settings.TIMEOUT > 0)
				results = executor.invokeAll(bots, settings.TIMEOUT, TimeUnit.SECONDS);
			else
				results = executor.invokeAll(bots);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		// Wait until all result values are available
		for(Future<List<Object>> f : results) {
			try {
				List<Object> res = f.get();
				if(res != null && !res.isEmpty())
					System.out.println(res);
			} catch(CancellationException e) {
				// If the executor timed out, the result is cancelled
				System.err.println("Simulation timed out! Execution reached " + settings.TIMEOUT + " sec duration. Aborting.");
				break;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		// Print communication statistics and shutdown
		System.out.println("SIMULATION COMPLETE");
		System.out.println("---Message Stats---");
		simEngine.getComChannel().printStatistics();
		shutdown();
	}

	public void shutdown() {
		simEngine.simulationDone();
		executor.shutdownNow();
	}
	
	public long getSimulationDuration() {
		return simEngine.getDuration();
	}
}
