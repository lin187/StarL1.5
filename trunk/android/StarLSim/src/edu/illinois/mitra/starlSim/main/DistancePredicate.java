package edu.illinois.mitra.starlSim.main;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;
import edu.illinois.mitra.starl.objects.TraceWriter;

public class DistancePredicate extends TraceWriter implements Observer {
	
	private int distance;
	
	public DistancePredicate(String filename, String dir, int distance) {
		super(filename, dir);
		this.distance = distance;
		super.writeTag("distance", Integer.toString(distance));
		super.open("truth");
	}

	@Override
	public void update(Observable o, Object arg) {
		ArrayList<ItemPosition> pos = ((PositionList) arg).getList();
		for(ItemPosition p : pos) {
			for(ItemPosition q : pos) {
				if(!p.equals(q) && p.distanceTo(q) < distance) {
					super.open("violation");
					super.writeTimeTag();
					super.close("violation");
					return;
				}
			}
		}
	}

	@Override
	public void close() {
		super.close("truth");
		super.close();
	}

}
