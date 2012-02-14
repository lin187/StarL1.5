package edu.illinois.mitra;

import android.util.Log;
import edu.illinois.mitra.Objects.LeaderElection;
import edu.illinois.mitra.Objects.Synchronizer;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.Objects.itemPosition;
import edu.illinois.mitra.bluetooth.RobotMotion;
import edu.illinois.mitra.comms.RobotMessage;

public class LogicThread extends Thread {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private globalVarHolder gvh;
	private RobotMotion motion;

	private int stage = 0;
	private String name = null;
	private String leader = null;
	
	private Synchronizer sync;
	
	// Constant stage names
	public static final int STAGE_START	= 0;
	public static final int STAGE_MOVE	= 1;
	public static final int STAGE_DONE	= 99;
	public static final int STAGE_LE = 2;
	public static final int STAGE_LE_BARRIER = 3;

	//---------------------
	// Constant message IDs
	public static final int MSG_BARRIERSYNC 			= 5;
	public static final int MSG_MUTEX_TOKEN_OWNER_BCAST = 6;
	public static final int MSG_MUTEX_TOKEN 			= 7;
	public static final int MSG_MUTEX_TOKEN_REQUEST 	= 8;
	public static final int MSG_LEADERELECT 			= 9;
	public static final int MSG_LEADERELECT_ANNOUNCE	= 10;
	
	// Application specific message IDs
	public static final int MSG_HEARTBEAT			= 50;
	
	public LogicThread(globalVarHolder gvh, RobotMotion motion) {
		this.gvh = gvh;
		this.motion = motion;
		name = gvh.getName();
		sync = new Synchronizer(gvh);
	}

	@Override
	public void run() {
		while(true) {
			switch(stage) {
			case STAGE_START:
				sync.barrier_sync("LE");
				stage = STAGE_LE_BARRIER;
				break;
				
			case STAGE_LE_BARRIER:
				if(sync.barrier_proceed("LE")) {
					stage = STAGE_LE;
				}
				break;
				
			case STAGE_LE:
				LeaderElection le = new LeaderElection(gvh);
				leader = le.elect();
				stage = STAGE_MOVE;
				break;
				
			case STAGE_MOVE:
				itemPosition origin = new itemPosition("origin", 0, 0, 0);
				motion.go_to(origin);
				
				RobotMessage hb = new RobotMessage(leader, gvh.getName(),MSG_HEARTBEAT,"HELLO.");
				gvh.addOutgoingMessage(hb);					
				
				while(motion.inMotion) { }
				motion.song(2);
				stage = STAGE_DONE;
				
				break;

			case STAGE_DONE:
				//Nothing!
				break;
				
			default:
				//Nothing here!
				break;
			}	
		}
	}
	
	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		super.start();
	}
	
	public void cancel() {
		Log.d(TAG, "CANCELLING LOGIC THREAD");
	}

	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}	
	}	
}
