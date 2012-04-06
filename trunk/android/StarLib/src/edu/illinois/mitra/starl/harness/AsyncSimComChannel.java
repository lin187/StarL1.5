package edu.illinois.mitra.starl.harness;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.illinois.mitra.starl.interfaces.SimComChannel;


public class AsyncSimComChannel implements SimComChannel {
	private static final String BROADCAST_IP = "192.168.1.255";
	private static final int POOL_SIZE = 1;
	
	private ConcurrentHashMap<String, SimComThread> receivers;
	
	// Executor
	private ScheduledThreadPoolExecutor exec;
	
	// Message loss and delay stats
	private int dropRate;
	private int meanDelay;
	private int delayStdDev;
	
	// Message statistics
	private int stat_totalMessages = 0;
	private int stat_bcastMessages = 0;
	private int stat_lostMessages = 0;
	private int stat_overallDelay = 0;
	private int stat_actual_delivers = 0;
	private int stat_actual_delaytotal = 0;
	private int stat_actual_maxdelay = Integer.MIN_VALUE;
	private int stat_actual_mindelay = Integer.MAX_VALUE;
	
	private Random rand;
	
	// Drop rate is per 100 messages
	public AsyncSimComChannel(int meanDelay, int delayStdDev, int dropRate) {
		this.meanDelay = meanDelay;
		this.dropRate = (100-dropRate);
		this.delayStdDev = delayStdDev;
		receivers = new ConcurrentHashMap<String,SimComThread>();
		rand = new Random(System.currentTimeMillis());
		exec = new ScheduledThreadPoolExecutor(POOL_SIZE);
	}

	public synchronized void registerMsgReceiver(SimComThread hct, String IP) {
		receivers.put(IP, hct);
	}

	public synchronized void removeMsgReceiver(SimComThread hct, String IP) {
		receivers.remove(IP);
	}
	
	private synchronized void addInTransit(String msg, String IP) {
		stat_totalMessages ++;
		if(meanDelay > 0) {
			if(rand.nextInt(100) < dropRate) {
				int delay = Math.abs((rand.nextInt(2*delayStdDev+1)-delayStdDev)+meanDelay);
				stat_actual_delivers ++;
				exec.schedule(new DeliveryEvent(msg,IP), delay, TimeUnit.MILLISECONDS);
				stat_overallDelay += delay;
			} else {
				stat_lostMessages ++;
			}
		} else if(receivers.containsKey(IP)){
			receivers.get(IP).receive(msg);
		}
	}
	
	public synchronized void sendMsg(String from, String msg, String IP) {
		if(IP.equals(BROADCAST_IP)) {
			stat_bcastMessages ++;
			for(String ip : receivers.keySet()) {
				if(!ip.equals(from)) {
					addInTransit(msg, ip);
				}
			}
		} else if(receivers.containsKey(IP)) {
			addInTransit(msg,IP);
		}
	}
	
	private class DeliveryEvent implements Runnable {
		private String IP;
		private String msg;
		private long createTime = 0;

		public DeliveryEvent(String msg, String iP) {
			this.IP = iP;
			this.msg = msg;
			this.createTime = System.currentTimeMillis();
		}

		public void run() {
			long myDelay = (System.currentTimeMillis() - createTime);
			stat_actual_delaytotal += myDelay;
			stat_actual_maxdelay = (int) Math.max(myDelay, stat_actual_maxdelay);
			stat_actual_mindelay = (int) Math.min(myDelay, stat_actual_mindelay);
			receivers.get(IP).receive(msg);
		}	
	}
	
	public void printStatistics() {
		if(stat_totalMessages > 0) {
			System.out.println("Total messages: " + stat_totalMessages);
			System.out.println("Broadcast messages: " + stat_bcastMessages);
			System.out.println("Dropped messages: " + stat_lostMessages + " = " + 100*(float)stat_lostMessages/stat_totalMessages + "%");
			System.out.println("Average requested delay: " + (float)stat_overallDelay/stat_totalMessages + " ms");
			
			System.out.println("\nActual stats");
			System.out.println("Actual delivers: " + stat_actual_delivers);
			System.out.println("Actual average delay: " + (int)(stat_actual_delaytotal/stat_actual_delivers) + " ms");
			System.out.println("Actual max delay: " + stat_actual_maxdelay + " ms");
			System.out.println("Actual min delay: " + stat_actual_mindelay + " ms");
		} else {
			System.out.println("No messages were sent.");
		}
	}
}
