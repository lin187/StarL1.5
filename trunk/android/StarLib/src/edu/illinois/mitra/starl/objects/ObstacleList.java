package edu.illinois.mitra.starl.objects;

import java.util.*;

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
		for(int i=1; i<=ObList.size(); i++){
			if(ObList.elementAt(i) != null){
				check = check || ObList.elementAt(i).checkCross(destination, current);
			}
			else
			break;
		}
		return check;
	}
	
}
