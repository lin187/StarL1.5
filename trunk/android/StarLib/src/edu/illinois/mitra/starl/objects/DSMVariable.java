package edu.illinois.mitra.starl.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * This class represents a DSM Variable
 * owner is the agent that owns this variable, * means it is a multiple writer variable
 * 
 * @author Liyi Sun and Yixiao Lin
 *
 * TODO: add different types of values with type safety
 * 
 */
public class DSMVariable {
	public String name;
	//public int id;
	public String attr;
	public HashMap<String, AttrValue> values;
	public String owner;

	//constructor
	public DSMVariable(String name, String owner, int value, long timestamp) {
		this.name = name;
		this.owner = owner;
		this.values = new HashMap<String, AttrValue>();
		this.values.put("default", new AttrValue(value, timestamp));
	}
	public DSMVariable(String name, String owner) {
		this.name = name;
		this.owner = owner;
		this.values = new HashMap<String, AttrValue>();
	}

	public String getname() {
		return this.name;
	}
	
	public List<String> toStringList(){
		List<String> infolist = new ArrayList<String>();
		infolist.add(name);
		infolist.add(owner);
		long min = get_oldestTS();
		infolist.add(String.valueOf(min));
		infolist.add(String.valueOf(values.size()));
		for(String key : values.keySet()){
			AttrValue cur = values.get(key);
			infolist.add(cur.type);
			infolist.add(cur.s_value);
		}
		if(infolist.size() != 4+ 3*values.size()){
			System.out.println("Can not get string of this DSM Variable!");
			return null;
		}
		return infolist;
	}
	private long get_oldestTS() {
		long toReturn = 0;
		for(String key : values.keySet()){
			AttrValue cur = values.get(key);
			toReturn = Math.min(toReturn, cur.s_timeS);
		}
		// TODO Auto-generated method stub
		return toReturn;
	}
}

