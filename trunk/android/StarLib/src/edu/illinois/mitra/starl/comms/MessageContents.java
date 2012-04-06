package edu.illinois.mitra.starl.comms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageContents {
	private List<String> contents;
	
	public MessageContents(String ... pieces) {
		contents = new ArrayList<String>();
		for(String p : pieces) {
			contents.add(p);
		}
	}
	
	public MessageContents(String fromString) {
		contents = new ArrayList<String>();
		String[] pieces = fromString.split("`");
		contents.addAll(Arrays.asList(pieces));
	}
	
	public MessageContents(int fromInt) {
		contents = new ArrayList<String>();
		contents.add(Integer.toString(fromInt));
	}
	
	public void append(MessageContents other) {
		append(other.getContents());
	}
	
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
