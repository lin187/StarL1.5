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
		while((pos = wpt.getPosition(idx + "-A")) != null) {
			ImagePoint a = new ImagePoint(pos.getX(), pos.getY(), pos.getAngle());
			pos = wpt.getPosition(idx +"-B");
			if(pos == null)
				return retval;
			ImagePoint b = new ImagePoint(pos.getX(), pos.getY(), pos.getAngle());
			retval.add(new ImageEdge(a,b));
			idx ++;
		}
		return retval;
	}
}
