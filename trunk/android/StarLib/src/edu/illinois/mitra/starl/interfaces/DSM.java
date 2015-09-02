package edu.illinois.mitra.starl.interfaces;

import java.util.List;
import edu.illinois.mitra.starl.objects.*;

public interface DSM extends Cancellable{
	public abstract void start();
	public abstract void stop();
	public abstract void reset();
	public abstract List<DSMVariable> getAll(String name, String owner);
	public abstract DSMVariable get(String name, String owner);
	public abstract Object get(String name, String owner, String attr);
	public abstract boolean put(DSMVariable tuple);
	public abstract boolean putAll(List<DSMVariable> tuples);
	public abstract boolean put(String name, String owner, int value);
	public abstract boolean put(String name, String owner, String attr, int value);
}