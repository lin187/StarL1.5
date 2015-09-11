package edu.illinois.mitra.starl.objects;

import java.util.*;
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
	public DSMVariable(String name, String owner) {
		this.name = name;
		this.owner = owner;
		this.values = new HashMap<String, AttrValue>();
	}
	public DSMVariable(String name, String owner, String value, long timestamp) {
		this.name = name;
		this.owner = owner;
		this.values = new HashMap<String, AttrValue>();
		this.values.put("default", new AttrValue(value, timestamp));
	}
	public DSMVariable(String name, String owner, String attr, String value, long timestamp) {
		this.name = name;
		this.owner = owner;
		this.values = new HashMap<String, AttrValue>();
		this.values.put(attr, new AttrValue(value, timestamp));
	}
	public DSMVariable(String name, String owner, long timestamp, String... attr_and_value) {
		this.name = name;
		this.owner = owner;
		this.values = new HashMap<String, AttrValue>();
		if(attr_and_value.length %2 != 0){
			System.out.println("attribute and value list length is not divisible by 2, failed to create DSM Variable");
			return;
		}
		int i = 0;
		String temp = "";
		for(String attr_val : attr_and_value){
			if(i%2 == 0){
				temp = attr_val;
			}
			else{
				this.values.put(temp, new AttrValue(attr_val, timestamp));
			}
			i++;
		}
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
			infolist.add(key);
			infolist.add(cur.s_value);
		}
		if(infolist.size() != 4 + 2*values.size()){
			System.out.println("Can not get string of this DSM Variable!");
			return null;
		}
		return infolist;
	}
	private long get_oldestTS() {
		if(values.isEmpty()){
			return 0; 
		}
		long toReturn = Long.MAX_VALUE;
		for(String key : values.keySet()){
			AttrValue cur = values.get(key);
			toReturn = Math.min(toReturn, cur.s_timeS);
		}
		return toReturn;
	}
}

