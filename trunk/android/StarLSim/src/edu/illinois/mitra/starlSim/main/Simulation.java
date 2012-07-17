package edu.illinois.mitra.starlSim.main;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
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
import edu.illinois.mitra.starlSim.main.GlobalLogger;
import edu.illinois.mitra.starlSim.draw.RobotData;

public class Simulation {	
	private Collection<SimApp> bots = new HashSet<SimApp>();
	private HashMap<String,String> participants = new HashMap<String, String>();
	private SimGpsProvider gps;
	private SimulationEngine se;	
	
	private ExecutorService executor;

	private DistancePredicate distpred;

	public Simulation(int n_bots, String wptfile, String initposfile, Class<?> appToRun) {
		// Ensure that the class to instantiate is an instance of SimApp
		if(!LogicThread.class.isAssignableFrom(appToRun)) throw new RuntimeException("The requested application, " + appToRun.getSimpleName() + ", does not extend LogicThread!");

		// Start the sim engine
		se = new SimulationEngine(SimSettings.MSG_MEAN_DELAY,SimSettings.MSG_STDDEV_DELAY,SimSettings.MSG_LOSSES_PER_HUNDRED,SimSettings.MSG_RANDOM_SEED,SimSettings.TIC_TIME_RATE);
				
		// Create the sim gps
		if(SimSettings.IDEAL_MOTION) {
			gps = new IdealSimGpsProvider(se, SimSettings.GPS_PERIOD, SimSettings.GPS_ANGLE_NOISE, SimSettings.GPS_POSITION_NOISE);
		} else {
			gps = new RealisticSimGpsProvider(se, SimSettings.GPS_PERIOD, SimSettings.GPS_ANGLE_NOISE, SimSettings.GPS_POSITION_NOISE, SimSettings.BOT_RADIUS);
		}
		
		// Load waypoints
		if(wptfile != null) {
			gps.setWaypoints(WptLoader.loadWaypoints(wptfile));
		}
		se.gps = gps;
		gps.start();
		
		// Load initial positions
		ArrayList<ItemPosition> initpos = null;
		int initposIdx = 0;
		if(initposfile != null) {
			initpos = WptLoader.loadWaypoints(initposfile).getList();
		}
		
		Random rand = new Random();

		// Create participants and instantiate SimApps
		for(int i = 0; i < n_bots; i++) {
			participants.put(SimSettings.BOT_NAME+i, Integer.toString(i));
		}
		
		// initial condition
		for(int i = 0; i < n_bots; i++) {			
			ItemPosition nextInitPos = new ItemPosition("bot"+i,0,0,0);
			if(initpos != null && initpos.size() > 0) {
				ItemPosition tmp = initpos.get(initposIdx); 
				nextInitPos.setPos(tmp.x, tmp.y, tmp.angle);
				initposIdx ++;
				initposIdx %= initpos.size();
			}
			else
			{
				// random initial condition if unspecified
				nextInitPos = new ItemPosition("bot" + i,rand.nextInt(SimSettings.GRID_XSIZE), rand.nextInt(SimSettings.GRID_YSIZE), rand.nextInt(360));
				//nextInitPos = new ItemPosition("bot" + i, n_bots * 2 * (int)Math.toDegrees(Math.cos(2*Math.PI * (i-1) / n_bots)), n_bots * 2 *(int)Math.toDegrees(Math.sin(2*Math.PI * (i-1) / n_bots)), rand.nextInt(360));
			}
			bots.add(new SimApp("bot"+i,participants, se, nextInitPos, SimSettings.TRACE_OUT_DIR, appToRun, SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX));
		}
		
		// Initialize viewer
		final DrawFrame d = new DrawFrame(se.startTime, SimSettings.GRID_XSIZE, SimSettings.GRID_YSIZE);
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		d.setVisible(true);
		
		// Initialize global logger
		final GlobalLogger gl = new GlobalLogger(SimSettings.TRACE_OUT_DIR, "global.txt");
		
		// GUI observer updates the viewer when new positions are calculated
		Observer guiObserver = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				ArrayList<ItemPosition> pos = ((PositionList) arg).getList();
				ArrayList<RobotData> rd = new ArrayList<RobotData>();
				// Add robots
				for(ItemPosition ip : pos) {
					RobotData nextBot = new RobotData(ip.name, ip.x, SimSettings.GRID_YSIZE-ip.y, -ip.angle);
					nextBot.radius = SimSettings.BOT_RADIUS;
					rd.add(nextBot);
				}
				// Add waypoints
				for(ItemPosition ip : gps.getWaypointPositions().getList()) {
					RobotData nrd = new RobotData(ip.name, ip.x, SimSettings.GRID_YSIZE-ip.y, ip.angle);
					nrd.radius = 5;
					nrd.c = new Color(255, 0, 0);
					rd.add(nrd);
				}
				d.updateData(rd, se.time);
			}
		};
		gps.addObserver(guiObserver);
		
		// global logger observer updates the log file when new positions are calculated
		Observer globalLogger = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				ArrayList<ItemPosition> pos = ((PositionList) arg).getList();
				ArrayList<RobotData> rd = new ArrayList<RobotData>();
				// Add robots
				for(ItemPosition ip : pos) {
					RobotData nextBot = new RobotData(ip.name, ip.x, ip.y, ip.angle, ip.receivedTime);
					nextBot.radius = SimSettings.BOT_RADIUS;
					rd.add(nextBot);
				}
				gl.updateData(rd, se.time);
			}
		};
		gps.addObserver(globalLogger);
	}
	
	public void enableDistacePredicate(int radius, String dir) {
		if(radius > 0) {
			distpred = new DistancePredicate("truth", dir, radius);
			gps.addObserver(distpred);
		}
	}
	
	public void start() {
		executor = Executors.newFixedThreadPool(participants.size());
		System.out.println("Starting with " + participants.size() + " robots");
		
		// Invoke all simulated robots
		List<Future<List<Object>>> results = null;
		try {
			results = executor.invokeAll(bots);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Wait until all result values are available
		for(Future<List<Object>> f : results) {
			try {
				List<Object> res = f.get();
				if(res != null && !res.isEmpty()) System.out.println(res);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Print communication statistics and shutdown
		System.out.println("SIMULATION COMPLETE");
		System.out.println("---Message Stats---");
		se.comms.printStatistics();
		se.simulationDone();
		shutdown();
	}

	public void shutdown() {
		if(distpred != null) distpred.close();
		executor.shutdownNow();
	}
}
