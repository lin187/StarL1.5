package edu.illinois.mitra.starlSim.main;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.SimGlobalVarHolder;
import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.objects.ItemPosition;

public abstract class SimApp implements Callable<List<String>> {
	protected String name;
	protected GlobalVarHolder gvh;
	protected IdealSimGpsProvider gps;
	
	int initx = 0;
	int inity = 0;
	int inita = 0;
	
	protected String[] results;
	
	public SimApp(String name, HashMap<String,String> participants, SimComChannel channel, IdealSimGpsProvider gps, ItemPosition initpos, String traceDir) {
		this.name = name;
		this.gps = gps;
		gvh = new SimGlobalVarHolder(name, participants, channel, gps, initpos, traceDir);
		gvh.comms.startComms();
		gvh.gps.startGps();
		
		results = new String[0];
	}

	public String getLog() {
		return gvh.log.getLog();
	}

}