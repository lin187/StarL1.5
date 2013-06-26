package edu.illinois.mitra.starl.objects;

import java.util.*;

import edu.illinois.mitra.starl.motion.RRTNode;

public class ObstacleList {
	public Vector<Obstacles> ObList;

	public ObstacleList(Vector<Obstacles> Oblist) {
			this.ObList = Oblist;
		}
	
	public ObstacleList(){
		this.ObList = new Vector<Obstacles>(3,2);
	}
	
//check if the line alone destination and current has any intersection with any obstacles
	public boolean badPath(ItemPosition destination, ItemPosition current){
		boolean check = false;
		for(int i=0; i< ObList.size(); i++){
			if(ObList.elementAt(i) != null){
				check = check || ObList.elementAt(i).checkCross(destination, current);
			}
			else
			break;
		}
		return check;
	}
	
//check if the line alone destination and current has any intersection with any obstacles
	public boolean badPath(RRTNode destinationNode, RRTNode currentNode){
		ItemPosition destination = new ItemPosition("NodeToIPDes", destinationNode.position.x, destinationNode.position.y, 0); 
		ItemPosition current = new ItemPosition("NodeToIPCurrt", currentNode.position.x, currentNode.position.y, 0); 
		boolean check = false;
		for(int i=0; i< ObList.size(); i++){
			if(ObList.elementAt(i) != null){
				check = check || ObList.elementAt(i).checkCross(destination, current);
			}
			else
			break;
		}
		return check;
	}
	
//check if the point is reachable by robot
	public boolean validstarts(ItemPosition destination, double radius){
		if(destination == null)
			return false;
		if(ObList == null)
			return true;
		
		boolean check = true;
		for(int i=0; i< ObList.size(); i++){
			if(ObList.elementAt(i) != null){
				check = check && ObList.elementAt(i).validItemPos(destination, radius);
			}
			else
			break;
		}
		return check;
	}
	
	
//return true if the path specified by two RRTNode has a line such that every point alone the line is reachable by robots
//check two line segments smallest distance is bigger than radius
//For line AB and CD, shortest distance is min of A to CD, B to CD, C to AB, D to AB
	public boolean validPath(RRTNode destinationNode, RRTNode currentNode,  int Radius){
		if(destinationNode == null)
			return false;
		if(ObList == null)
			return true;
		if(badPath(destinationNode, currentNode))
			return false;
		else{
			boolean check = true;
			for(int i=0; i< ObList.size(); i++){
				if(ObList.elementAt(i) != null){
					check = check && (ObList.elementAt(i).findMinDist(destinationNode, currentNode)> Radius);
					if(!check)
						break;
				}
				else
					break;
			}
			return check;
			
			/*
			boolean check = true;
			RRTNode FirstNode = currentNode;
			RRTNode SecondNode = destinationNode;
			if(destinationNode.position.x < currentNode.position.x){
				FirstNode = destinationNode;
				SecondNode = currentNode;
			
			}
			
			for(int p = 1; p< (SecondNode.position.x - FirstNode.position.x); p++){
				ItemPosition destination = new ItemPosition("pathPoint", destinationNode.position.x + p, 
					SecondNode.position.y + p*(SecondNode.position.y -FirstNode.position.y)/(SecondNode.position.x -FirstNode.position.x) , 0);
					check = check && validstarts(destination, Radius);
					if(check == false)
						break;
			}
			return check;
			*/
			
//			ItemPosition dest = new ItemPosition("dest", destinationNode.position.x, destinationNode.position.y, 0);
//			return(validstarts(dest, Radius));
		}
	}

	
}
