package edu.illinois.mitra.starl.interfaces;

import java.util.Set;

public interface MutualExclusion {

	public abstract void start();
	
	public abstract void requestEntry(int id);

	public abstract void requestEntry(Set<Integer> ids);

	public abstract boolean clearToEnter(int id);

	public abstract boolean clearToEnter(Set<Integer> ids);

	public abstract void exit(int id);

	public abstract void exit(Set<Integer> ids);

	public abstract void exitAll();

	public abstract void cancel();

}