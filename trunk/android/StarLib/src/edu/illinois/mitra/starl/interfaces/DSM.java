package edu.illinois.mitra.starl.interfaces;

import java.util.List;
import edu.illinois.mitra.starl.objects.*;

public interface DSM extends Cancellable{
	public abstract void start();
	public abstract void stop();
	public abstract void reset();
	public abstract List<DSMVariable> getAll(String name, String owner);
	public abstract DSMVariable get_V(String name, String owner);
	public abstract String get(String name, String owner);
	public abstract String get(String name, String owner, String attr);
	public abstract boolean put(DSMVariable tuple);
	public abstract boolean putAll(List<DSMVariable> tuples);
	public abstract boolean put(String name, String owner, int value);
	public abstract boolean put(String name, String owner, String attr, int value);
	public abstract boolean put(String name, String owner, String ... attr_and_value);
	public abstract boolean createMW(String name, int value);	
	public abstract boolean createMW(String name, String ... attr_and_value);
}