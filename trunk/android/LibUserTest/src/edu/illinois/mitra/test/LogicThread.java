package edu.illinois.mitra.test;

import android.util.Log;
import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.BullyLeaderElection;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.globalVarHolder;

public class LogicThread extends Thread {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private boolean running = true;
	private globalVarHolder gvh = null;
	private RobotMotion motion = null;
	private MutualExclusion mutex = null;
	private String name = null;

	private LeaderElection le;
	private Synchronizer sync;

	public enum STAGE {
		START,SYNC,LE,DONE
	}
	private STAGE stage = STAGE.START;
	
	//---------------------
	// Constant message IDs
	public static final int MSG_BARRIERSYNC 			= 1;
	public static final int MSG_MUTEX_TOKEN_OWNER_BCAST = 2;
	public static final int MSG_MUTEX_TOKEN 			= 3;
	public static final int MSG_MUTEX_TOKEN_REQUEST 	= 4;
	public static final int MSG_LEADERELECT 			= 5;
	public static final int MSG_LEADERELECT_ANNOUNCE	= 6;
	
	public LogicThread(globalVarHolder gvh, RobotMotion motion) {
		this.gvh = gvh;
		this.motion = motion;
		name = gvh.getName();
		Log.i(TAG, "I AM " + name);
		
		sync = new BarrierSynchronizer(gvh);
		le = new BullyLeaderElection(gvh);
	}
	
	@Override
	public void run() {
		while(running) {
			switch(stage) {
			case START:
				int sleepFor = 1000 + (int)(Math.random()*1000.0);
				Log.d(TAG, "Sleeping for " + sleepFor);
				sleep(sleepFor);
				sync.barrier_sync("1");
				stage = STAGE.LE;
				break;
				
			case LE:
				Log.d(TAG, "Electing...");
				String leader = le.elect();
				gvh.sendMainToast("LEADER: " + leader);
				stage = STAGE.DONE;
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
