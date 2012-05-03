package edu.illinois.mitra.starl.harness;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;

public class DelayedRunnable implements Delayed, Runnable {

	private GlobalVarHolder gvh;
	private Runnable run;
	
	private long origin;
	private long delay;
	
	public DelayedRunnable(GlobalVarHolder gvh, long delay, Runnable run) {
		this.gvh = gvh;
		this.run = run;
		this.delay = delay;
		this.origin = gvh.time();
	}

	@Override
	public int compareTo(Delayed delayed) {
		if (delayed == this) {
			return 0;
		}

		if (delayed instanceof DelayedRunnable) {
			long diff = delay - ((DelayedRunnable) delayed).delay;
			return ((diff == 0) ? 0 : ((diff < 0) ? -1 : 1));
		}

		long d = (getDelay(TimeUnit.MILLISECONDS) - delayed
				.getDelay(TimeUnit.MILLISECONDS));
		return ((d == 0) ? 0 : ((d < 0) ? -1 : 1));
	}

	@Override
	public void run() {
		run.run();
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(delay - (gvh.time() - origin), TimeUnit.MILLISECONDS);
	}
}
