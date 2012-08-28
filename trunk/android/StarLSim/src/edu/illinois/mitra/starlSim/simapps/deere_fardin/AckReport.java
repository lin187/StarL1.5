package edu.illinois.mitra.starlSim.simapps.deere_fardin ; 

public class AckReport {
	
	int lastPathId ; 
	int robotId ;
	boolean received ; 

	public AckReport(){
		this.lastPathId = 0 ; 
		this.received = false ; 
	}	
}