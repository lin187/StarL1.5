package edu.illinois.mitra.starl.interfaces;

import java.awt.Graphics2D;
import java.awt.Point;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;

/**
 * The base class for all StarL application main classes.
 * @author Adam Zimmerman
 * @version 1.0
 */
public abstract class LogicThread extends StarLCallable implements Cancellable, Drawer, AcceptsPointInput {

	protected String name;
	
	public LogicThread(GlobalVarHolder gvh) {
		super(gvh, "LogicThread");
		this.name = gvh.id.getName();
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void draw(Graphics2D g) 
	{
		// used in simulation, you can draw something algorithm specific (debug information perhaps)
	}
	
	@Override
	public void receivedPointInput(Point p)
	{
		// used if you want your code to respond to point-input from the user, in simulation this is done with right clicks
	}
}
