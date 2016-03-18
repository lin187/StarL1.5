package edu.illinois.mitra.starl.objects;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.util.*;

import edu.illinois.mitra.starl.motion.RRTNode;
/**
 * The obstacle is defined here
 * Each obstacle is a polygon, the list of points should construct a closed shape
 * @author Yixiao Lin
 * @version 1.0
 */
public class Obstacles {
	public Vector<Point3d> obstacle;
	public int height;
	public long timeFrame;
	public boolean hidden;
	public boolean grided;
	//time that the obstacle will stay in the system, in milliseconds
	//if -1, it is a static obstacle
	//once zero, it will be removed from the obsList

	public Obstacles(){
		obstacle = new Vector<Point3d>(4, 3); 
	}

	public Obstacles(Vector<Point3d> obstacle1) {
		obstacle = obstacle1;
	}
	
	public Obstacles(Obstacles original){
		obstacle = new Vector<Point3d>(4, 3); 
		for(int i = 0; i< original.obstacle.size(); i++){
			add(original.obstacle.get(i).x, original.obstacle.get(i).y);
		}
		grided = original.grided;
		timeFrame = original.timeFrame;
		hidden = original.hidden;
		height = -1;
	}
	
	//method for adding unknown obstacles
	public Obstacles(int x, int y){
		obstacle = new Vector<Point3d>(4, 3);
		add(x, y, 0) ;
		grided = false;
		timeFrame = -1;
		height = -1;
	}
	
	public Obstacles(int x, int y, int z){
		obstacle = new Vector<Point3d>(4, 3);
		add(x, y, z) ;
		grided = false;
		timeFrame = -1;
		height = -1;
	}
	
	public void add(int x, int y){
		add(x, y, 0);
	}
	
	public void add(int x, int y, int z){
		Point3d temp = new Point3d(x,y, z);
		obstacle.add(temp) ;
	}
	
    /**
     * return a clone so the obstacles cannot be modified
     * TODO: check that this deep copies all the points too
     * @return
     */
    public Vector<Point3d> getObstacleVector() {
        return (Vector<Point3d>)this.obstacle.clone();
    }
	
	/**
	 * check if line from current to destination has intersection with any part of the object
	 * return true if cross
	 * @param destination
	 * @param current
	 * @return
	 */
	public boolean checkCross(ItemPosition destination, ItemPosition current){
		boolean check = false;
		Line2D.Double path = new Line2D.Double(destination.x, destination.y, current.x, current.y);
		Line2D.Double obSeg = new Line2D.Double();
		for(int i=0; i<obstacle.size(); i++){
			for(int j=0; j<obstacle.size(); j++){
				if(i != j)
				{	
					if(obstacle.elementAt(i) != null){
						obSeg.setLine(obstacle.elementAt(i).getX(), obstacle.elementAt(i).getY(), obstacle.elementAt(j).getX(), obstacle.elementAt(j).getY());
						check = check || obSeg.intersectsLine(path);
					}
					else
						break;
				}
			}
		}
		return check;
	}
	
	/**
	 * check if the itemPosotion destination is reachable by robots
	 * return true if robot can reach it
	 * @param destination
	 * @param radius
	 * @return
	 */
	
	public boolean validItemPos(ItemPosition destination, double radius){
		
		if(destination == null)
			return false;
		if(obstacle.size() == 0)
			return true;
		
			Point3d nextpoint = obstacle.firstElement();
			Point3d curpoint = obstacle.firstElement();
			Line2D.Double segment;
			int[] x = new int[obstacle.size()];
			int[] y = new int[obstacle.size()];
			
			for(int j = 0; j < obstacle.size() ; j++){
				curpoint = obstacle.get(j);
				if (j == obstacle.size() -1){
					nextpoint = obstacle.firstElement();
				}
				else{
					nextpoint = obstacle.get(j+1);
				}
				segment = new Line2D.Double(curpoint.x,curpoint.y,nextpoint.x,nextpoint.y);
				x[j] = curpoint.x;
				y[j] = curpoint.y;
				if((segment.ptSegDist(destination.x,destination.y) < radius)){
					return false;
				}
				
			}
			Polygon obspoly = new Polygon(x,y,obstacle.size());
			if(obspoly.contains(destination.x, destination.y))
				return false;
			else
				return true;
	}
	
	/**
	 * check if the itemPosotion destination is reachable by robots
	 * return true if robot can reach it
	 * @param destination
	 * @return
	 */
	public boolean validItemPos(ItemPosition destination){
		
			Point3d curpoint = obstacle.firstElement();
			int[] x = new int[obstacle.size()];
			int[] y = new int[obstacle.size()];
			
			for(int j = 0; j < obstacle.size() ; j++){
				curpoint = obstacle.get(j);
				x[j] = curpoint.x;
				y[j] = curpoint.y;
				
			}
			Polygon obspoly = new Polygon(x,y,obstacle.size());
			if(obspoly.contains(destination.x, destination.y))
				return false;
			else
				return true;
	}
	
