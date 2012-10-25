package edu.illinois.mitra.starl.harness;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import edu.illinois.mitra.starl.interfaces.SimComChannel;

public class DecoupledSimComChannel implements SimComChannel {
	private static final String BROADCAST_IP = "192.168.1.255";

	private Map<String, SimSmartComThread> receivers;

	private Set<DeliveryEvent> msgs = new HashSet<DeliveryEvent>();
	private Set<DeliveryEvent> toAdd = new HashSet<DeliveryEvent>();

	// Message loss and delay stats
	private int dropRate;
	private int meanDelay;
	private int delayStdDev;

	// Message statistics
	private int stat_totalMessages = 0;
	private int stat_bcastMessages = 0;
	private int stat_lostMessages = 0;
	private int stat_overallDelay = 0;

	private Random rand;

	// for message blocking
	private Map<String, String> ipToNameMap = new HashMap<String, String>();
	private Set<String> blockedRobot;

	// Drop rate is per 100 messages
	public DecoupledSimComChannel(int meanDelay, int delayStdDev, int dropRate, int seed, Set<String> blockedRobot, Map<String, String> nameToIpMap) {
		this.meanDelay = meanDelay;
		this.dropRate = (100 - dropRate);
		this.delayStdDev = delayStdDev;
		this.blockedRobot = blockedRobot;

		receivers = new HashMap<String, SimSmartComThread>();
		rand = new Random(seed);

		// We require a map from IP -> Name, are given a map from Name -> IP
		for(Entry<String, String> participant : nameToIpMap.entrySet())
			ipToNameMap.put(participant.getValue(), participant.getKey());
	}

	public void registerMsgReceiver(SimSmartComThread hct, String IP) {
		receivers.put(IP, hct);
	}

	public void removeMsgReceiver(String IP) {
		receivers.remove(IP);
	}

	private synchronized void addInTransit(String msg, String IP, boolean forceDrop) {
		stat_totalMessages++;

		if(forceDrop)
			stat_lostMessages++;
		else {
			if(meanDelay > 0) {
				if(rand.nextInt(100) < dropRate) {
					int delay = Math.max(1, (rand.nextInt(2 * delayStdDev + 1) - delayStdDev) + meanDelay);
					toAdd.add(new DeliveryEvent(msg, delay, receivers.get(IP)));
					stat_overallDelay += delay;
				} else {
					stat_lostMessages++;
				}
			} else if(receivers.containsKey(IP)) {
				receivers.get(IP).receive(msg);
			}
		}
	}

	public void sendMsg(String from, String msg, String IP) {
		boolean forceDrop = shouldForceDrop(from, IP);

		if(IP.equals(BROADCAST_IP)) {
			stat_bcastMessages++;
			for(String ip : receivers.keySet()) {
				if(!ip.equals(from)) {
					addInTransit(msg, ip, forceDrop);
				}
			}
		} else if(receivers.containsKey(IP)) {
			addInTransit(msg, IP, forceDrop);
		}
	}

	private boolean shouldForceDrop(String fromIp, String toIp) {
		String from = ipToNameMap.get(fromIp);
		String to = ipToNameMap.get(toIp);
		return blockedRobot.contains(from) || blockedRobot.contains(to);
	}

	// Must not advance by more than the minimum delay!!
	public void advanceTime(long advance) {
		Set<DeliveryEvent> toRemove = new HashSet<DeliveryEvent>();
		for(DeliveryEvent de : msgs) {
			de.delay -= advance;
			// If this message's delay has expired, deliver it and flag it for removal
			if(de.delay == 0) {
				de.deliver();
				toRemove.add(de);
			} else if(de.delay < 0) {
				throw new RuntimeException("ENCOUNTERED A NEGATIVE MESSAGE DELAY! TIME ADVANCED BY MORE THAN MINIMUM DELAY!");
			}
		}
		// Remove all delivered messages
		msgs.removeAll(toRemove);
	}

	public long minDelay() {
		Long min = Long.MAX_VALUE;
		int retries = 0;
		while(!toAdd.isEmpty() && retries < 5) {
			try {
				synchronized(msgs) {
					msgs.addAll(toAdd);
					toAdd.clear();
				}
			} catch(ConcurrentModificationException e) {
				e.printStackTrace(System.out);
				System.out.println("\n\tOh snap! Don't worry, everything is probably ok.");
				retries++;
			}
		}
		for(DeliveryEvent de : msgs) {
			min = Math.min(min, de.delay);
		}

		return min;
	}

	private class DeliveryEvent {
		private SimSmartComThread recipientThread;
		private String msg;
		public long delay = 0;

		public DeliveryEvent(String msg, long delay, SimSmartComThread recipientThread) {
			this.delay = delay;
			this.msg = msg;
			this.recipientThread = recipientThread;
		}

		public void deliver() {
			recipientThread.receive(msg);
		}

		private DecoupledSimComChannel getOuterType() {
			return DecoupledSimComChannel.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((msg == null) ? 0 : msg.hashCode());
			result = prime * result + ((recipientThread == null) ? 0 : recipientThread.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			DeliveryEvent other = (DeliveryEvent) obj;
			if(!getOuterType().equals(other.getOuterType()))
				return false;
			if(msg == null) {
				if(other.msg != null)
					return false;
			} else if(!msg.equals(other.msg))
				return false;
			if(recipientThread == null) {
				if(other.recipientThread != null)
					return false;
			} else if(!recipientThread.equals(other.recipientThread))
				return false;
			return true;
		}
	}

	public String getStatistics() {
		StringBuilder sb = new StringBuilder();
		if(stat_totalMessages > 0) {
			sb.append("Total messages: ").append(stat_totalMessages).append('\n');
			sb.append("Broadcast messages: ").append(stat_bcastMessages).append('\n');
			sb.append("Dropped messages: ").append(stat_lostMessages).append(" = ").append(100 * (float) stat_lostMessages / stat_totalMessages + "%").append('\n');
			sb.append("Average delay: ").append((float) stat_overallDelay / stat_totalMessages + " ms\n");
			return sb.toString();
		} else {
			return "No messages were sent.";
		}
	}
}
