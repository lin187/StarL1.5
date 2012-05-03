package edu.illinois.mitra.starl.interfaces;


public interface LeaderElection extends Cancellable {
	
	public abstract String getLeader();
	
	public abstract void elect();

	public abstract void cancel();
}
