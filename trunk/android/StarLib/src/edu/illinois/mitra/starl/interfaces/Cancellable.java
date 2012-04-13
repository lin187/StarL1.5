package edu.illinois.mitra.starl.interfaces;

// Interface for items which need to be shut down properly when the program concludes
/**
 * Objects implementing Cancellable provide a method, cancel(), which is to be called before the program ends in order to properly conclude 
 * @author Adam Zimmerman
 * @version 1.0
 */
public interface Cancellable {
	public void cancel();
}
