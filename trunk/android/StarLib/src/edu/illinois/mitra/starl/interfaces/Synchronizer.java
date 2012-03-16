package edu.illinois.mitra.starl.interfaces;

public interface Synchronizer extends Cancellable {
	
	public abstract void barrier_sync(String barrierID);

	public abstract boolean barrier_proceed(String barrierID);
}