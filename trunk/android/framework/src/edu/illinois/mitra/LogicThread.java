package edu.illinois.mitra;

import android.util.Log;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.bluetooth.RobotMotion;
import edu.illinois.mitra.interfaces.MutualExclusion;

public class LogicThread extends Thread {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private boolean running = true;
	private globalVarHolder gvh = null;
	private RobotMotion motion = null;
	private MutualExclusion mutex = null;
	private String name = null;


	public enum STAGE {
		START,DONE
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
	}
	
	@Override
	public void run() {
		while(running) {
			switch(stage) {
			case START:

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
	}

	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}	
	}	
}
