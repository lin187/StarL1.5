package edu.illinois.mitra.starl.interfaces;

import java.util.List;
import java.util.concurrent.Callable;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;

public abstract class LogicThread implements Cancellable, Callable<List<Object>> {

	protected GlobalVarHolder gvh;
	protected String name;
	protected Object[] results;
	
	public LogicThread(GlobalVarHolder gvh) {
		this.gvh = gvh;
		this.name = gvh.id.getName();
		results = new Object[0];
	}
	
	@Override
	public void cancel() {
		// TODO Auto-generated method stub
	}
}
