package edu.illinois.mitra.starl.interfaces;


public interface LeaderElection {

	public abstract String elect();

	public abstract void cancel();
}
