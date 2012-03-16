
package edu.illinois.mitra.starl.objects;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;
import edu.illinois.mitra.starl.exceptions.ItemFormattingException;

public class positionList {
	private static final String TAG = "positionList";
	private static final String ERR = "Critical Error";
	
	private HashMap<String,itemPosition> positions;
	
	// Default constructor
	public positionList() {
		positions = new HashMap<String,itemPosition>();
	}
	
	public void update(itemPosition received) {
		positions.put(received.getName(), received);
	}
	
	public itemPosition getPosition(String name) {
		if(positions.containsKey(name)) {
			return positions.get(name);
		}
		return null;
	}
	
	public itemPosition getPositionRegex(String exp) {
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
		for(itemPosition i : positions.values()) {
			toRet = toRet + i.toString() + "\n";
		}
		return toRet;
	}
	
	public int getNumPositions() {
		return positions.size();
	}
	
	public ArrayList<itemPosition> getList() {
		return new ArrayList<itemPosition>(positions.values());
	}
}