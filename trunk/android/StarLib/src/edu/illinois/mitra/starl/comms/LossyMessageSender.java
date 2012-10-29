package edu.illinois.mitra.starl.comms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.objects.Common;

public class LossyMessageSender {	
	private Random rand;
	
	private GlobalVarHolder gvh;
	
	private Map<String, Double> sendRates = new HashMap<String, Double>();
	private Map<String, ArrayList<TimedLoss>> lossTimes = new HashMap<String, ArrayList<TimedLoss>>();

	public LossyMessageSender(GlobalVarHolder gvh) {
		this.gvh = gvh;
		for(String s : gvh.id.getParticipants()) {
			sendRates.put(s, 1.0);
			lossTimes.put(s, new ArrayList<TimedLoss>());
		}
		rand = new Random();
	}
	
	/**
	 * @param recipient The receiving robot which will not receive messages 
	 * @param startTime The time at which to begin lossy transmissions (in system milliseconds)
	 * @param duration The length of the lossy transmission period (in system milliseconds)
	 * @param lossPercentage The percentage of messages to drop during lossy transmission (0.0 to 1.0)
	 */
	public void newTemporalLoss(String recipient, long startTime, long duration, double lossPercentage) {
		if(!Common.inRange(lossPercentage, 0.0, 1.0))
			throw new IllegalArgumentException("Loss percentage must be between 0 and 1");
		if(duration <= 0.0)
			throw new IllegalArgumentException("Lossy transmissions have a positive nonzero duration");
		
		List<TimedLoss> tl = lossTimes.get(recipient);
		tl.add(new TimedLoss(startTime, duration, lossPercentage));
	}
	
	public void setStaticLossRate(String recipient, double lossPercentage) {
		if(!Common.inRange(lossPercentage, 0.0, 1.0))
			throw new IllegalArgumentException("Loss percentage must be between 0 and 1");
		
		sendRates.put(recipient, (1-lossPercentage));
	}
	
	public void send(RobotMessage rm) {
		
		if(rm.getTo().equals("ALL")) {
			RobotMessage single = new RobotMessage(rm);
			for(String s : gvh.id.getParticipants()) {
				single.setTo(s);
				send(single);
			}
		} else {
			boolean send = sendRates.get(rm.getTo()) > rand.nextDouble();
			if(send) {
				for(TimedLoss tl : lossTimes.get(rm.getTo())) {
					send &= tl.send(gvh.time());
				}
				if(send)
					gvh.comms.addOutgoingMessage(rm);
			}
		}
	}

	private class TimedLoss {
		private long start;
		private long stop;
		private double percentage;
		private Random rand;
		
		public TimedLoss(long start, long duration, double lossPercentage) {
			this.start = start;
			this.stop = start + duration;
			this.percentage = lossPercentage;
			rand = new Random();
		}
		
		private boolean applies(long now) {
			return (now > start || now < stop);
		}
				
		public boolean send(long now) {
			return !applies(now) || (1-percentage) > rand.nextDouble(); 
		}
	}
	
}
