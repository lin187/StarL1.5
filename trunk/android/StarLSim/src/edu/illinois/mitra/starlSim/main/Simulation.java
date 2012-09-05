package edu.illinois.mitra.starlSim.main;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

	private DistancePredicate distpred;

	public Simulation(Class<? extends LogicThread> app, final SimSettings settings) {
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
		LinkedList <LogicThread> logicThreads = new LinkedList <LogicThread>();
		simEngine = new SimulationEngine(settings.MSG_MEAN_DELAY, settings.MSG_STDDEV_DELAY, 
				settings.MSG_LOSSES_PER_HUNDRED, settings.MSG_RANDOM_SEED, settings.TIC_TIME_RATE, 
				blockedRobots, participants, drawFrame.getPanel(), logicThreads);

		// Create the sim gps
		if(settings.IDEAL_MOTION) {
			gps = new IdealSimGpsProvider(simEngine, settings.GPS_PERIOD, settings.GPS_ANGLE_NOISE, settings.GPS_POSITION_NOISE);
		} else {
			gps = new RealisticSimGpsProvider(simEngine, settings.GPS_PERIOD, settings.GPS_ANGLE_NOISE, settings.GPS_POSITION_NOISE, settings.BOT_RADIUS);
		}

		// Load waypoints
		if(settings.WAYPOINT_FILE != null)
			gps.setWaypoints(WptLoader.loadWaypoints(settings.WAYPOINT_FILE));

		simEngine.gps = gps;
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
				System.out.println("Waypoint file did not contain a waypoint named '" + botName + "', using random position.");
				initialPosition = new ItemPosition(botName, rand.nextInt(settings.GRID_XSIZE), rand.nextInt(settings.GRID_YSIZE), rand.nextInt(360));
			}
			
			SimApp sa = new SimApp(botName, participants, simEngine, initialPosition, settings.TRACE_OUT_DIR, app, 
					drawFrame, settings.TRACE_CLOCK_DRIFT_MAX, settings.TRACE_CLOCK_SKEW_MAX); 
			
			bots.add(sa);
			
			logicThreads.add(sa.logic);
		}
		
		// initialize debug drawer class if it was set in the settings
		if (settings.DRAWER != null)
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
				for(ItemPosition ip : gps.getWaypointPositions().getList()) {
					RobotData waypoint = new RobotData(ip.name, ip.x, ip.y, ip.angle);
					waypoint.radius = 5;
					waypoint.c = new Color(255, 0, 0);
					rd.add(waypoint);
				}
				drawFrame.updateData(rd, simEngine.time);
			}
		};
		gps.addObserver(guiObserver);

		if(settings.USE_GLOBAL_LOGGER)
			gps.addObserver(createGlobalLogger(settings));
		
		if(settings.USE_DISTANCE_PREDICATE)
			enableDistancePredicate(settings.PREDICATE_RADIUS, settings.PREDICATE_OUT_DIR);

		// show viewer
		drawFrame.setVisible(true);
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
				gl.updateData(rd, simEngine.time);
			}
		};
		return globalLogger;
	}

	private void enableDistancePredicate(int radius, String dir) {
		if(dir == null) {
			System.err.println("Distance predicate is enabled but the output directory is null!");
			return;
		} else if(radius > 0) {
			distpred = new DistancePredicate("truth", dir, radius);
			gps.addObserver(distpred);
		}
	}

	public void start() {
		executor = Executors.newFixedThreadPool(participants.size());
		System.out.println("Starting with " + participants.size() + " robots");

		// /////////// START EDITS ///////////////////
		// spawn threads manually
		/*
		 * for (SimApp robotLogic : bots) { final SimApp simApp = robotLogic;
		 * 
		 * new Thread() { public void run() { try { simApp.call(); } catch
		 * (Exception e) { e.printStackTrace(); } } }.start(); }
		 * 
		 * while (true) { try { Thread.sleep(Integer.MAX_VALUE); } catch
		 * (InterruptedException e) { break; } }
		 */
		// //////////////// end edits ///////////////

		// Invoke all simulated robots
		List<Future<List<Object>>> results = null;
		try {
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
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		// Print communication statistics and shutdown
		System.out.println("SIMULATION COMPLETE");
		System.out.println("---Message Stats---");
		simEngine.comms.printStatistics();
		simEngine.simulationDone();
		shutdown();
	}

	public void shutdown() {
		if(distpred != null)
			distpred.close();
		executor.shutdownNow();
	}
}
