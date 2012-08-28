package edu.illinois.mitra.starlSim.simapps.deere_fardin;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class CurveGenerator 
{
	public static ArrayList<WayPoint> createNewPath(ArrayList<WayPoint> path, Point2D.Double detourPoint, 
			double anchorDistance, double seperation)
	{
		System.out.println("path = {");
		
		for (WayPoint wp : path)
		{
			System.out.println("new WayPoint(" + wp.x + ", " + wp.y + ", " + wp.time + "),");
		}
		
		System.out.println("};");
		
		System.out.println("detourPoint = " + detourPoint);
		System.out.println("anchorDistance = " + anchorDistance);
		System.out.println("seperation = " + seperation);
		
		
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
		boolean done = false;
		boolean overflow;
		if(CS1size < path.size()-1){
			overflow = false;
			}else{
				overflow = true;
			}
		int i = CS1size;
		while(!done && !overflow){
			reunionPoint.setLocation(path.get(i).x, path.get(i).y);
			dirPoint2.setLocation(2*reunionPoint.x - path.get(i+1).x, 
					2*reunionPoint.y - path.get(i+1).y);
			detourPath2.setCurve(getCurve(detourPoint, reunionPoint, 
					dirPoint1, dirPoint2, anchorDistance));
			CurveSegs2 = subdivideCurve(detourPath2, sepSq);
			if(i <= CurveSegs2.size() + CS1size - 1){
				i++;
				CurveSegs2.removeAll(CurveSegs2);
			}else if(i >= path.size()-2){ // reunion path cannot catch up with the old path in the given time bound
				overflow = true;
			}else{
				done = true;
			}
		}
		
		
		// write newPath with the concatenation of CurveSegs1, CurveSegs2 and the last part of path.
		if(!overflow){
			Point2D.Double SegEnd = new Point2D.Double();
			for(i = 0; i<CurveSegs1.size(); i++){
				SegEnd.setLocation(CurveSegs1.get(i).getP2());
				newPath.add( new WayPoint((int)SegEnd.x, (int)SegEnd.y, path.get(i+1).time) ) ;
			}
			for(i = 0; i<CurveSegs2.size(); i++){
				SegEnd.setLocation(CurveSegs2.get(i).getP2());
				newPath.add( new WayPoint((int)SegEnd.x, (int)SegEnd.y, path.get(i+1+CS1size).time) ) ;
			}
			for(i = i+1+CS1size; i<path.size(); i++){
				newPath.add(path.get(i));
			}
		}else{
			newPath = null;
		}
		
		return newPath;
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
}
