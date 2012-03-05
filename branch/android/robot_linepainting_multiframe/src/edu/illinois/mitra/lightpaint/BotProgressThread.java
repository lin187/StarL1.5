package edu.illinois.mitra.lightpaint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.util.Log;
import edu.illinois.mitra.lightpaint.main.LogicThread;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.globalVarHolder;

public class BotProgressThread implements MessageListener {
	private static final String TAG = "ProgressTrack";
	private static final String ERR = "Critical Error";
	
	private String[] names;
	private HashMap<String,BotProgress> progress;
	private ArrayList<String> completed;
	private String name;
	private int num_bots;
	private globalVarHolder gvh;
	
	private static final int ROBOT_TIMEOUT = 7500; // Milliseconds
	
	public BotProgressThread(globalVarHolder gvh) {
		this.gvh = gvh;

		names = gvh.getParticipants().toArray(new String[0]);
		
		completed = new ArrayList<String>();
		progress = new HashMap<String, BotProgress>();
		num_bots = names.length;
		
		// Create a HashMap of progress trackers
		for(int i = 0; i < num_bots; i++) {
			if(!names[i].equals(name)) {
				progress.put(names[i], new BotProgress());
			}
		}
		
		Log.i(TAG, "Starting progress tracker!");
		gvh.addMsgListener(LogicThread.MSG_LINEPROGRESS, this);
	}
	
	// Returns a string for the debug window indicating who has expired
	public String getDebug() {
		String retval = "";
		long time = System.currentTimeMillis();
		for(int i = 0; i < num_bots; i++) {
			if(!names[i].equals(gvh.getName())) {
				if(progress.containsKey(names[i])) {
					BotProgress tracker = progress.get(names[i]);
					retval = retval + names[i] + "  " + tracker.getLastLine() + "-" + tracker.getLastSegment() + " : " + (time - tracker.getLastReportTime()) + " ms";
					if(isExpired(names[i])) {
						retval += " EXPIRED!";
					}
				} else {
					retval += names[i] + " DONE!";
				}
				retval += "\n";
			}
		}
		return retval;
	}

	// Indicate whether a particular robot has expired
	public synchronized boolean isExpired(String robot) {
		if(progress.containsKey(robot)) {
			long elapsed_time = (System.currentTimeMillis() - progress.get(robot).getLastReportTime());
			return (elapsed_time > ROBOT_TIMEOUT);
		}
		return false;
	}
	
	// Return the last reported line and segment for a robot
	public synchronized int[] getLastReport(String robot) {
		int [] retval = {-1,-1};
		if(progress.containsKey(robot)) {
			retval[0] = progress.get(robot).getLastLine();
			retval[1] = progress.get(robot).getLastSegment();
		}
		return retval;
	}
	
	public void updateMyProgress(int line, int seg) {
		RobotMessage update = new RobotMessage("ALL", gvh.getName(), LogicThread.MSG_LINEPROGRESS, line + "-" + seg);
		gvh.addOutgoingMessage(update);
	}
	
	public void sendDone() {
		RobotMessage update = new RobotMessage("ALL", gvh.getName(), LogicThread.MSG_LINEPROGRESS, "DONE");
		gvh.addOutgoingMessage(update);
	}
	
	public void cancel() {
		Log.d(TAG, "CANCELLING PROGRESS TRACKER");
		gvh.removeMsgListener(LogicThread.MSG_LINEPROGRESS);
	}

	public void updateMyProgress(int[] curPos) {
		updateMyProgress(curPos[0], curPos[1]);
	}

	public void messageReceied(RobotMessage m) {
		// Receive line progress update messages
		String from = m.getFrom();
		
		// If the next message indicates that the robot is done, remove it from the progress tracking map
		if(m.getContents().equals("DONE")) {
			progress.remove(from);
			completed.add(from);
		// Otherwise, if we have a tracker for this robot, update it
		} else if(progress.containsKey(from)) {
			progress.get(from).update(m);
		} else if(!completed.contains(from)) {
			Log.e(ERR, "Don't have a progress tracker for robot with name " + from);
		}
	}
}