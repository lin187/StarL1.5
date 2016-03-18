package edu.illinois.mitra.starl.functions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.DSM;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.*;

public class DSMMultipleAttr implements DSM, MessageListener{

	private static final String TAG = "DSM_Multiple_Attr";
	//private static final String ERR = "Critical Error";
	private Map<String, DSMVariable> dsm_map;
	private GlobalVarHolder gvh;
	private String agent_name;
	
	public DSMMultipleAttr(GlobalVarHolder gvh){
		this.gvh = gvh;
		dsm_map = new HashMap<String, DSMVariable>();
		agent_name = gvh.id.getName();
		// TODO: attach to gvh
		// gvh.dsm = this;
		start();
	}

	@Override
	public void cancel() {
		gvh.trace.traceEvent(TAG, "Cancelled", gvh.time());
		gvh.comms.removeMsgListener(Common.MSG_DSM_INFORM);
	}

	@Override
	public void messageReceived(RobotMessage m) {
		if(m.getMID() == Common.MSG_DSM_INFORM && m.getTo().equals(agent_name) || m.getTo().equals("ALL")){
			// var_name owner timeStamp num_of_attr attr1_name attr1_type1 attr1_value attr2_name attr2_type attr2_value
		    //get the dsm content
		    //split back to value,type, and timestamp
		    MessageContents temp = m.getContents();
		    String var_name = temp.get(0);
		    String owner = temp.get(1);
		    long newtimestamp = Long.parseLong(temp.get(2));
		    int num_of_attr = Integer.parseInt(temp.get(3));
		    //if the key exists
		    if(!dsm_map.containsKey(var_name)) {
		    	dsm_map.put(var_name, new DSMVariable(var_name, owner));
		    }
		    DSMVariable cur = dsm_map.get(var_name);
		    for(int i = 0;i<num_of_attr; i++){
		    	String cur_name = temp.get(4+2*i);
		    	String cur_value =temp.get(5+2*i);
		    	//if the specific attr exists in the key
		    	if (cur.values.containsKey(cur_name)){
		    		//get the current attr
		    		AttrValue cur_attr = cur.values.get(cur_name);
		    		//use the update method
		    		cur_attr.updateValue(cur_value,newtimestamp);
		    	}
		    	else	{//else create new attr with specific value
		    		cur.values.put(cur_name, new AttrValue(cur_value, newtimestamp));
		    	}
		    }	
		}
	}
	

	@Override
	public void start() {
		gvh.trace.traceEvent(TAG, "Started", gvh.time());
		gvh.comms.addMsgListener(this, Common.MSG_DSM_INFORM);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<DSMVariable> getAll(String name, String owner) {
		return ((List<DSMVariable>) dsm_map.values());
	}

	@Override
	public DSMVariable get_V(String name, String owner){
		if(owner == "*"){
			if(dsm_map.containsKey(name)){
				return dsm_map.get(name);
			}
		}
		else{
			if(dsm_map.containsKey(name+owner)){
				return dsm_map.get(name+owner);
			}
		}
		gvh.log.d(TAG, "Variable not found: "+ name + ", owner:" + owner);
		return null;
	}
	
	@Override
	public String get(String name, String owner){
		if(owner == "*"){
			if(dsm_map.containsKey(name)){
				return dsm_map.get(name).values.get("default").s_value;
			}
		}
		else{
			if(dsm_map.containsKey(name+owner)){
				return dsm_map.get(name+owner).values.get("default").s_value;
			}
		}
		gvh.log.d(TAG, "Variable not found: "+ name + ", owner:" + owner);
		return null;
	}
	
	@Override
	public String get(String name, String owner, String attr){
		if(owner == "*"){
			if(dsm_map.containsKey(name)){
				DSMVariable cur = dsm_map.get(name);
				if(cur.values.containsKey(attr)){
					return cur.values.get(attr).s_value;
				}
			}
		}
		else{
			if(dsm_map.containsKey(name+owner)){
				DSMVariable cur = dsm_map.get(name+owner);
				if(cur.values.containsKey(attr)){
					return cur.values.get(attr).s_value;
				}
			}
		}
		return null;
	}

	@Override
	public boolean put(DSMVariable input) {
		if(!input.owner.equals(agent_name) && !input.owner.equals("*")){
			return false;
		}
		else{
			DSMVariable curVar;
			if(dsm_map.containsKey(input.name)){
				curVar = dsm_map.get(input.name);
			}
			else{
				curVar = new DSMVariable(input.name, input.owner);
				dsm_map.put(input.name, curVar);
			}
			updateVar(curVar, input);
			informOthers(input);
			return true;
		}
	}

	@Override
	public boolean put(String name, String owner, String attr, int value) {
		long curTS = getConsistantTS(owner);
		DSMVariable input;
		if(owner == "*"){
			input = new DSMVariable(name, owner, attr, String.valueOf(value), curTS);
		}
		else{
			input = new DSMVariable(name+owner, owner, attr, String.valueOf(value), curTS);
		}
		return put(input);
	}

	@Override
	public boolean put(String name, String owner, int value) {
		// put default attribute and value into variable
		DSMVariable input = new DSMVariable(name, owner, String.valueOf(value), getConsistantTS(owner));
		return put(input);
	}

	@Override
	public boolean put(String name, String owner, String... attr_and_value) {
		if(attr_and_value.length %2 != 0){
			return false;
		}
		long curTS = getConsistantTS(owner);
		DSMVariable input = new DSMVariable(name, owner, curTS, attr_and_value);
		return put(input);
	}
	
	@Override
	public boolean putAll(List<DSMVariable> inputs) {
		boolean r_code = true;
		for(DSMVariable tuple : inputs){
			boolean temp = put(tuple);
			if(!temp){
				gvh.log.d(TAG, "Fail to put: "+ tuple.toString());
			}
			r_code = r_code && temp;
		}
		return r_code;
	}
	
	public long getConsistantTS(String owner){
		if(owner.equals(agent_name)){
			return gvh.time();
			// TODO: add local clock and clock de_sync simulation 
		}
		else{
			// TODO : add clock sync to this time stamp
			return gvh.time();
		}
	}
	
	public void updateVar(DSMVariable oldVar, DSMVariable newVar){
		if(oldVar == null || newVar == null){
			System.out.println("Could not update null DSM Variable");
			return;
		}
		for(String curKey : newVar.values.keySet()){
			if(oldVar.values.containsKey(curKey)){
				AttrValue temp = newVar.values.get(curKey);
				oldVar.values.get(curKey).updateValue(temp.s_value, temp.s_timeS);
			}
			else{
				AttrValue temp = newVar.values.get(curKey);
				oldVar.values.put(curKey, new AttrValue(temp.s_value, temp.s_timeS));
			}
		}
		return;
	}
	
	private void informOthers(DSMVariable input) {
		MessageContents temp = new MessageContents();
		temp.append(input.toStringList());
		RobotMessage inform = new RobotMessage("ALL", agent_name, Common.MSG_DSM_INFORM, temp);
		gvh.comms.addOutgoingMessage(inform);
	}

	@Override
	public boolean createMW(String name, String... attr_and_value) {
		if(attr_and_value.length %2 != 0){
			return false;
		}
		long curTS = -1;
		// use a negative time stamp, if the MW variable already exists, no need to write the value again
		DSMVariable input = new DSMVariable(name, "*", curTS, attr_and_value);
		return put(input);
	}

	@Override
	public boolean createMW(String name, int value) {
		if(get(name, "*") != null){
			return false;
		}
		DSMVariable input = new DSMVariable(name, "*", String.valueOf(value), -1);
		return put(input);
	}
}
