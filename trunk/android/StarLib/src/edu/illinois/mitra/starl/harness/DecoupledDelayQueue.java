package edu.illinois.mitra.starl.harness;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DecoupledDelayQueue<E extends Delayed> extends DelayQueue<E> {
	public long getMinimumDelay(TimeUnit t) {
		try {
			return peek().getDelay(t);
		} catch(NullPointerException e) {
			return Long.MAX_VALUE;
		}
	}
	
	public Collection<E> pollAll() {
		Collection<E> retval = new LinkedList<E>();
		E rem = poll();
		while(rem != null) {
			retval.add(rem);
			rem = poll();
		}
		return retval;
	}

}