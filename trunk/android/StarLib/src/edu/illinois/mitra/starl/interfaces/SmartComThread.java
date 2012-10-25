package edu.illinois.mitra.starl.interfaces;

import edu.illinois.mitra.starl.comms.SmartCommsHandler;
import edu.illinois.mitra.starl.comms.UdpMessage;

/**
 * Low level communication thread which handles sending and receiving of packets. Received packets are passed to the associated SmartCommsHandler.
 * @author Adam Zimmerman
 * @version 1.0
 * @see SmartCommsHandler
 */
public interface SmartComThread extends Cancellable {

	/**
	 * Assign a SmartCommsHandler to handle all received packets. Any packets received before a SmartCommsHandler is associated will be discarded without warning. 
	 * @param sch the SmartCommsHandler handler to use
	 */
	abstract void setCommsHandler(SmartCommsHandler sch);
	
	/**
	 * Send a message
	 * @param msg the message to send
	 * @param IP the destination IP address
	 */
	abstract void write(UdpMessage msg, String IP);
}
