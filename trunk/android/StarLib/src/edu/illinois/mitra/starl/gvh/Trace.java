package edu.illinois.mitra.starl.gvh;

import edu.illinois.mitra.starl.objects.TraceWriter;

/**
 * A thin wrapper for the TraceWriter class. Instantiated by the GlobalVarHolder.
 * 
 * @author Adam Zimmerman
 * @version 1.0
 *
 */
public class Trace {
	private String name;
	private TraceWriter trace;
	private String tracedir;
	private GlobalVarHolder gvh;
	
	private int driftMax = 0;
	private float skewBound = 0.0f;
	
	public Trace(String name, String tracedir, GlobalVarHolder gvh) {
		this.name = name;
		this.tracedir = tracedir;
		this.gvh = gvh;
	}
	
	public Trace(String name, String tracedir, GlobalVarHolder gvh, int driftMax, float skewBound) {
		this(name, tracedir, gvh);
		this.driftMax = driftMax;
		this.skewBound = skewBound;
	}

	public void traceStart() {
		openTraceFile(name);
	}
	
	private void openTraceFile(String fname) {
		if(trace == null) {
			if(driftMax != 0 || skewBound != 0f)
			{
				trace = new TraceWriter(fname,tracedir,driftMax,skewBound,gvh);
			} else {
				trace = new TraceWriter(fname,tracedir,gvh);
			}
		}
	}

	public void traceStart(int runId) {
		openTraceFile(runId + "-" + name);
	}
	
	public void traceVariable(String source, String name, Object data) {
		if(trace != null) trace.variable(source, name, data);
	}
	
	public void traceEvent(String source, String type, Object data) {
		if(trace != null) trace.event(source, type, data);
	}
	
	public void traceEvent(String source, String type) {
		if(trace != null) trace.event(source, type, null);
	}
	
	public void traceSync(String source) {
		if(trace != null) trace.sync(source);
	}
	
	public void traceEnd() {
		if(trace != null) {
			trace.close();
			trace = null;
		}
	}
	
}
