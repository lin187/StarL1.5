package edu.illinois.mitra.starl.interfaces;

/**
 * Synchronizers are used to ensure that all agents are at the same point in the execution of a program
 * @author Adam Zimmerman
 * @version 1.0
 * 
 */
public interface Synchronizer extends Cancellable {
	
	/**
	 * Signals to all agents that this robot has arrived at a barrier and is ready to proceed
	 * @param barrierID The identifier of the barrier
	 */
	public abstract void barrierSync(String barrierID);

	/**
	 * Checks whether all robots have arrived at a barrier
	 * @param barrierID The identifier of the barrier to check against
	 * @return True if all robots have signaled their arrival at the barrier, false otherwise
	 */
	public abstract boolean barrierProceed(String barrierID);
}