package edu.illinois.mitra.starl.comms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MessageContents is a container allowing multiple strings to be easily sent in a single message.
 * These strings are transmitted by being delineated with a backtick character (`). Strings containing
 * a backtick can not be sent and will cause a RuntimeException.
 * In a future version, backticks will be safely escaped.
 * 
 * @author Adam Zimmerman
 * @version 1.0
 * @see RobotMessage
 */
public class MessageContents {
	private List<String> contents;
	
	/**
	 * @param pieces A string array to be sent in a message
	 */
	public MessageContents(String ... pieces) {
		contents = new ArrayList<String>();
		for(String p : pieces) {
			contents.add(p);
		}
	}
	
	/**
	 * Reconstruct a MessageContents object from a received string
	 * @param fromString Received string to be parsed
	 */
	public MessageContents(String fromString) {
		contents = new ArrayList<String>();
		String[] pieces = fromString.split("`");
		contents.addAll(Arrays.asList(pieces));
	}
	
	/**
	 * Construct a new MessageContents object containing a single int
	 * @param fromInt The int value to send
	 */
	public MessageContents(int fromInt) {
		contents = new ArrayList<String>();
		contents.add(Integer.toString(fromInt));
	}
	
	/**
	 * Concatenate two MessageContents objects
	 * @param other The MessageContents to append
	 */
	public void append(MessageContents other) {
		append(other.getContents());
	}
	
	/**
	 * Append a list of strings to this MessageContents object
	 * @param other The list to append
	 */
	public void append(List<String> other) {
		for(String s : other) {
			contents.add(s);
		}
	}
	
	public List<String> getContents() {
		return contents;
	}
	
	public String get(int location) {
		return (String) contents.get(location);
	}
	
	public String toString() {
		String retval = "";
		for(String o : contents) {
			if(o.toString().contains("`")) throw new RuntimeException("Tried to make a MessageContents containing a backtick!!");
			retval = retval + o.toString() + "`";
		}		
		return retval;
	}
	
	public int getLength() {
		return contents.size();
	}
}
