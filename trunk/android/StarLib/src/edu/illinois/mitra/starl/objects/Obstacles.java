package edu.illinois.mitra.starl.objects;

import java.util.*;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;

public class Obstacles {
	public Vector<Point> obstacle;

	public Obstacles(){
		obstacle = new Vector<Point>(4, 3); 
	}

	public Obstacles(Vector<Point> obstacle1) {
		obstacle = obstacle1;
	}
	public void add(int x, int y){
		Point temp = new Point(x,y);
		obstacle.add(temp) ;
	}
	
	public boolean checkCross(ItemPosition destination, ItemPosition current){
		//check if line from current to destination has intersection with any part of the object
		//return true if cross
		
		boolean check = false;
		Line2D.Double path = new Line2D.Double(destination.x, destination.y, current.x, current.y);
		Line2D.Double obSeg = new Line2D.Double();
		for(int i=1; i<=obstacle.size(); i++){
			for(int j=1; j<=obstacle.size(); j++){
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
	
	public boolean validItemPos(ItemPosition destination, double radius){
		//check if the itemPosotion destination is reachable by robots
		//return true if robot can reach it
		
		
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
	
	public boolean validItemPos(ItemPosition destination){
		//check if the itemPosotion destination is reachable by robots
		//return true if robot can reach it
		
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
}