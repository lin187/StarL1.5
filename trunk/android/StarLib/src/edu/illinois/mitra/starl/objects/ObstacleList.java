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
	
	public boolean validPath(RRTNode destinationNode){
		if(destinationNode == null)
			return false;
		if(ObList == null)
			return true;
		
		ItemPosition destination = new ItemPosition("pathPoint", destinationNode.position.x, destinationNode.position.y, 0);
		boolean check = true;
		for(int i=0; i< ObList.size(); i++){
			if(ObList.elementAt(i) != null){
				check = check && ObList.elementAt(i).validItemPos(destination);
			}
			else
			break;
		}
		return check;
	}
	
}
