package edu.illinois.mitra.starl.functions;

import java.util.HashMap;
import java.util.HashSet;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.Common;

/**
 * The BarrierSynchronizer class is used to synchronize all agents to a known point. The synchronizer
 * will not provide "permission" to proceed past a certain point (a barrier) until all participating 
 * agents have signaled that they are at the barrier.
 * 
 * @author Adam Zimmerman
 * @version 1.1
 */
public class BarrierSynchronizer implements Synchronizer, MessageListener {
	private static final String TAG = "BarrierSynchronizer";
	private static final String ERR = "Critical Error";
	
	private GlobalVarHolder gvh;
	// Barriers tracks which barriers are active and how many robots have reported ready to proceed for each
	// Keys are barrier IDs, values are number of robots ready to proceed
	private HashMap<String,HashSet<String>> barriersNames;
	private int n_participants;
	private String name;
	
	/**
	 * Construct a new BarrierSynchronizer
	 * 
	 * @param gvh The main GlobalVarHolder
	 */
	public BarrierSynchronizer(GlobalVarHolder gvh) {
		this.gvh = gvh;
		n_participants = gvh.id.getParticipants().size();
		barriersNames = new HashMap<String,HashSet<String>>();
		name = gvh.id.getName();
		gvh.comms.addMsgListener(this, Common.MSG_BARRIERSYNC);
		gvh.trace.traceEvent(TAG, "Created", gvh.time());
		gvh.log.i(TAG, "Created BarrierSynchronizer, registered message listener with GVH");
	}
	
	@Override
	public void barrierSync(String barrierID) {	 
		HashSet<String> names;
		if(barriersNames.containsKey(barrierID)) {
			if(barriersNames.get(barrierID).contains(name)) {
				// Already requested entry!
				gvh.log.e(TAG, "TRIED TO SYNC MULTIPLE TIMES FOR BID " + barrierID);
				return;
			} else {
				names = barriersNames.get(barrierID);
			}
		} else {
			names = new HashSet<String>();
		}
		names.add(name);
		barriersNames.put(barrierID, names);
		
		gvh.log.e(TAG, "SENDING SYNC FOR ID " + barrierID);
		RobotMessage notify_sync = new RobotMessage("ALL", name, Common.MSG_BARRIERSYNC, new MessageContents(barrierID));
		gvh.comms.addOutgoingMessage(notify_sync);
	}
	
	@Override
	public boolean barrierProceed(String barrierID) {
		try {
			if(barriersNames.get(barrierID).size() == n_participants) {
				gvh.trace.traceEvent(TAG, "Barrier ready to proceed", barrierID, gvh.time());
				gvh.log.i(TAG, "Barrier " + barrierID + " has all robots ready to proceed!");
				barriersNames.remove(barrierID);
				return true;
			}
		} catch(NullPointerException e) {}
		return false;
	}

	@Override
	public void messageReceived(RobotMessage m) {
		// Update the barriers when a barrier sync message is received
		String bID = m.getContents(0);
		
		gvh.trace.traceEvent(TAG, "Received barrier sync message", bID, gvh.time());

		HashSet<String> names;
		if(barriersNames.containsKey(bID)) {
			names = barriersNames.get(bID);
			if(names.contains(m.getFrom())) {
				gvh.log.e(TAG, "Received duplicate sync message from " + m.getFrom() + "!");
				return;
			} else {
				names.add(m.getFrom());
			}
		} else {
			names = new HashSet<String>();
			names.add(m.getFrom());
		}
		barriersNames.put(bID, names);
		gvh.log.d(TAG, "Received barrier notice for bID " + bID + " from " + m.getFrom() + ". Current count: " + barriersNames.get(bID).size());
	}
	
	@Override
	public void cancel() {
		gvh.comms.removeMsgListener(Common.MSG_BARRIERSYNC);
		gvh.trace.traceEvent(TAG, "Cancelled", gvh.time());
	}
}
