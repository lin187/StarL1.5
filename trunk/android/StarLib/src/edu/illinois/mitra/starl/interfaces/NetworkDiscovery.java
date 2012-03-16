package edu.illinois.mitra.starl.interfaces;

public interface NetworkDiscovery extends Cancellable {
	static String TAG = "Discovery";
	static String ERR = "Critical Error";
	
	public abstract void addListener(NetworkDiscoveryListener l);

	public abstract void removeListener(NetworkDiscoveryListener l);

	public abstract void start();
}