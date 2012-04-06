
package edu.illinois.mitra.starl.objects;

import java.util.ArrayList;
import java.util.HashMap;

public class PositionList {
	private static final String TAG = "positionList";
	private static final String ERR = "Critical Error";
	
	private HashMap<String,ItemPosition> positions;
	
	// Default constructor
	public PositionList() {
		positions = new HashMap<String,ItemPosition>();
	}
	
	public void update(ItemPosition received) {
		positions.put(received.getName(), received);
	}
	
	public ItemPosition getPosition(String name) {
		if(positions.containsKey(name)) {
			return positions.get(name);
		}
		return null;
	}
	
	public ItemPosition getPositionRegex(String exp) {
		for(String n : positions.keySet()) {
			if(n.matches(exp)) {
				return positions.get(n);
			}
		}
		return null;
	}
	
	public boolean hasPositionFor(String name) {
		return positions.containsKey(name);
	}

	@Override
	public String toString() {
		String toRet = "";
		for(ItemPosition i : positions.values()) {
			toRet = toRet + i.toString() + "\n";
		}
		return toRet;
	}
	
	public int getNumPositions() {
		return positions.size();
	}
	
	public ArrayList<ItemPosition> getList() {
		return new ArrayList<ItemPosition>(positions.values());
	}
}