package edu.illinois.mitra.starl.interfaces;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;

/**
 * The base class for all StarL application main classes.
 * @author Adam Zimmerman
 * @version 1.0
 */
public abstract class LogicThread extends StarLCallable implements Cancellable {

	protected String name;
	
	public LogicThread(GlobalVarHolder gvh) {
		super(gvh, "LogicThread");
		this.name = gvh.id.getName();
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
	}
}
