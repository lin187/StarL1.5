package edu.illinois.mitra.synchronous;

import java.util.List;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.Synchronizer;

public class SynchronousModel extends LogicThread {

	public interface SynchronousApp {
		public void runRound(int roundNumber);

		public void receive(RobotMessage m);
	}

	private SynchronousApp app;
	private Synchronizer roundSync;
	private int round = 0;
	private String roundString = "0";

	public SynchronousModel(GlobalVarHolder gvh, SynchronousApp app, int[] messages) {
		super(gvh);

		if(app == null)
			throw new IllegalArgumentException("Application must not be null.");

		this.app = app;

		for(int i : messages)
			gvh.comms.addMsgListener(i, this);

		roundSync = new BarrierSynchronizer(gvh);
	}
	
	private Stage stage;
	public enum Stage {SYNC, WAIT, RUN}; 

	@Override
	public List<Object> callStarL() {
		
		while(true) {
			switch(stage) {
			case SYNC:
				roundString = Integer.toString(round);
				roundSync.barrierSync(roundString);
				stage = Stage.WAIT;
				break;
			case WAIT:
				if(roundSync.barrierProceed(roundString))
					stage = Stage.RUN;
				break;
			case RUN:
				break;
			}
			
			if(0 == 1)
				break;
		}	

		// TODO Auto-generated method stub
		return null;
	}
	// synchronize to round number, then execute callback. All messages received
	// during round n will be delivered immediately before round n+1 begins.

}