package edu.illinois.mitra.starl.exceptions;

public class ItemFormattingException extends Exception {
	private String error;
	
	public ItemFormattingException(String string) {
		error = string;
	}

	public String getError() {
		return error;
	}
	
	public String toString() {
		return getError();
	}
}
