
package edu.illinois.mitra.starl.objects;

import java.util.*;
import java.util.Map.Entry;

/**
 * PositionList is a thin wrapper for a HashMap (String -> ItemPosition). Collections of ItemPositions
 * are stored in PositionLists.
 * they are sorted in the natural order of their keys
 * @author Adam Zimmerman, Yixiao Lin
 * @version 1.1
 */
public class PositionList implements Iterable<ItemPosition> {
	private static final String TAG = "positionList";
	private static final String ERR = "Critical Error";
	
	private TreeMap<String,ItemPosition> positions;
	
	/**
	 * Create an empty PositionList
	 */
	public PositionList() {
		positions = new TreeMap<String,ItemPosition>();
	}
	
	/**
	 * @param received The ItemPosition to add to the list. If a position with the same name is present, it
	 * will be overwritten. 
	 */
	public void update(ItemPosition received, long time) {
		if(positions.containsKey(received.name)) {
			try {
				int velocity = (int) (received.distanceTo(positions.get(received.name))/(time - positions.get(received.name).receivedTime));
				received.velocity =velocity;
			} catch (ArithmeticException e) {
				
			}
		}
		received.receivedTime = time;
		positions.put(received.name, received);
	}
	
	public void update(ItemPosition received) {
		update(received, 0);
	}
	
	
	/**
	 * @param name The name to match
	 * @return An ItemPosition with a matching name, null if one doesn't exist.
	 */
	public ItemPosition getPosition(String name) {
		if(positions.containsKey(name)) {
			return positions.get(name);
		}
		return null;
	}
	
	/**
	 * @param exp The regex string to match against
	 * @return The first ItemPosition in the PositionList whose name matches the regular expression
	 */
	public ItemPosition getPositionRegex(String exp) {
		for(Entry<String, ItemPosition> entry : positions.entrySet()) {
			if(entry.getKey().matches(exp)) {
				return entry.getValue();
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
	
	public void clear() {
		positions.clear();
	}
	
	/**
	 * @return An ArrayList representation of all contained ItemPositions
	 */
	public ArrayList<ItemPosition> getList() {
		return new ArrayList<ItemPosition>(positions.values());
	}

	@Override
	public Iterator<ItemPosition> iterator() {
		return positions.values().iterator();
	}
}