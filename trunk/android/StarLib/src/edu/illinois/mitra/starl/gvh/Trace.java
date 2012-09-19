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
	
	public Trace(String name, String tracedir, GlobalVarHolder gvh) {
		this.name = name;
		this.tracedir = tracedir;
		this.gvh = gvh;
	}

	public void traceStart() {
		openTraceFile(name);
	}
	
	private void openTraceFile(String fname) {
		if(trace == null) {
			trace = new TraceWriter(fname,tracedir,gvh);
		}
	}

	public void traceStart(int runId) {
		openTraceFile(runId + "-" + name);
	}
	
	public void traceVariable(String source, String name, Object data, long timestamp) {
		if(trace != null) trace.variable(source, name, data, timestamp);
	}
	
	public void traceEvent(String source, String type, Object data, long timestamp) {
		if(trace != null) trace.event(source, type, data, timestamp);
	}
	
	public void traceEvent(String source, String type, long timestamp) {
		if(trace != null) trace.event(source, type, null, timestamp);
	}
	
	public void traceSync(String source, long timestamp) {
		if(trace != null) trace.sync(source, timestamp);
	}
	
	public void traceEnd() {
		if(trace != null) {
			trace.close();
			trace = null;
		}
	}
	
}
