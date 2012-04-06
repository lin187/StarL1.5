package edu.illinois.mitra.starl.interfaces;

import java.util.ArrayList;

import edu.illinois.mitra.starl.comms.UDPMessage;

public interface ComThread extends Cancellable {

	abstract void setMsgList(ArrayList<UDPMessage> ReceivedMessageList);
	
	abstract void write(UDPMessage msg, String IP);

	abstract void start();
	
}
