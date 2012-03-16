package edu.illinois.mitra.starl.interfaces;


public interface LeaderElection extends Cancellable {

	public abstract String elect();

	public abstract void cancel();
}
