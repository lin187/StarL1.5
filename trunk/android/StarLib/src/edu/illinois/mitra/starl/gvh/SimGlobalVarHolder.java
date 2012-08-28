package edu.illinois.mitra.starl.gvh;

import java.util.HashMap;

import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.harness.IdealSimMotionAutomaton;
import edu.illinois.mitra.starl.harness.RealisticSimMotionAutomaton;
import edu.illinois.mitra.starl.harness.SimGpsReceiver;
import edu.illinois.mitra.starl.harness.SimSmartComThread;
import edu.illinois.mitra.starl.harness.SimulationEngine;
import edu.illinois.mitra.starl.objects.ItemPosition;

/**
 * Extension of the GlobalVarHolder class for use in simulations of StarL applications 
 * @author Adam Zimmerman
 * @version 1.0
 *
 */
public class SimGlobalVarHolder extends GlobalVarHolder {
	
	private SimulationEngine engine;
	
	/**
	 * @param name the name of this agent
	 * @param participants contains (name,IP) pairs for each participating agent 
	 * @param engine the main SimulationEngine
	 * @param initpos this agent's initial position
	 * @param traceDir the directory to write trace files to
	 */
	public SimGlobalVarHolder(String name, HashMap<String,String> participants, SimulationEngine engine, ItemPosition initpos, String traceDir, int trace_driftMax, float trace_skewBound) {
		super(name, participants);
		this.engine = engine;
		super.comms = new Comms(this, new SimSmartComThread(this, engine.comms));
		super.gps = new Gps(this, new SimGpsReceiver(this, engine.gps, initpos));
		super.log = new SimLogging(name,this);
		super.trace = new Trace(name, traceDir, this);
		super.plat = new AndroidPlatform();
		if(engine.gps instanceof IdealSimGpsProvider) {
			plat.moat = new IdealSimMotionAutomaton(this, (IdealSimGpsProvider)engine.gps);
		} else {
			plat.moat = new RealisticSimMotionAutomaton(this, engine.gps);
			plat.moat.start();
		}
	}

	@Override
	public void sleep(long time) {
		if(time <= 0) throw new RuntimeException("What are you doing?? You can't sleep for <= 0!");
		try {
			engine.threadSleep(time, Thread.currentThread());
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public long time() {
		return engine.getTime();
	}

	@Override
	public void threadCreated(Thread thread) {
		engine.registerThread(thread);
	}

	@Override
	public void threadDestroyed(Thread thread) {
		engine.removeThread(thread);
	}	
}