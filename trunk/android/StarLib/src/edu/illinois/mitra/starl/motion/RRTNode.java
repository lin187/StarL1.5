package edu.illinois.mitra.starl.motion;

import java.awt.Point;


public class RRTNode {
	public Point position;
	public RRTNode parent;

	public double [] getValue(){
		double [] toReturn = {position.x,position.y}; 
		return toReturn;
	}

	public RRTNode(){
		position.x = 0;
		position.y = 0;
		parent = null;
	}
	
	public RRTNode(int x, int y){
		position.x = x;
		position.y = y;
		parent = null;
	}

	public RRTNode(RRTNode copy){
		position.x = copy.position.x;
		position.y = copy.position.y;
		parent = copy.parent;
	}

}

