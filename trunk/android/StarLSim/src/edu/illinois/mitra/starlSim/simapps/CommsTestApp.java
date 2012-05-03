package edu.illinois.mitra.starlSim.simapps;

import java.util.List;

import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.Synchronizer;

public class CommsTestApp extends LogicThread {
	private enum STAGE { START, SYNC, ELECT, DONE }
	private STAGE stage = STAGE.START;
	
	private LeaderElection le;
	private Synchronizer sn;
	
	public CommsTestApp(GlobalVarHolder gvh) {
		super(gvh);
		//gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		
		le = new RandomLeaderElection(gvh);
		sn = new BarrierSynchronizer(gvh);
		
		results = new String[2];
		results[0] = name;
	}

	@Override
	public List<Object> callStarL() {
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
					le.elect();
				}
				break;
			case ELECT:
				// Add the elected leader to the results
				// these results are printed out by the simulator when the simulation ends
				// This will let us easily verify that all robots elected the same leader
				if(le.getLeader() != null) {
					results[1] = le.getLeader();
					stage = STAGE.DONE;
				}
				break;
			case DONE:
				gvh.trace.traceEnd();
				return returnResults();
			}
			gvh.sleep(30);
		}
	}
}
