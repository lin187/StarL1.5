
package edu.illinois.mitra.Objects;

import java.util.HashMap;
import java.util.Iterator;


import android.util.Log;

public class positionList2 {
	private HashMap<String,itemPosition> positions;
	
	public positionList2() {
		positions = new HashMap<String,itemPosition>();
	}
	
	public void update(String received) {		
		String[] parts = received.replace(",", "").split("\\|");
		
		// If this name has been seen before, update the entry in the list
		if(parts.length == 6) {
			if(positions.containsKey(parts[1])) {
				itemPosition toUpdate = positions.get(parts[1]);
				toUpdate.setPos(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
				positions.put(parts[1], toUpdate);
			} else {
				positions.put(parts[1], new itemPosition(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4])));
			}
		} else {
			Log.d("positionList", "Invalid item formatting: " + parts.length);
		}
	}
	
	public itemPosition getPosition(String name) {
		if(positions.containsKey(name)) {
			return positions.get(name);
		} else {
			return null;
		}
	}
	
	public boolean hasPositionFor(String name) {
		return positions.containsKey(name);
	}

	@Override
	public String toString() {
		return positions.entrySet().toString();
	}
	
	public int getNumPositions() {
		return positions.size();
	}
	
	public itemPosition getPositionAtIndex(int idx) {
		Iterator<String> iter = positions.keySet().iterator();
		for(int i = 0; i < (idx-1); i++) {
			iter.next();
		}
		return positions.get(iter.next());
	}
	
	private int indexOfName(String name) {
		Iterator<String> iter = positions.keySet().iterator();
		int i;
		for(i = 0; !iter.next().equals(name); i++) {
			if(i > positions.size()) {
				i = -1;
				break;
			}
		}
		return i;
	}
}
