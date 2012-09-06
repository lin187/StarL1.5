package edu.illinois.mitra.starlSim ; 

public class AckReport {
	
	int lastPathIdReceived ; 
	int robotId ;
	boolean received ;
	public int lastPathIdSent; 

	public AckReport(){
		this.lastPathIdSent = 0 ;
		this.lastPathIdReceived = 0 ; 
		this.received = false ; 
	}	
}