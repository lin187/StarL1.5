package edu.illinois.mitra.starl.interfaces;

import java.util.ArrayList;

import edu.illinois.mitra.starl.comms.UdpMessage;

public interface ComThread extends Cancellable {

	abstract void setMsgList(ArrayList<UdpMessage> ReceivedMessageList);
	
	abstract void write(UdpMessage msg, String IP);

	abstract void start();
	
}
