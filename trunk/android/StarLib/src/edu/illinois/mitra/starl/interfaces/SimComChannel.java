package edu.illinois.mitra.starl.interfaces;

import edu.illinois.mitra.starl.harness.SimComThread;

public interface SimComChannel {

	public abstract void registerMsgReceiver(SimComThread hct, String IP);

	public abstract void removeMsgReceiver(SimComThread hct, String IP);

	public abstract void sendMsg(String from, String msg, String IP);

	public abstract void printStatistics();
	
}