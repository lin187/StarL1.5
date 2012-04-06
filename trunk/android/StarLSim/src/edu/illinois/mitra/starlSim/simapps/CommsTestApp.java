package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimApp;
import edu.illinois.mitra.starlSim.main.SimSettings;

public class CommsTestApp extends SimApp {
	private enum STAGE { START, SYNC, ELECT, DONE }
	private STAGE stage = STAGE.START;
	
	private LeaderElection le;
	private Synchronizer sn;
	
	public CommsTestApp(String name, HashMap<String,String> participants, SimComChannel channel, IdealSimGpsProvider gps, ItemPosition initpos) {
		super(name, participants, channel, gps, initpos, "C:\\");
		gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		
		le = new RandomLeaderElection(gvh);
		sn = new BarrierSynchronizer(gvh);
		
		results = new String[2];
		results[0] = name;
	}

	@Override
	public List<String> call() throws Exception {
		while(true) {			
			switch(stage) {
			case START:
				gvh.trace.traceSync("Launch");
				sn.barrier_sync("Start");
				stage = STAGE.SYNC;
				break;
			case SYNC:
				if(sn.barrier_proceed("Start")) {
					stage = STAGE.ELECT;
				}
				break;
			case ELECT:
				// Add the elected leader to the results
				// these results are printed out by the simulator when the simulation ends
				// This will let us easily verify that all robots elected the same leader
				results[1] = le.elect();
				stage = STAGE.DONE;
				break;
			case DONE:
				gvh.trace.traceEnd();
				return Arrays.asList(results);
			}
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {}
		}
	}
}
