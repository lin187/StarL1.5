package edu.illinois.mitra.starl.interfaces;

import java.util.LinkedList;
import java.util.concurrent.Callable;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;

public abstract class LogicThread implements Cancellable, Callable<LinkedList<Object>> {

	protected GlobalVarHolder gvh;
	
	public LogicThread(GlobalVarHolder gvh) {
		this.gvh = gvh;
	}
	
	@Override
	public void cancel() {
		// TODO Auto-generated method stub
	}
}
