package edu.illinois.mitra.Objects.exceptions;

public class MessageIDInUseException extends Exception {

	int mid = -1;
	
	public MessageIDInUseException() {
	}
	
	public MessageIDInUseException(int mid) {
		this.mid = mid;
	}
	
	public String getError() {
		return "Message ID " + mid + " already has a listner!";
	}
}
