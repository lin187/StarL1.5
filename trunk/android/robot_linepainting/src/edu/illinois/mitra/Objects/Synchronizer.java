package edu.illinois.mitra.Objects;

import java.util.HashMap;

import android.util.Log;

import edu.illinois.mitra.LogicThread;
import edu.illinois.mitra.comms.RobotMessage;

public class Synchronizer {
	private static String TAG = "Synchronizer";
	private static String ERR = "Critical Error";
	
	private globalVarHolder gvh;
	// Barriers tracks which barriers are active and how many robots have reported ready to proceed for each
	// Keys are barrier IDs, values are number of robots ready to proceed
	private HashMap<String,Integer> barriers;
	private int n_participants;
	private String name;
	
	public Synchronizer(globalVarHolder gvh) {
		this.gvh = gvh;
		n_participants = gvh.getParticipants().size();
		barriers = new HashMap<String,Integer>();
		name = gvh.getName();
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
		// Update the barriers
		int msgs = gvh.getIncomingMessageCount(LogicThread.MSG_BARRIERSYNC);
		for(int i = 0; i < msgs; i++) {
			RobotMessage recd = gvh.getIncomingMessage(LogicThread.MSG_BARRIERSYNC);
			String bID = recd.getContents();
			// If the barrier ID has been encountered before, increment the count
			// of robots at the barrier. Otherwise, add a new barrier to barriers
			if(barriers.containsKey(bID)) {
				Integer currentCount = barriers.get(bID);
				barriers.put(bID, Integer.valueOf(currentCount + 1));
				Log.d(TAG, "Received barrier notice for bID " + bID + ". Current count: " + barriers.get(bID));
			} else {
				Log.d(TAG, "Received first barrier notice for bID " + bID);
				barriers.put(bID, new Integer(1));
			}
		}
		
		if(barriers.get(barrierID) == n_participants) {
			Log.i(TAG, "Barrier " + barrierID + " has all robots ready to proceed!");
			barriers.remove(barrierID);
			return true;
		}
		return false;
	}
}
