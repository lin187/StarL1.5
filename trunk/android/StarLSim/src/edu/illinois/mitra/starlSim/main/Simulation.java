package edu.illinois.mitra.starlSim.main;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JFrame;

import edu.illinois.mitra.starl.harness.AsyncSimComChannel;
import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;
import edu.illinois.mitra.starlSim.draw.DrawFrame;
import edu.illinois.mitra.starlSim.draw.RobotData;

public class Simulation {	
	private Collection<SimApp> bots = new HashSet<SimApp>();
	private HashMap<String,String> participants = new HashMap<String, String>();
	private SimComChannel channel = new AsyncSimComChannel(SimSettings.MSG_MEAN_DELAY,SimSettings.MSG_STDDEV_DELAY,SimSettings.MSG_LOSSES_PER_HUNDRED);
	private IdealSimGpsProvider gps = new IdealSimGpsProvider(SimSettings.GPS_PERIOD, SimSettings.GPS_ANGLE_NOISE, SimSettings.GPS_POSITION_NOISE);
	private ExecutorService executor;

	private DistancePredicate distpred;

	public Simulation(int n_bots, String wptfile, String initposfile, Class<?> appToRun) {
		// Ensure that the class to instantiate is an instance of SimApp
		if(!SimApp.class.isAssignableFrom(appToRun)) throw new RuntimeException("The requested application does not extend SimApp!");

		executor = Executors.newFixedThreadPool(n_bots);
		
		// Load waypoints
		if(wptfile != null) gps.setWaypoints(WptLoader.loadWaypoints(wptfile));
		gps.start();
		
		// Load initial positions
		ArrayList<ItemPosition> initpos = null;
		int initposIdx = 0;
		if(initposfile != null) {
			initpos = WptLoader.loadWaypoints(initposfile).getList();
		}
		
		// Create participants and instantiate SimApps
		for(int i = 0; i < n_bots; i++) {
			participants.put("bot"+i, Integer.toString(i));
		}
		for(int i = 0; i < n_bots; i++) {
			SimApp n = null;
			
			ItemPosition nextInitPos = new ItemPosition("bot"+i,0,0,0);
			if(initpos != null) {
				ItemPosition tmp = initpos.get(initposIdx); 
				nextInitPos.setPos(tmp.x, tmp.y, tmp.angle);
				initposIdx ++;
				initposIdx %= initpos.size();
			}
			
			try {
				// Generically instantiate an instance of the requested SimApp
				n = (SimApp) appToRun.getConstructor(String.class, HashMap.class, SimComChannel.class, IdealSimGpsProvider.class, ItemPosition.class)
						.newInstance("bot"+i,participants,channel, gps, nextInitPos);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			try {
				bots.add(n);
			} catch(NullPointerException e) {
				System.out.println("Failed to instantiate a new " + appToRun.getSimpleName());
			}
		}
		
		// Initialize viewer
		final DrawFrame d = new DrawFrame();
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		d.setVisible(true);
		
		// GUI observer updates the viewer when new positions are calculated
		Observer guiObserver = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				ArrayList<ItemPosition> pos = ((PositionList) arg).getList();
				ArrayList<RobotData> rd = new ArrayList<RobotData>();
				// Add robots
				for(ItemPosition ip : pos) {
					rd.add(new RobotData(ip.name, ip.x, ip.y, ip.angle));
				}
				// Add waypoints
				for(ItemPosition ip : gps.getWaypointPositions().getList()) {
					RobotData nrd = new RobotData(ip.name, ip.x, ip.y, ip.angle);
					nrd.radius = 5;
					nrd.c = new Color(255, 0, 0);
					rd.add(nrd);
				}
				d.updateData(rd);
			}
		};
		gps.addObserver(guiObserver);
	}
	
	public void enableDistacePredicate(int radius, String dir) {
		if(radius > 0) {
			distpred = new DistancePredicate("truth", dir, radius);
			gps.addObserver(distpred);
		}
	}
	
	public void start() {
		System.out.println("Starting with " + participants.size() + " robots");
		
		// Invoke all simulated robots
		List<Future<List<String>>> results = null;
		try {
			results = executor.invokeAll(bots);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Wait until all result values are available
		for(Future<List<String>> f : results) {
			while(!f.isDone()) {}
			try {
				List<String> res = f.get();
				if(res != null && !res.isEmpty()) System.out.println(res);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Print communication statistics and shutdown
		System.out.println("SIMULATION COMPLETE");
		System.out.println("---Message Stats---");
		channel.printStatistics();
		shutdown();
	}

	public void shutdown() {
		if(distpred != null) distpred.close();
		executor.shutdownNow();
	}
}
