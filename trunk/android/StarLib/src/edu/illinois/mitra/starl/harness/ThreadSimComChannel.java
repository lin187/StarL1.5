package edu.illinois.mitra.starl.harness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import edu.illinois.mitra.starl.interfaces.SimComChannel;

public class ThreadSimComChannel extends Thread implements SimComChannel {
	private static final String BROADCAST_IP = "192.168.1.255";
	private boolean D = false;
	
	private HashMap<String,SimComThread> receivers;
	private HashSet<SimMsgInTransit> intransit;
	private HashSet<SimMsgInTransit> toadd;
	
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
	public ThreadSimComChannel(int meanDelay, int delayStdDev, int dropRate, boolean debug) {
		this.meanDelay = meanDelay;
		this.dropRate = (100-dropRate);
		this.delayStdDev = delayStdDev;
		this.D = debug;
		receivers = new HashMap<String,SimComThread>();
		intransit = new HashSet<SimMsgInTransit>();
		toadd = new HashSet<SimMsgInTransit>();
		rand = new Random(System.currentTimeMillis());
		this.start();
	}
	
	public synchronized void registerMsgReceiver(SimComThread hct, String IP) {
		receivers.put(IP, hct);
	}
	
	private synchronized void addInTransit(String msg, String IP) {
		stat_totalMessages ++;
		if(meanDelay > 0) {
			synchronized(toadd) {
				if(rand.nextInt(100) < dropRate) {
					int delay = Math.abs((rand.nextInt(2*delayStdDev)-delayStdDev)+meanDelay);
					stat_overallDelay += delay;
					toadd.add(new SimMsgInTransit(msg,IP,delay));
					if(D) System.out.println("Sending " + msg + " to " + IP + " with delay " + delay + " ms");
				} else {
					stat_lostMessages ++;
				}
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
	
	@Override
	public void run() {
		HashSet<SimMsgInTransit> toremove = new HashSet<SimMsgInTransit>();
		super.run();
		while(true) {
			synchronized(toadd) {
				intransit.addAll(toadd);
				toadd.clear();
			}
			long time = System.currentTimeMillis();
			for(SimMsgInTransit m : intransit) {
				if(m.getDeliverTime() <= time) {
					if(receivers.containsKey(m.getDest())) {
						receivers.get(m.getDest()).receive(m.getMsg());
						toremove.add(m);
						stat_actual_delivers ++;
						int curdelay = (int)(time - m.getCreateTime());
						stat_actual_delaytotal += curdelay;
						if(curdelay > stat_actual_maxdelay) {
							stat_actual_maxdelay = curdelay;
						}
						if(curdelay < stat_actual_mindelay) {
							stat_actual_mindelay = curdelay;
						}
						if(D) System.out.println("Delivered " + m.getMsg() + " AGE: " + curdelay  + " ms");
					}
				}
			}
			intransit.removeAll(toremove);
			toremove.clear();
		}
	}
	
	public void printStatistics() {
		System.out.println("Total messages: " + stat_totalMessages);
		System.out.println("Broadcast messages: " + stat_bcastMessages);
		System.out.println("Dropped messages: " + stat_lostMessages + " = " + 100*(float)stat_lostMessages/stat_totalMessages + "%");
		System.out.println("Average delay: " + (float)stat_overallDelay/stat_totalMessages + " ms");
		
		System.out.println("\nActual delivers: " + stat_actual_delivers);
		System.out.println("Actual average delay: " + (int)(stat_actual_delaytotal/stat_actual_delivers) + " ms");
		System.out.println("Actual max delay: " + stat_actual_maxdelay + " ms");
		System.out.println("Actual min delay: " + stat_actual_mindelay + " ms");
	}

	@Override
	public void removeMsgReceiver(SimComThread hct, String IP) {
		// TODO Auto-generated method stub
		
	}
}
