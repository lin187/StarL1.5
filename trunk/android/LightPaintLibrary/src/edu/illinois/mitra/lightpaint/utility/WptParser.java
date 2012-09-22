package edu.illinois.mitra.lightpaint.utility;

import java.util.HashSet;
import java.util.Set;

import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public class WptParser {

	public static Set<ImageEdge> parseWaypoints(GlobalVarHolder gvh) {
		Set<ImageEdge> retval = new HashSet<ImageEdge>();
		int idx = 0;
		PositionList wpt = gvh.gps.getWaypointPositions();
		ItemPosition pos;
		while((pos = wpt.getPositionRegex(idx + "-A_[0-9]+")) != null) {
			ImagePoint a = new ImagePoint(pos.getX(), pos.getY(), pos.getAngle(), getSizeFromName(pos.getName()));
			pos = wpt.getPositionRegex(idx +"-B_[0-9]+");
			if(pos == null)
				return retval;
			ImagePoint b = new ImagePoint(pos.getX(), pos.getY(), pos.getAngle(), getSizeFromName(pos.getName()));
			retval.add(new ImageEdge(a,b));
			idx ++;
		}
		return retval;
	}
	
	private static int getSizeFromName(String name) {
		String[] parts = name.split("_");
		return Integer.parseInt(parts[1]);
	}
}

