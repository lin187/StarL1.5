package edu.illinois.mitra.starl.interfaces;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;

/**
 * The base class for all StarL application main classes.
 * 
 * @author Adam Zimmerman
 * @version 1.0
 */
public abstract class LogicThread extends StarLCallable implements Cancellable, AcceptsPointInput, MessageListener {

	protected String name;

	public LogicThread(GlobalVarHolder gvh) {
		super(gvh, "LogicThread");
		this.name = gvh.id.getName();
	}

	@Override
	public void receivedPointInput(int x, int y) {
		// used if you want your code to respond to point-input from the user,
		// in simulation this is done with right clicks
	}

	private final Queue<RobotMessage> msgs = new ConcurrentLinkedQueue<RobotMessage>();

	@Override
	public final void messageReceived(RobotMessage m) {
		if(isSleeping)
			receive(m);
		else
            msgs.add(m);

	}

	private volatile boolean isSleeping = false;

	protected final void sleep(int ms) {
		int toDeliver = msgs.size();
		for(int i = 0; i < toDeliver; i++) {
            receive(msgs.poll());
        }
		isSleeping = true;
		gvh.sleep(ms);
		isSleeping = false;
	}

	protected void receive(RobotMessage m) {
		// Override to receive messages
	}

	@Override
	public void cancel() {
	}

}
