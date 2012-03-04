package edu.illinois.mitra.functions;

import java.util.HashMap;

import android.util.Log;
import edu.illinois.mitra.LogicThread;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.comms.RobotMessage;
import edu.illinois.mitra.interfaces.MessageListener;
import edu.illinois.mitra.interfaces.Synchronizer;

public class BarrierSynchronizer implements Synchronizer, MessageListener {
	private static String TAG = "Synchronizer";
	private static String ERR = "Critical Error";
	
	private globalVarHolder gvh;
	// Barriers tracks which barriers are active and how many robots have reported ready to proceed for each
	// Keys are barrier IDs, values are number of robots ready to proceed
	private HashMap<String,Integer> barriers;
	private int n_participants;
	private String name;
	
	public BarrierSynchronizer(globalVarHolder gvh) {
		this.gvh = gvh;
		n_participants = gvh.getParticipants().size();
		barriers = new HashMap<String,Integer>();
		name = gvh.getName();
		gvh.addMsgListener(LogicThread.MSG_BARRIERSYNC, this);
	}
	
	public void barrier_sync(String barrierID) {
		if(barriers.containsKey(barrierID)) {
			Integer currentCount = barriers.get(barrierID);
			barriers.put(barrierID, Integer.valueOf(currentCount + 1));
			Log.d(TAG, "Updated barrier for bID " + barrierID);
		} else {
			barriers.put(barrierID, new Integer(1));
			Log.d(TAG, "Added barrier for bID " + barrierID);
		}
		RobotMessage notify_sync = new RobotMessage("ALL", name, LogicThread.MSG_BARRIERSYNC, barrierID);
		gvh.addOutgoingMessage(notify_sync);
	}
	
	public boolean barrier_proceed(String barrierID) {	
		if(barriers.get(barrierID) == n_participants) {
			Log.i(TAG, "Barrier " + barrierID + " has all robots ready to proceed!");
			barriers.remove(barrierID);
			return true;
		}
		return false;
	}

	public void messageReceied(RobotMessage m) {
		// Update the barriers when a barrier sync message is received
		String bID = m.getContents();
		
		if(barriers.containsKey(bID)) {
			Integer currentCount = barriers.get(bID);
			barriers.put(bID, Integer.valueOf(currentCount + 1));
			Log.d(TAG, "Received barrier notice for bID " + bID + " from " + m.getFrom() + ". Current count: " + barriers.get(bID));
		} else {
			Log.d(TAG, "Received first barrier notice for bID " + bID);
			barriers.put(bID, new Integer(1));
		}
	}
	
	public void cancel() {
		gvh.removeMsgListener(LogicThread.MSG_BARRIERSYNC);
	}
}
