package edu.illinois.mitra.starl.exceptions;

/**
 * An exception thrown when an entity to be parsed has the incorrect number of elements
 * @author Adam Zimmerman
 *
 */
public class ItemFormattingException extends Exception {
	private static final long serialVersionUID = 1L;
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
