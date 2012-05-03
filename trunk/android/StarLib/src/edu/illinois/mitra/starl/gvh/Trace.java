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
		if(trace == null) {
			trace = new TraceWriter(name,tracedir,gvh);
		}
	}
	
	public void traceStart(int runId) {
		if(trace == null) {
			trace = new TraceWriter(runId + "-" + name,tracedir,gvh);
		}
	}
	
	public void traceStart(int drift, float skew) {
		if(trace == null) {
			trace = new TraceWriter(name,tracedir,drift,skew,gvh);
		}		
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
