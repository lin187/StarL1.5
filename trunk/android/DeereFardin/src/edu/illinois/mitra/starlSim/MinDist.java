//import java.awt.geom.*;
package edu.illinois.mitra.starlSim;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;



class MinDist{

	public Point2D.Double P11;
	public Point2D.Double P12;
	public Point2D.Double P21;
	public Point2D.Double P22;
	
	public Line2D.Double L1;
	public Line2D.Double L2;
	
	private boolean IsIntersect;
	private double dist;
	
	MinDist(double x11, double y11, double x12, double y12, 
			double x21, double y21, double x22, double y22){
		
		this.P11 = new Point2D.Double(x11, y11);
		this.P12 = new Point2D.Double(x12, y12);
		this.P21 = new Point2D.Double(x21, y21);
		this.P22 = new Point2D.Double(x22, y22);
		
		this.L1 = new Line2D.Double(P11, P12);
		this.L2 = new Line2D.Double(P21, P22);
		
		this.IsIntersect = L1.intersectsLine(L2);
		this.dist = CalMinDist();
	}
	
	MinDist(Point2D.Double P11, Point2D.Double P12, 
			Point2D.Double P21, Point2D.Double P22){
		
		this.P11 = P11;
		this.P12 = P12;
		this.P21 = P21;
		this.P22 = P22;
		
		this.L1 = new Line2D.Double(P11, P12);
		this.L2 = new Line2D.Double(P21, P22);
		
		this.IsIntersect = L1.intersectsLine(L2);
		this.dist = CalMinDist();
	}
	
	MinDist(Line2D.Double L1, Line2D.Double L2){
		
		this.L1 = L1;
		this.L2 = L2;
		
		this.P11 = (Double) L1.getP1();
		this.P12 = (Double) L1.getP2();
		this.P21 = (Double) L2.getP1();
		this.P22 = (Double) L2.getP2();
		
		this.IsIntersect = L1.intersectsLine(L2);
		
		
		this.dist = CalMinDist();
	}
	
	
	public boolean doesIntersect(){
		
		return IsIntersect ; 
		
	}
	private double CalMinDist(){

		if (	P11.x == P12.x && P11.y == P12.y  && P21.x == P22.x && P21.y == P22.y ){
			dist = P11.distance(P22) ; 
		}
		
			
		else if(IsIntersect){
			dist = 0;
		}
		else{
			double min_sq = L1.ptSegDistSq(P21);
			min_sq = Math.min(min_sq, L1.ptSegDistSq(P22));
			min_sq = Math.min(min_sq, L2.ptSegDistSq(P11));
			min_sq = Math.min(min_sq, L2.ptSegDistSq(P12));
			
			dist = Math.sqrt(min_sq);
		}
		
		return dist;
	}
	
	public void setMinDist(Line2D.Double L1, Line2D.Double L2){
		this.L1 = L1;
		this.L2 = L2;
		
		this.P11 = (Double) L1.getP1();
		this.P12 = (Double) L1.getP2();
		this.P21 = (Double) L2.getP1();
		this.P22 = (Double) L2.getP2();
		
		this.IsIntersect = L1.intersectsLine(L2);
		this.dist = CalMinDist();
	}
	
	public double getMinDist(){
		return dist;
	}
	
	public double returnMinDit(){
		
		return this.dist ; 
	}
}
