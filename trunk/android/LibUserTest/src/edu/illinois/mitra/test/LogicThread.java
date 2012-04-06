package edu.illinois.mitra.test;

import android.util.Log;
import edu.illinois.mitra.starl.bluetooth.MotionAutomaton;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.interfaces.Synchronizer;

public class LogicThread extends Thread {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private boolean running = true;
	private GlobalVarHolder gvh = null;
	private MotionAutomaton motion = null;
	private MutualExclusion mutex = null;
	private String name = null;

	private LeaderElection le;
	private Synchronizer sync;

	public enum STAGE {
		START,SYNC,LE,MOVE,DONE
	}
	private STAGE stage = STAGE.START;
	
	private String leader = null;
	
	public LogicThread(GlobalVarHolder gvh, MotionAutomaton motion) {
		this.gvh = gvh;
		this.motion = motion;
		name = gvh.id.getName();
		Log.i(TAG, "I AM " + name);
		
		sync = new BarrierSynchronizer(gvh);
		le = new RandomLeaderElection(gvh);
	}
	
	@Override
	public void run() {
		while(running) {
			switch(stage) {
			case START:
				int sleepFor = 1000 + (int)(Math.random()*1000.0);
				Log.d(TAG, "Sleeping for " + sleepFor);
				sleep(sleepFor);
				//sync.barrier_sync("1");
				stage = STAGE.MOVE;
				break;
				
			case LE:
				Log.d(TAG, "Electing...");
				leader = le.elect();
				gvh.plat.sendMainToast("LEADER: " + leader);
				stage = STAGE.MOVE;
				break;
			case MOVE:
				//if(leader.equals(name)) {
					motion.GoTo(gvh.gps.getWaypointPosition("middle"));
				//} else {
					while(motion.inMotion) {}
					gvh.plat.sendMainToast("Done moving!");
					stage = STAGE.DONE;
				//}
				break;
			case DONE:
				//Nothing!
				break;
				
			default:
				break;
			}	
		}
	}
	
	@Override
	public synchronized void start() {
		super.start();
		running = true;
	}
	
	public void cancel() {
		Log.d(TAG, "CANCELLING LOGIC THREAD");
		
		running = false;
		if(mutex != null) {
			mutex.cancel();
		}
		if(sync != null) {
			sync.cancel();
		}
		if(le != null) {
			le.cancel();
		}
	}

	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}	
	}	
}
