package edu.illinois.mitra.starl.gvh;

import java.util.HashMap;

import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.harness.IdealSimMotionAutomaton;
import edu.illinois.mitra.starl.harness.SimComThread;
import edu.illinois.mitra.starl.harness.SimGpsReceiver;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class SimGlobalVarHolder extends GlobalVarHolder {
	
	public SimGlobalVarHolder(String name, HashMap<String,String> participants, SimComChannel sct, IdealSimGpsProvider gpsp, ItemPosition initpos, String traceDir) {
		super(name, participants);
		super.comms = new Comms(this, new SimComThread(this, sct));
		super.gps = new Gps(this, new SimGpsReceiver(this, gpsp, initpos));
		super.log = new SimLogging(name);
		super.trace = new Trace(name, traceDir);
		super.plat = new AndroidPlatform();
		plat.moat = new IdealSimMotionAutomaton(this, gpsp);
	}	
}