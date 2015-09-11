package edu.illinois.mitra.starl.objects;

public class AttrValue{
	public String s_value;
	public long s_timeS;
	
	public AttrValue(){
		s_value = new String("");
		s_timeS = -1;
	}
	public AttrValue(String s_value, long s_timeS){
		this.s_value = s_value;
		this.s_timeS = s_timeS;
	}
	
	public void updateValue(String i_value, long i_time){
		if(i_time >= s_timeS){
			s_value = i_value;
			s_timeS = i_time;
		}
		else{
			System.out.println("can not update due to time stamp");
		}
	}
	
	public Object getValue(){
		if(this.s_value == null){
			return null;
		}
		return s_value;
	}
}	