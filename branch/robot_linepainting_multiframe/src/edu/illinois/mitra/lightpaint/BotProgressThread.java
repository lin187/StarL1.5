package edu.illinois.mitra.lightpaint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.util.Log;

import edu.illinois.mitra.LogicThread;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.comms.RobotMessage;

public class BotProgressThread extends Thread {
	private static final String TAG = "ProgressTrack";
	private static final String ERR = "Critical Error";
	
	private String[] names;
	private HashMap<String,BotProgress> progress;
	private ArrayList<String> completed;
	private String name;
	private int num_bots;
	private globalVarHolder gvh;
	
	private boolean running = false;
	
	private static final int ROBOT_TIMEOUT = 7500; // Milliseconds
	
	private static final int GUI_UPDATE_TIMER = 250;
	private long lastUpdate = 0;
	
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
	}
	
	public synchronized void start() {
		Log.i(TAG, "Starting progress tracker!");
		running = true;
		super.start();
	}

	public void run() {
		super.run();
		int n_received_msgs = 0;
		while(running) {
			// Receive line progress update messages
			n_received_msgs = gvh.getIncomingMessageCount(LogicThread.MSG_LINEPROGRESS);
			Iterator<RobotMessage> iter = gvh.getIncomingMessages(LogicThread.MSG_LINEPROGRESS).iterator();
			for(int i = 0; i < n_received_msgs; i++) {
				//RobotMessage rec = gvh.getIncomingMessage(LogicThread.MSG_LINEPROGRESS);
				RobotMessage rec = iter.next();
				String from = rec.getFrom();
				
				// If the next message indicates that the robot is done, remove it from the progress tracking map
				if(rec.getContents().equals("DONE")) {
					progress.remove(from);
					completed.add(from);
				// Otherwise, if we have a tracker for this robot, update it
				} else if(progress.containsKey(from)) {
					progress.get(from).update(rec);
				} else if(!completed.contains(from)) {
					Log.e(ERR, "Don't have a progress tracker for robot with name " + from);
				}
			}
			
			// TODO: ERASE THIS TEMPORARY DEBUG STUFF:
			// Update the GUI
//			if((System.currentTimeMillis() - lastUpdate) > GUI_UPDATE_TIMER) {
//				gvh.setDebugInfo(getDebug());
//				lastUpdate = System.currentTimeMillis();
//			}
		}
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
		Log.d(TAG, "CANCELLING PROGRESS THREAD");
		running = false;
	}

	public void updateMyProgress(int[] curPos) {
		updateMyProgress(curPos[0], curPos[1]);
	}
}