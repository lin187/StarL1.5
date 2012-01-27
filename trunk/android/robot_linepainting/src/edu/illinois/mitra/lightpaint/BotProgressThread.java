package edu.illinois.mitra.lightpaint;

import java.util.ArrayList;
import java.util.HashMap;

import edu.illinois.mitra.LogicThread;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.comms.RobotMessage;

public class BotProgressThread extends Thread {

	private HashMap<String,BotProgress> progress;
	private ArrayList<String> completed;
	private String name;
	private int num_bots;
	private globalVarHolder gvh;
	
	private boolean running = false;
	
	private static final int ROBOT_TIMEOUT = 5000; // Milliseconds
	
	public BotProgressThread(globalVarHolder gvh) {
		this.gvh = gvh;
		String [] names = (String[]) gvh.getParticipants().toArray();
		completed = new ArrayList<String>();
		progress = new HashMap<String, BotProgress>();
		num_bots = names.length - 1;
		
		// Create a HashMap of progress trackers
		for(int i = 0; i < num_bots + 1; i++) {
			if(!names[i].equals(name)) {
				progress.put(names[i], new BotProgress());
			}
		}
	}
	
	public synchronized void start() {
		running = true;
		super.start();
	}

	public void run() {
		super.run();
		int n_received_msgs = 0;
		while(running) {
			n_received_msgs = gvh.getIncomingMessageCount(LogicThread.MSG_LINEPROGRESS);
			for(int i = 0; i < n_received_msgs; i++) {
				RobotMessage rec = gvh.getIncomingMessage(LogicThread.MSG_LINEPROGRESS);
				String from = rec.getFrom();
				
				if(rec.getContents().equals("DONE")) {
					progress.remove(from);
					completed.add(from);
				}
				
				if(progress.containsKey(from)) {
					progress.get(from).update(rec);
				}
			}
			// TODO: Insert a pause??
		}
	}	

	// Indicate whether a particular robot has expired
	public synchronized boolean isExpired(String robot) {
		if(progress.containsKey(robot)) {
			long elapsed_time = (System.currentTimeMillis() - progress.get(robot).getLastReportTime());
			return (elapsed_time > ROBOT_TIMEOUT);
		}
		return false;
	}
	
	// Return the last reported line and segment
	public synchronized int[] getLastLine(String robot) {
		int [] retval = {-1,-1};
		if(progress.containsKey(robot)) {
			retval[0] = progress.get(robot).getLastLine();
			retval[1] = progress.get(robot).getLastSegment();
		}
		return retval;
	}
	
	public void cancel() {
		running = false;
	}
}