package edu.illinois.mitra.starl.gvh;

import edu.illinois.mitra.starl.objects.TraceWriter;

public class Trace {
	private String name;
	private TraceWriter trace;
	private String tracedir;
	
	public Trace(String name, String tracedir) {
		this.name = name;
		this.tracedir = tracedir;
	}

	public void traceStart() {
		if(trace == null) {
			trace = new TraceWriter(name,tracedir);
		}
	}
	
	public void traceStart(int runId) {
		if(trace == null) {
			trace = new TraceWriter(runId + "-" + name,tracedir);
		}
	}
	
	public void traceStart(int drift, float skew) {
		if(trace == null) {
			trace = new TraceWriter(name,tracedir,drift,skew);
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
