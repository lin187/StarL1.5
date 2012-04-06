package edu.illinois.mitra.starl.gvh;

import java.util.HashMap;
import java.util.HashSet;

import edu.illinois.mitra.starl.interfaces.RobotEventListener;

public abstract class GlobalVarHolder {

	public Comms comms;
	public Gps gps;
	public Id id;
	public Logging log;
	public Trace trace;
	public AndroidPlatform plat;
	
	public GlobalVarHolder(String name, HashMap<String,String> participants) {
		id = new Id(name, participants);
	}
	
	// Events
	private HashSet<RobotEventListener> eventListeners = new HashSet<RobotEventListener>();
	
	// Handle event providers and listeners 
	public void addEventListener(RobotEventListener el) {
		eventListeners.add(el);
	}
	public void removeEventListener(RobotEventListener el) {
		eventListeners.remove(el);
	}
	public void sendRobotEvent(int type, int event) {
		for(RobotEventListener el : eventListeners) {
			el.robotEvent(type, event);
		}
	}
	public void sendRobotEvent(int type) {
		sendRobotEvent(type, -1);
	}
}