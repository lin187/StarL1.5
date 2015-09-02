package edu.illinois.mitra.starl.objects;

public class AttrValue{
	public String type;
	public String s_value;
	public long s_timeS;
	
	public AttrValue(String s_value, long s_timeS){
		this.type = "string";
		this.s_value = s_value;
		this.s_timeS = s_timeS;
	}
	
	public AttrValue(int s_value, long s_timeS){
		this.type = "int";
		this.s_value = Integer.toString(s_value);
		this.s_timeS = s_timeS;
	}
	
	public AttrValue(long s_value, long s_timeS){
		this.type = "long";
		this.s_value = Long.toString(s_value);
		this.s_timeS = s_timeS;
	}
	
	public AttrValue(float s_value, long s_timeS){
		this.type = "float";
		this.s_value = Float.toString(s_value);
		this.s_timeS = s_timeS;
	}
	
	public AttrValue(double s_value, long s_timeS){
		this.type = "double";
		this.s_value = Double.toString(s_value);
		this.s_timeS = s_timeS;
	}
	
	public AttrValue(boolean s_value, long s_timeS){
		this.type = "boolean";
		this.s_value = Boolean.toString(s_value);
		this.s_timeS = s_timeS;
	}
	
	public AttrValue(String type, String s_value, long s_timeS){
		this.type = type;
		this.s_value = s_value;
		this.s_timeS = s_timeS;
	}
	
	public void updateValue(String i_type, String i_value, long i_time){
		if(i_time> s_timeS && i_type.equals(type)){
			s_value = i_value;
			s_timeS = i_time;
		}
	}
	
	public Object getValue(){
		if(this.type == null){
			return null;
		}
		switch(this.type){
		case "int":
			return Integer.parseInt(s_value);
		case "boolean":
			return Boolean.parseBoolean(s_value);
		case "long":
			return Long.parseLong(s_value);
		case "float":
			return Float.parseFloat(s_value);
		case "double":
			return Double.parseDouble(s_value);
		case "string":
			return new String(s_value);
		default:
			System.out.println("Unsupported dsm variable type: "+ this.type);
			return null;
		}
	}
}	