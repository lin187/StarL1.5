package edu.illinois.mitra.starl.objects;

import java.util.*;

import edu.illinois.mitra.starl.motion.RRTNode;

public class ObstacleList {
	public Vector<Obstacles> ObList;
	public int detect_Precision;
	public int de_Radius;
	private long lastUpdateTime;

	public ObstacleList(Vector<Obstacles> Oblist) {
			this.ObList = Oblist;
			lastUpdateTime = System.currentTimeMillis();
		}
	
	public ObstacleList(){
		this.ObList = new Vector<Obstacles>(3,2);
		lastUpdateTime = System.currentTimeMillis();
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
	
	/**
	* This method is used for checking if the line alone destination and current has any intersection with any obstacles
	*/
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
	
	/**
	 * 
	 * @param destination
	 * @param radius
	 * @return boolean
	 * check if the point is reachable by robot
	 * 
	 */
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
	
	/**
	 * 
	 * @param destinationNode
	 * @param currentNode
	 * @param Radius
	 * @return
	 * 
	 *return true if the path specified by two RRTNode has a line such that every point alone the line is reachable by robots.
	 *It checks if two line segments smallest distance is bigger than radius
	 *For example, line AB and CD, the shortest distance is minimum of A to CD, B to CD, C to AB, D to AB 
	 */

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
					double minDist = ObList.elementAt(i).findMinDist(destinationNode, currentNode);
					check = check && (minDist> Radius); 
					if(!check)
						break;
				}
				else
					break;
			}
			return check;
			
		}
	}
	
//methods for hidden or time vise obstacles	
	public void updateObs(){
		long duration = System.currentTimeMillis() - lastUpdateTime;
		synchronized (ObList){
		for(int i=0; i< ObList.size(); i++){
			if(ObList.elementAt(i) != null){
				if(ObList.elementAt(i).timeFrame == 0){
					ObList.remove(i);
					updateObs();
					break;
				}
				else{
					if(ObList.elementAt(i).timeFrame > 0){
						ObList.elementAt(i).timeFrame -= duration;
						if(ObList.elementAt(i).timeFrame <0)
							ObList.elementAt(i).timeFrame = 0;
					}
				}
			}
			else
			break;
		}
		}
	}

	public ObstacleList downloadObs() {
		//download method to provide robots with informations of unhidden obstacles
		// sever as an selected deep copy
		if(ObList == null)
			return null;
		
		ObstacleList obsList = new ObstacleList();
		for(int i = 0; i< ObList.size(); i++){
			Obstacles temp = new Obstacles(ObList.get(i));
			if(!ObList.get(i).hidden)
			obsList.ObList.addElement(temp);
		}
		obsList.de_Radius = de_Radius;
		obsList.detect_Precision = detect_Precision;
		
		return obsList;
	}

	//change the obstacle map to grid wise representation of obstacles, detect_Precision is the length of the grid, 1 as minimum
	public void Gridfy() {
		for(int i=0; i< ObList.size(); i++){
			if(ObList.elementAt(i) != null){
				ObList.elementAt(i).ToGrid(detect_Precision);
			}
		
		}
	}
	
	//add the detected obstacle to the current map
	public void detected(ItemPosition blocker){
		boolean uncontained = true;
		for(int i=0; i< ObList.size(); i++){
			if(ObList.elementAt(i) != null){
				uncontained = uncontained && ObList.elementAt(i).validItemPos(blocker);
			}
		
		}
		if(uncontained){
			Obstacles newObs = new Obstacles(blocker.x, blocker.y);
			ObList.addElement(newObs);
			ObList.lastElement().ToGrid(detect_Precision * de_Radius);
		}
	}
	
	public void remove(ItemPosition robotPos, double radius){
		if(robotPos == null)
			return;
		if(ObList == null)
			return;
		
		for(int i=0; i< ObList.size(); i++){
			if(ObList.elementAt(i) != null){
				if(!ObList.elementAt(i).validItemPos(robotPos, radius)){
					ObList.remove(i);
				}
			}
			else
			break;
		}
		return;
	}
	
}
