package edu.illinois.mitra.starl.motion;

import java.util.LinkedList;
import edu.illinois.mitra.starl.objects.*;
import edu.wlu.cs.levy.CG.KDTree;

public class RRTPathPlanning {
	public LinkedList<ItemPosition> path;
	
    private void findRoute(ItemPosition destination, ItemPosition start, int K, ObstacleList obsList, int xRange, int yRange) {
//initialize a kd tree;
    	KDTree<RRTNode> kd = new KDTree<RRTNode>(2);
    	double [] root = {start.x,start.y};
    	final RRTNode rootNode = new RRTNode(start.x,start.y);
    	final RRTNode destNode = new RRTNode(destination.x, destination.y);
    	path.clear();
    	
    	try{
    		kd.insert(root, rootNode);
    	}
    	catch(Exception e){
    		System.err.println(e);
    	}
    	
    	
    	RRTNode currentNode = new RRTNode(rootNode);
    	RRTNode addedNode = new RRTNode(rootNode);
    //for(i< k)  keep finding	
    	for(int i = 0; i<K; i++){
    	//if can go from current to destination, meaning path found, add destinationNode to final, stop looping.
    		if(!obsList.badPath(addedNode, destNode)){
    			
    			destNode.parent = addedNode;
    			try{	
    			kd.insert(destNode.getValue(), destNode);
    			}
        		catch (Exception e) {
        		    System.err.println(e);
        		}
    			System.out.println("Path found!");
    			break;
    		}
    		//not find yet, keep exploring
    		//random a sample point in the valid set of space
    		boolean validRandom = false;
    		int xRandom = 0;
    		int yRandom = 0;
    		RRTNode sampledNode = new RRTNode(xRandom, yRandom);
    		while(!validRandom){
    			xRandom = (int)(Math.random() * ((xRange) + 1));
        		yRandom = (int)(Math.random() * ((yRange) + 1));
        		sampledNode.position.x = xRandom;
        		sampledNode.position.y = yRandom;
        		validRandom = !obsList.validPath(sampledNode); 	
    		}
    		// with a valid random sampled point, we find it's nearest neighbor in the tree, set it as current Node
    		try{
    		currentNode = kd.nearest(sampledNode.getValue());
    		}
    		catch (Exception e) {
    		    System.err.println(e);
    		}
    		sampledNode = toggle(currentNode, sampledNode, obsList);
    		//check if toggle failed
    		//if not failed, insert the new node to the tree
    		if(sampledNode != currentNode){
    			sampledNode.parent = currentNode;
    			try{
    	    		kd.insert(sampledNode.getValue(), sampledNode);
    	    		}
    	    		catch (Exception e) {
    	    		    System.err.println(e);
    	    		}
    			//set currentNode as newest node added, so we can check if we can reach the destination
    			currentNode = sampledNode;
    			//
    		}
    	}
    	
    	//after searching, we update the path
    	RRTNode curNode = destNode;
    	int i = 0;
    	while(curNode != rootNode){
    		ItemPosition pathPoint = new ItemPosition(start.name+"to"+destination.name+Integer.toString(i), curNode.position.x, curNode.position.y, 0);
    		path.addFirst(pathPoint);
    		curNode = curNode.parent;
    		i++;
    	}
    	path.addFirst(destination);
    }
    
    
    public LinkedList<ItemPosition> findPath(ItemPosition destination, ItemPosition start, int K, ObstacleList obsList, int xRange, int yRange){
    	findRoute(destination, start, K, obsList, xRange, yRange);
    	return path;
    }

	private RRTNode toggle(RRTNode currentNode, RRTNode sampledNode, ObstacleList obsList) {
		// toggle function deals with constrains by the environment as well as robot systems. 
		// It changes sampledNode to some point alone the line of sampledNode and currentNode so that no obstacles are in the middle
		// In other words, it changes sampledNode to somewhere alone the line where robot can reach
		
		// we can add robot system constraints later
		
		RRTNode toggleNode = new RRTNode(sampledNode);
		int tries = 0;
		// try 20 times, which will shorten it to 0.00317 times the original path length
		// smaller tries might make integer casting loop forever
		while((obsList.badPath(currentNode, toggleNode) && (tries < 20)))
		{
			//move 1/4 toward current
			toggleNode.position.x = (int) ((toggleNode.position.x + currentNode.position.x)/(1.5));
			toggleNode.position.y = (int) ((toggleNode.position.y + currentNode.position.y)/(1.5));
			tries ++;
		}
		//return currentNode if toggle failed
		if(tries >= 19)
			return currentNode;
		else
		return toggleNode;
	}

}
