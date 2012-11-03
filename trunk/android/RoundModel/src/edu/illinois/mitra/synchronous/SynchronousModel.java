package edu.illinois.mitra.synchronous;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

		gvh.comms.addMsgListener(this, messages);

		roundSync = new BarrierSynchronizer(gvh);
	}

	private Stage stage;

	public enum Stage {
		SYNC, WAIT, RUN, WAIT_FOR_RUN_END
	};

	private Set<RobotMessage> received = Collections.synchronizedSet(new HashSet<RobotMessage>());

	@Override
	protected void receive(RobotMessage m) {
		received.add(m);
	}
	
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
				for(RobotMessage rm : received)
					app.receive(rm);
				received.clear();
				app.runRound(round);
				stage = Stage.SYNC;
				round ++;
				break;
			}

			gvh.sleep(100);
			
			if(0 == 1)
				return null;
		}
	}

	// synchronize to round number, then execute callback. All messages received
	// during round n will be delivered immediately before round n+1 begins.

}