package edu.illinois.mitra.starl.comms;

/**
 * A MessageResult object is returned whenever a RobotMessage is added to the outgoing queue to track its progress. 
 * Once all receivers have acknowledged the message, getResult will return true. 
 * 
 * @author Adam Zimmerman
 * @version 1.0
 */
public class MessageResult {
	private boolean result = true;
	private int results_set = 0;
	private int recipients = 1;
	
	public static interface ResultCallback {
		public void messageFailed();
		public void messageReceived();
	}
	
	private static final ResultCallback DEFAULT_CALLBACK = new ResultCallback() {
		@Override
		public void messageReceived() {
		}
		
		@Override
		public void messageFailed() {
		}
	};

	private ResultCallback callback = DEFAULT_CALLBACK;
	
	public MessageResult(int recipients) {
		this.recipients = recipients;
	}
	
	public void setCallback(ResultCallback callback) {
		this.callback = callback;
	}
	
	public void setFailed() {
		results_set ++;
		result = false;
		callback.messageFailed();
	}
	
	public void setReceived() {
		results_set ++;
		result &= true;
		
		if(results_set == recipients && result == true)
			callback.messageReceived();
	}
}
