package edu.illinois.mitra.starl.gvh;

import java.util.Map;
import java.util.Set;

/**
 * Maintains identities of participating robots. Instantiated in the GlobalVarHolder
 * 
 * @author Adam Zimmerman
 * @version 1.0
 * @see GlobalVarHolder
 *
 */
public class Id {
	// Identification
	private Map<String, String> participants = null;
	private String name = null;

	public Id(String name, Map<String, String> participants) {
		this.participants = participants;
		this.name = name;
	}

	public Set<String> getParticipants() {
		return participants.keySet();
	}

	public Map<String,String> getParticipantsIPs() {
		return participants;
	}
	
	public String getName() {
		return name;
	}
}
