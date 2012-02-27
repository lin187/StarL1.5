package edu.illinois.mitra;

import java.util.Collection;
import java.util.Iterator;

import android.util.Log;
import edu.illinois.mitra.Objects.LeaderElection;
import edu.illinois.mitra.Objects.MutualExclusion;
import edu.illinois.mitra.Objects.Synchronizer;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.Objects.itemPosition;
import edu.illinois.mitra.bluetooth.RobotMotion;
import edu.illinois.mitra.comms.RobotMessage;

public class LogicThread extends Thread {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private boolean running = true;
	private globalVarHolder gvh = null;
	private RobotMotion motion = null;
	private Synchronizer sync = null;
	private MutualExclusion mutex = null;
	private String name = null;
	private String leader = null;
	private boolean iamleader = false;
	
	// Maximum angle at which robots can curve to their destination.
	// This prevents "soft" corners and forces robots to turn in place at sharper angles
	private static final int MAXCURVEANGLE = 25;
	
	// Constant stage names
	private int stage = 0;
	public static final int STAGE_START = 0;
	public static final int STAGE_LE_SYNC = 1;
	public static final int STAGE_MOTION = 2;
	public static final int STAGE_MESSAGE = 3;
	public static final int STAGE_DONE = 99;
	
	//---------------------
	// Constant message IDs
	public static final int MSG_BARRIERSYNC 			= 5;
	public static final int MSG_MUTEX_TOKEN_OWNER_BCAST = 6;
	public static final int MSG_MUTEX_TOKEN 			= 7;
	public static final int MSG_MUTEX_TOKEN_REQUEST 	= 8;
	public static final int MSG_LEADERELECT 			= 9;
	public static final int MSG_LEADERELECT_ANNOUNCE	= 10;

	public LogicThread(globalVarHolder gvh, RobotMotion motion) {
		this.gvh = gvh;
		this.motion = motion;
		name = gvh.getName();
		sync = new Synchronizer(gvh);
		running = true;
		Log.i(TAG, "I AM " + name);
	}

	@Override
	public void run() {
		while(running) {
			
			/* This is a simple demo program involving leader election, message passing, and motion.
			 * It is not a critical component of the framework and is intended to be changed.
			 */
			
			switch(stage) {
			case STAGE_START:
				sync.barrier_sync("LE");
				stage = STAGE_LE_SYNC;
				break;
				
			case STAGE_LE_SYNC:
				if(sync.barrier_proceed("LE")) {
					LeaderElection le = new LeaderElection(gvh);
					leader = le.elect();
					gvh.sendMainToast("Elected " + leader);
					iamleader = leader.equals(name);
					stage = STAGE_MOTION;
				}
				break;
				
			case STAGE_MOTION:
				if(iamleader) {
					itemPosition dest = new itemPosition("middle",1500,1500,0);
					motion.go_to(dest);
					while(motion.inMotion) {}
					motion.song();
				} else {
					RobotMessage msg = new RobotMessage(leader, name, 11, "HELLO, GLORIOUS LEADER.");
					gvh.addOutgoingMessage(msg);
				}
				stage = STAGE_MESSAGE;
				break;

			case STAGE_MESSAGE:
				if(iamleader) {
					while(gvh.getIncomingMessageCount(11) < (gvh.getParticipants().size()-1)) {}
					
					String debug = "";
					for(RobotMessage msg : gvh.getIncomingMessages(11)) {
						debug = debug + msg.getFrom() + " says: " + msg.toString() + "\n";
					}
					gvh.setDebugInfo(debug);			
				} else {
					stage = STAGE_DONE;
				}
				break;

			case STAGE_DONE:
				//Nothing!
				break;
				
			default:
				Log.e(ERR, "LogicThread somehow ended up in an uncovered stage: " + stage);
				break;
			}	
		}	
	}

	@Override
	public synchronized void start() {
		super.start();
	}
	
	public void cancel() {
		Log.d(TAG, "CANCELLING LOGIC THREAD");
		
		running = false;
		if(mutex != null) {
			mutex.cancel();
		}
	}

	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}	
	}	
}
