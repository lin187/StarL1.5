
package edu.illinois.mitra.Objects;

import java.util.ArrayList;


import android.util.Log;

public class positionList {
	private ArrayList<itemPosition> positions;
	private ArrayList<String> names;
	
	// Default constructor
	public positionList() {
		names = new ArrayList<String>();
		positions = new ArrayList<itemPosition>();
	}
	
	public void update(String received) {		
		String[] parts = received.replace(",", "").split("\\|");
		if(parts.length == 6) {
			// If this name has been seen before, update the entry in the list
			int idx = names.indexOf(parts[1]);
			if(idx != -1) {
				itemPosition toUpdate = positions.get(idx);
				toUpdate.setPos(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
			// Add to the list if it's a new name
			} else {
				positions.add(new itemPosition(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4])));
				names.add(parts[1]);
			}
		} else {
			Log.d("positionList", "Invalid item formatting: " + parts.length);
		}
	}
	
	public itemPosition getPosition(String name) {
		int pos = names.indexOf(name);
		if(pos > -1) {
			return positions.get(pos);
		} else {
			return null;
		}
	}
	
	public boolean hasPositionFor(String name) {
		return names.contains(name);
	}

	@Override
	public String toString() {
		String toRet = "";
		for(int i = 0; i<positions.size(); i++) {
			toRet = toRet + positions.get(i).toString() + "\n";
		}
		return toRet;
	}
	
	public int getNumPositions() {
		return positions.size();
	}
	
	public itemPosition getPositionAtIndex(int idx) {
		return positions.get(idx);
	}
}