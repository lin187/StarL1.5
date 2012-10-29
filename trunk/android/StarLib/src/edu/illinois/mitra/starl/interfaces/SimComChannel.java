package edu.illinois.mitra.starl.interfaces;

import edu.illinois.mitra.starl.harness.SimSmartComThread;

public interface SimComChannel {

	public abstract void registerMsgReceiver(SimSmartComThread hct, String IP);

	public abstract void removeMsgReceiver(String IP);

	/**
	 * Send a simulated message
	 * 
	 * @param from
	 *            the IP address of the sending robot
	 * @param msg
	 *            the contents of the message. The contents should be provided
	 *            as they would be to the socket connection in a non-simulated
	 *            robot.
	 * @param IP
	 *            the IP address the message will be sent to. This may be the
	 *            broadcast IP address or the IP address of a single robot
	 */
	public abstract void sendMsg(String from, String msg, String IP);

	public abstract String getStatistics();

}