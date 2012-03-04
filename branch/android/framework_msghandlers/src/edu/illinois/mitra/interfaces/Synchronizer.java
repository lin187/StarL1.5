package edu.illinois.mitra.interfaces;

public interface Synchronizer {

	public abstract void barrier_sync(String barrierID);

	public abstract boolean barrier_proceed(String barrierID);

	public abstract void cancel();

}