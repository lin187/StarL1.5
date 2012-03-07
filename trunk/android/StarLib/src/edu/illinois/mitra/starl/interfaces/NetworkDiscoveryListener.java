package edu.illinois.mitra.starl.interfaces;

public interface NetworkDiscoveryListener {
	public void neighborDiscoveredEvent(String name, String IP);
	public void neighborLostEvent(String name);
}
