package edu.illinois.mitra.starl.interfaces;

import edu.illinois.mitra.starl.harness.SimSmartComThread;

public interface SimComChannel {

	public abstract void registerMsgReceiver(SimSmartComThread hct, String IP); // TODO: Was SimComThread

	public abstract void removeMsgReceiver(String IP); // TODO: Was SimComThread

	public abstract void sendMsg(String from, String msg, String IP);

	public abstract void printStatistics();
	
}