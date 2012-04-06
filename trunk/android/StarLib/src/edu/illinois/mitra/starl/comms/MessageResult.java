package edu.illinois.mitra.starl.comms;

public class MessageResult {
	private boolean result = true;
	private int results_set = 0;
	private int recipients = 1;
	
	public MessageResult(int recipients) {
		this.recipients = recipients;
	}
	
	public void setFailed() {
		results_set ++;
		//gvh.i("Result", "Failure set #" + results_set + " out of " + recipients);
		result = false;
	}
	
	public void setReceived() {
		results_set ++;
		//gvh.i("Result", "Received set #" + results_set + " out of " + recipients);
		result &= true;
	}
	
	public Boolean getResult() {
		if(results_set >= recipients || result == false) {
			return result;
		}
		return null;
	}
}
