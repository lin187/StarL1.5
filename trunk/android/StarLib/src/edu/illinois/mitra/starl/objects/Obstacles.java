package edu.illinois.mitra.starl.objects;

import java.util.*;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;

import edu.illinois.mitra.starl.motion.RRTNode;
/**
 * The obstacle is defined here
 * Each obstacle is a polygon, the list of points should construct a closed shape
 * @author Yixiao Lin
 * @version 1.0
 */
public class Obstacles {
	public Vector<Point> obstacle;
	public long timeFrame;
	public boolean hidden;
	public boolean grided;
	//time that the obstacle will stay in the system, in milliseconds
	//if -1, it is a static obstacle
	//once zero, it will be removed from the obsList

	public Obstacles(){
		obstacle = new Vector<Point>(4, 3); 
	}

	public Obstacles(Vector<Point> obstacle1) {
		obstacle = obstacle1;
	}
	
	public Obstacles(Obstacles original){
		obstacle = new Vector<Point>(4, 3); 
		for(int i = 0; i< original.obstacle.size(); i++){
			add(original.obstacle.get(i).x, original.obstacle.get(i).y);
		}
		grided = original.grided;
		timeFrame = original.timeFrame;
		hidden = original.hidden;
	}
	
	//method for adding unknown obstacles
	public Obstacles(int x, int y){
		obstacle = new Vector<Point>(4, 3);
		add(x,y) ;
		grided = false;
		timeFrame = -1;
	}
	
	public void add(int x, int y){
		Point temp = new Point(x,y);
		obstacle.add(temp) ;
	}
	
    /**
     * return a clone so the obstacles cannot be modified
     * TODO: check that this deep copies all the points too
     * @return
     */
    public Vector<java.awt.Point> getObstacleVector() {
        return (Vector<Point>)this.obstacle.clone();
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
		
			Point nextpoint = obstacle.firstElement();
			Point curpoint = obstacle.firstElement();
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
		
			Point curpoint = obstacle.firstElement();
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
		Point nextpoint = obstacle.firstElement();
		Point curpoint = obstacle.firstElement();
		double minDist = 100000;
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
	
	public Point getClosestPointOnSegment(int sx1, int sy1, int sx2, int sy2, int px, int py)
	  {
	    double xDelta = sx2 - sx1;
	    double yDelta = sy2 - sy1;
	    double u = ((px - sx1) * xDelta + (py - sy1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

	    final Point closestPoint;
	    if (u < 0)
	    {
	      closestPoint = new Point(sx1, sy1);
	    }
	    else if (u > 1)
	    {
	      closestPoint = new Point(sx2, sy2);
	    }
	    else
	    {
	      closestPoint = new Point((int) Math.round(sx1 + u * xDelta), (int) Math.round(sy1 + u * yDelta));
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
		switch(obstacle.size()){
			case 1 : 
				Point leftBottom1 = new Point(obstacle.firstElement().x - ((obstacle.firstElement().x)% a), obstacle.firstElement().y - ((obstacle.firstElement().y)% a));
				Point rightBottom1 = new Point((leftBottom1.x + a), leftBottom1.y);
				Point rightTop1 = new Point((leftBottom1.x + a), (leftBottom1.y + a));
				Point leftTop1 = new Point((leftBottom1.x), (leftBottom1.y + a));
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
				
				Point leftBottom2 = new Point(min_x, min_y);
				Point rightBottom2 = new Point(max_x, min_y);
				Point rightTop2 = new Point(max_x, max_y);
				Point leftTop2 = new Point(min_x, max_y);
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
				
				Point leftBottom3 = new Point(min_x3, min_y3);
				Point rightBottom3 = new Point(max_x3, min_y3);
				Point rightTop3 = new Point(max_x3, max_y3);
				Point leftTop3 = new Point(min_x3, max_y3);
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