package edu.illinois.mitra.starl.harness;

import java.util.ArrayList;

import edu.illinois.mitra.starl.comms.UdpMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.ComThread;
import edu.illinois.mitra.starl.interfaces.SimComChannel;

public class SimComThread implements ComThread {
	private ArrayList<UdpMessage> receivedList;
	private SimComChannel channel;
	private String name;
	private String IP;
	private GlobalVarHolder gvh;
	
	public SimComThread(GlobalVarHolder gvh, SimComChannel channel) {
		this.channel = channel;
		this.gvh = gvh;
		name = gvh.id.getName();
		IP = gvh.id.getParticipantsIPs().get(name);
		// TODO: Was channel.registerMsgReceiver(this, IP);
	}
	
	@Override
	public void write(UdpMessage msg, String toIP) {
		channel.sendMsg(IP, msg.toString(), toIP);
	}
	
	public void receive(String msg) {
		receivedList.add(new UdpMessage(msg, gvh.time()));
	}

	
	@Override
	public void cancel() {
		// Doesn't do anything!
	}
	
	@Override
	public void start() {
		// Doesn't do anything because messages are received externally!
	}

	@Override
	public void setMsgList(ArrayList<UdpMessage> ReceivedMessageList) {
		receivedList = ReceivedMessageList;
	}
}
