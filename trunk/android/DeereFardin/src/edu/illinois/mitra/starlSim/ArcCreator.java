package edu.illinois.mitra.starlSim.simapps.deere_fardin ; 

import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class ArcCreator 
{
	
	/**
	 * Create a new path which is smooth from an existing path which passes through a detour point
	 * @param path the old path
	 * @param detourPoint the point to pass through
	 * @param anchorDistance the anchor point distance when defining the Bezier curve
	 * @param seperation the (maximum) separation desired between the output points
	 * @return a list of new waypoints which starts and ends in the same points as 'path', but goes through 'detourPoint'
	 */	
	public static ArrayList<WayPoint>  createNewPath(ArrayList<WayPoint> path, Point2D.Double detourPoint, 
			double anchorDistance, double seperation)
	{
		double sepSq = seperation * seperation;
		ArrayList<WayPoint> newPath = new ArrayList<WayPoint>();
		newPath.add(path.get(0));  //first point is fixed
		
		// define the cubic curve detourPath1 from startPoint to detourPoint
		Point2D.Double startPoint = new Point2D.Double(path.get(0).x, path.get(0).y);
		Point2D.Double dirPoint1 = new Point2D.Double(path.get(1).x, path.get(1).y);
		Point2D.Double dirPoint2 = new Point2D.Double();
		dirPoint2.setLocation(detourPoint.x + startPoint.x - dirPoint1.x, 
				detourPoint.y + startPoint.y - dirPoint1.y);
		
		CubicCurve2D.Double detourPath1 = getCurve(startPoint, detourPoint, 
				dirPoint1, dirPoint2, anchorDistance);
		
		// divide detourPath into segments to satisfy the separation constraint
		ArrayList<CubicCurve2D.Double> CurveSegs1 = subdivideCurve(detourPath1, sepSq);
		
		// compute the reunion path detourpath2 by iterative trying
		dirPoint1.setLocation(2 * detourPoint.x - dirPoint2.x, 2 * detourPoint.y - dirPoint2.y);
		Point2D.Double reunionPoint = new Point2D.Double();
		CubicCurve2D.Double detourPath2 = new CubicCurve2D.Double();
		ArrayList<CubicCurve2D.Double> CurveSegs2 = new ArrayList<CubicCurve2D.Double>();
		
		int CS1size = CurveSegs1.size();
		
		// logic to find the reunion point: find the closest point on the path to the detour point
		// and multiply it's index in the path by two to get the reunion point
		
		int reunionIndex = 2 * getClosestPointIndex(path, detourPoint); // STAN here
		
		if (reunionIndex > path.size() - 2)
			reunionIndex = path.size() - 2;
		
		reunionPoint.setLocation(path.get(reunionIndex).x, path.get(reunionIndex).y);
		
		dirPoint2.setLocation(2*reunionPoint.x - path.get(reunionIndex+1).x, 
				2*reunionPoint.y - path.get(reunionIndex+1).y);
		detourPath2.setCurve(getCurve(detourPoint, reunionPoint, 
				dirPoint1, dirPoint2, anchorDistance));
		CurveSegs2 = subdivideCurve(detourPath2, sepSq);
		
		// write newPath with the concatenation of CurveSegs1, CurveSegs2 and the last part of path.
		Point2D.Double SegEnd = new Point2D.Double();
		int cyclenth = path.get(1).time - path.get(0).time; 
		int time;
		for(int i = 0; i<CurveSegs1.size(); i++){
			if(i + 1 < path.size()){
				time = path.get(i+1).time;
			}else{
				time = newPath.get(i).time + cyclenth;
			}
			SegEnd.setLocation(CurveSegs1.get(i).getP2());
			newPath.add( new WayPoint((int)SegEnd.x, (int)SegEnd.y, time) ) ;
		}
		
		for(int i = 0; i<CurveSegs2.size(); i++){
			if(i+CS1size + 1 < path.size()){
				time = path.get(i+1+CS1size).time;
			}else{
				time = newPath.get(i+CS1size).time + cyclenth;
			}
			SegEnd.setLocation(CurveSegs2.get(i).getP2());
			newPath.add( new WayPoint((int)SegEnd.x, (int)SegEnd.y, time) ) ;
		}
		
		// last part of path
		
		// find closest index
		WayPoint last = newPath.get(newPath.size() - 1);
		WayPoint cur = path.get(0);
		
		int closestIndex = 0;
		double dx = last.x - cur.x;
		double dy = last.y - cur.y;
		double closestDistSq = dx * dx + dy * dy;
		
		for (int i = 1; i < path.size(); ++i)
		{
			cur = path.get(i);
			
			dx = last.x - cur.x;
			dy = last.y - cur.y;
			double distSq = dx * dx + dy * dy;
			
			if (distSq < closestDistSq)
			{
				closestDistSq = distSq;
				closestIndex = i;
			}
		}
		
		for(int i = closestIndex; i<path.size(); i++)
		{
			newPath.add(path.get(i));
		}

		
		return newPath;
	}
	
	private static int getClosestPointIndex(ArrayList<WayPoint> path, Point2D.Double p) 
	{
		// start at index 1 since we never want the reunion point to be index 0
		int rv = 1;
		double minDistSq = p.distanceSq(path.get(0).x, path.get(0).y);
		
		for (int i = 2; i < path.size() - 1; ++i)
		{
			WayPoint wp = path.get(i);
			
			double distSq = p.distanceSq(wp.x, wp.y);
			
			if (distSq < minDistSq)
			{
				minDistSq = distSq;
				rv = i;
			}
		}
		
		return rv;
	}

	private static CubicCurve2D.Double getCurve(Point2D.Double P1, Point2D.Double P2, 
			Point2D.Double C1, Point2D.Double C2, double anchorDistance){
		/**
		 * Create a cubic curve from P0 to P1 which is tangent to P1C1 at P1 and C2P2 at P2 with assigned anchor distance. 
		 * @param starting point
		 * @param ending point
		 * @param direction at P1
		 * @param direction at P2
		 * @param anchorDistance the anchor point distance when defining the Bezier curve
		 * @return a cubic curve
		 */
		CubicCurve2D.Double CCV = new CubicCurve2D.Double();
		double[] anchorOffset1 = new double[2];
		double[] anchorOffset2 = new double[2];
		anchorOffset1[0] = (C1.x - P1.x) / P1.distance(C1) * anchorDistance;
		anchorOffset1[1] = (C1.y - P1.y) / P1.distance(C1) * anchorDistance;
		anchorOffset2[0] = (C2.x - P2.x) / P2.distance(C2) * anchorDistance;
		anchorOffset2[1] = (C2.y - P2.y) / P2.distance(C2) * anchorDistance;
		
		
		Point2D.Double ctrl1 = new Point2D.Double(P1.x + anchorOffset1[0],
				P1.y + anchorOffset1[1]);
		Point2D.Double ctrl2 = new Point2D.Double(P2.x + anchorOffset2[0],
				P2.y + anchorOffset2[1]);
		CCV.setCurve(P1, ctrl1, ctrl2, P2);
		return CCV;
	}
	
	
	private static ArrayList<CubicCurve2D.Double> subdivideCurve(CubicCurve2D.Double curve, double sepSq){
		/**
		 * Divide a cubic curve into segments each of which is length-bounded
		 * @param the original curve
		 * @param the bound in the square of the distance between the starting and ending points of each segments
		 */
		ArrayList<CubicCurve2D.Double> CurveSegs = new ArrayList<CubicCurve2D.Double>();
		CubicCurve2D.Double leftSeg = new CubicCurve2D.Double();
		CubicCurve2D.Double rightSeg = new CubicCurve2D.Double();
		CubicCurve2D.Double currentSeg = new CubicCurve2D.Double();
		CurveSegs.add(curve);
		
		int i = 0;
		while(i < CurveSegs.size())
		{
			currentSeg.setCurve(CurveSegs.get(i));
			if(currentSeg.getP1().distanceSq(currentSeg.getP2()) > sepSq){
				currentSeg.subdivide(leftSeg, rightSeg); // divide currentSeg into two
				CurveSegs.remove(i); // replace currentSeg with his divisions
				CurveSegs.add(i, new CubicCurve2D.Double(rightSeg.x1, rightSeg.y1, rightSeg.ctrlx1, 
						rightSeg.ctrly1, rightSeg.ctrlx2, rightSeg.ctrly2, rightSeg.x2, rightSeg.y2));
				CurveSegs.add(i, new CubicCurve2D.Double(leftSeg.x1, leftSeg.y1, leftSeg.ctrlx1, 
						leftSeg.ctrly1, leftSeg.ctrlx2, leftSeg.ctrly2, leftSeg.x2, leftSeg.y2));
			}
			else{
				i ++;
			}				
		}
		return CurveSegs;
	}
	
	public ArcCreator()
	{
		// test function
		
		// create the old path
		ArrayList <WayPoint> oldPath = new ArrayList <WayPoint>();
		
		for (WayPoint wp : new WayPoint[]{
				new WayPoint(1000, 500, 2000),
				new WayPoint(1500, 500, 3000),
				new WayPoint(2000, 500, 4000),
				new WayPoint(2500, 500, 5000),
				new WayPoint(3000, 500, 6000),
				new WayPoint(3500, 500, 7000),
				new WayPoint(4000, 500, 8000),
				new WayPoint(4500, 500, 9000),
				})
		{
			oldPath.add(wp);
		}
		
		Point2D.Double detourPoint = new Point2D.Double(617.0, 1123.0);
		
		double ANCHOR_DISTANCE = 300; 
		double SEPARATION = 500; // this term should be greater than the separation of old path.
		
		ArrayList <WayPoint> newPath = createNewPath(oldPath, detourPoint, ANCHOR_DISTANCE, SEPARATION);
		
		// print paths
	//	System.out.println("Old Path: " + oldPath);
	//	System.out.println("\nNew Path: " + newPath);
	}

} 
	

