package edu.illinois.mitra.starl.interfaces;

import java.util.HashMap;

public interface Traceable {
	
	// Traceable objects return a HashMap (String -> String), keys represent tag names, values represent tag values
	// For example, put("color", "orange") will write "<color> orange </color>" in the XML file
	public HashMap<String,Object> getXML();
}
