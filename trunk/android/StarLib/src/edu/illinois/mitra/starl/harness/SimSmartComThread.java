package edu.illinois.mitra.starl.harness;

import edu.illinois.mitra.starl.comms.SmartCommsHandler;
import edu.illinois.mitra.starl.comms.UdpMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.interfaces.SmartComThread;

public class SimSmartComThread implements SmartComThread {

	private SimComChannel channel;
	private SmartCommsHandler sch;
	private GlobalVarHolder gvh;
	private String name;
	private String IP;
	
	public SimSmartComThread(GlobalVarHolder gvh, SimComChannel channel) {
		this.channel = channel;
		this.gvh = gvh;
		name = gvh.id.getName();
		IP = gvh.id.getParticipantsIPs().get(name);
		channel.registerMsgReceiver(this, IP);
	}
	
	@Override
	public void cancel() {
		channel.removeMsgReceiver(IP);
	}

	@Override
	public synchronized void write(UdpMessage msg, String toIP) {
		channel.sendMsg(IP, msg.toString(), toIP);
	}
	
	public void receive(String msg) {
		sch.handleReceived(new UdpMessage(msg, gvh.time()));
	}

	@Override
	public void setCommsHandler(SmartCommsHandler sch) {
		this.sch = sch;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimSmartComThread other = (SimSmartComThread) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