	public double findMinDist(RRTNode destNode, RRTNode currentNode){
		Point3d nextpoint = obstacle.firstElement();
		Point3d curpoint = obstacle.firstElement();
		double minDist = Double.MAX_VALUE;
		Line2D.Double current;
		current = new Line2D.Double(destNode.position.x,destNode.position.y,currentNode.position.x,currentNode.position.y);
		Line2D.Double segment;
		
		for(int j = 0; j < obstacle.size() ; j++){
			curpoint = obstacle.get(j);
			if (j == obstacle.size() -1){
				nextpoint = obstacle.firstElement();
			}
			else{
				nextpoint = obstacle.get(j+1);
			}
			segment = new Line2D.Double(curpoint.x,curpoint.y,nextpoint.x,nextpoint.y);
			double dist1 = segment.ptSegDist(current.x1, current.y1);
			double dist2 = segment.ptSegDist(current.x2, current.y2);
			double dist3 = current.ptSegDist(segment.x1, segment.y1);
			double dist4 = current.ptSegDist(segment.x2, segment.y2);
			double temp1 = Math.min(dist1, dist2);
			double temp2 = Math.min(dist3, dist4);
			double minDistNow = Math.min(temp1, temp2);
			minDist = Math.min(minDistNow, minDist);
		}
		return minDist;
	}
	
	public Point3d getClosestPointOnSegment(int sx1, int sy1, int sx2, int sy2, int px, int py)
	  {
	    double xDelta = sx2 - sx1;
	    double yDelta = sy2 - sy1;
	    double u = ((px - sx1) * xDelta + (py - sy1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

	    final Point3d closestPoint;
	    if (u < 0)
	    {
	      closestPoint = new Point3d(sx1, sy1);
	    }
	    else if (u > 1)
	    {
	      closestPoint = new Point3d(sx2, sy2);
	    }
	    else
	    {
	      closestPoint = new Point3d((int) Math.round(sx1 + u * xDelta), (int) Math.round(sy1 + u * yDelta));
	    }

	    return closestPoint;
	  }
	  
	/**
	 * gridify the map, make the obstacle map grid like. For any Grid that contains obstacle, that grid is considered an obstacle
	 * @param a
	 */
	public void ToGrid(int a){
		if(grided){
			return;
		}
		//System.out.println(obstacle);
		switch(obstacle.size()){
			case 1 : 
				Point3d leftBottom1 = new Point3d(obstacle.firstElement().x - ((obstacle.firstElement().x)% a), obstacle.firstElement().y - ((obstacle.firstElement().y)% a));
				Point3d rightBottom1 = new Point3d((leftBottom1.x + a), leftBottom1.y);
				Point3d rightTop1 = new Point3d((leftBottom1.x + a), (leftBottom1.y + a));
				Point3d leftTop1 = new Point3d((leftBottom1.x), (leftBottom1.y + a));
				obstacle.removeAllElements();
				obstacle.add(leftBottom1);
				obstacle.add(rightBottom1);
				obstacle.add(rightTop1);
				obstacle.add(leftTop1);
			break;	
			case 2 :
				int min_x = Math.min(obstacle.firstElement().x, obstacle.get(1).x);
				min_x = min_x - (min_x%a);
				int max_x = Math.max(obstacle.firstElement().x, obstacle.get(1).x);
				max_x = max_x - (max_x%a) +a;
				int min_y = Math.min(obstacle.firstElement().y, obstacle.get(1).y);
				min_y = min_y - (min_y%a);
				int max_y = Math.max(obstacle.firstElement().y, obstacle.get(1).y);
				max_y = max_y - (max_y%a) +a;
				
				Point3d leftBottom2 = new Point3d(min_x, min_y);
				Point3d rightBottom2 = new Point3d(max_x, min_y);
				Point3d rightTop2 = new Point3d(max_x, max_y);
				Point3d leftTop2 = new Point3d(min_x, max_y);
				obstacle.removeAllElements();
				obstacle.add(leftBottom2);
				obstacle.add(rightBottom2);
				obstacle.add(rightTop2);
				obstacle.add(leftTop2);
			break; 
			case 4 :
				int min_x3 = Math.min(obstacle.firstElement().x, obstacle.get(1).x);
				min_x3 = Math.min(min_x3, obstacle.get(2).x);
				min_x3 = Math.min(min_x3, obstacle.get(3).x);
				min_x3 = min_x3 - (min_x3%a);
				
				int max_x3 = Math.max(obstacle.firstElement().x, obstacle.get(1).x);
				max_x3 = Math.max(max_x3, obstacle.get(2).x);
				max_x3 = Math.max(max_x3, obstacle.get(3).x);
				max_x3 = max_x3 - (max_x3%a) +a;
				
				int min_y3 = Math.min(obstacle.firstElement().y, obstacle.get(1).y);
				min_y3 = Math.min(min_y3, obstacle.get(2).y);
				min_y3 = Math.min(min_y3, obstacle.get(3).y);
				min_y3 = min_y3 - (min_y3%a);
				
				int max_y3 = Math.max(obstacle.firstElement().y, obstacle.get(1).y);
				max_y3 = Math.max(max_y3, obstacle.get(2).y);
				max_y3 = Math.max(max_y3, obstacle.get(3).y);
				max_y3 = max_y3 - (max_y3%a) +a;
				
				Point3d leftBottom3 = new Point3d(min_x3, min_y3);
				Point3d rightBottom3 = new Point3d(max_x3, min_y3);
				Point3d rightTop3 = new Point3d(max_x3, max_y3);
				Point3d leftTop3 = new Point3d(min_x3, max_y3);
				obstacle.removeAllElements();
				obstacle.add(leftBottom3);
				obstacle.add(rightBottom3);
				obstacle.add(rightTop3);
				obstacle.add(leftTop3);
				break;
			default :
				System.out.println("not an acceptable demension of "+obstacle.size() + " to be grided");
			break;
		}
		grided = true;
		
	}
}