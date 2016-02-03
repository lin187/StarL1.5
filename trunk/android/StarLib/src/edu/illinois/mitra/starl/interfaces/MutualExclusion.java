package edu.illinois.mitra.starl.interfaces;

import java.util.List;
import java.util.Set;

public interface MutualExclusion extends Cancellable {
	
	public abstract void setGroup(List<String> party);
	
	public abstract void requestEntry(int id);

	public abstract void requestEntry(Set<Integer> ids);

	public abstract boolean clearToEnter(int id);

	public abstract boolean clearToEnter(Set<Integer> ids);

	public abstract void exit(int id);

	public abstract void exit(Set<Integer> ids);

	public abstract void exitAll();
}