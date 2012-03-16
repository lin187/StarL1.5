package edu.illinois.mitra.starl.interfaces;

// Interface for items which need to be shut down properly when the program concludes
public interface Cancellable {
	public void cancel();
}
