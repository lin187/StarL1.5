package edu.illinois.mitra.starlSim.simapps;

import java.util.List;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class CircleMotion extends LogicThread {

	public CircleMotion(GlobalVarHolder gvh) {
		super(gvh);
	}
	
	private enum Stage {INIT,GO,WAIT_TO_ARRIVE,DONE};
	private int currentPoint = 0;
	private static final int CIRCLE_SIZE = 35;
	private Stage stage = Stage.INIT;
	
	private static final ItemPosition[] CIRCLE = new ItemPosition[CIRCLE_SIZE];
	static {
		int radius = 1000;
		int centerX = 1500;
		int centerY = 1500;
		for(int i = 0; i < CIRCLE_SIZE; i ++) {
			double rads = Math.PI*2*i/(double)CIRCLE_SIZE;
			CIRCLE[i] = new ItemPosition("pt"+i, (int)(radius*Math.cos(rads)+centerX), (int)(radius*Math.sin(rads)+centerY), 0);
		}
	}
	
	@Override
	public List<Object> callStarL() {
		while(true) {
			switch(stage) {
			case INIT:
				currentPoint = 0;
				stage = Stage.GO;
			case GO:
				goToPoint(currentPoint++);
				stage = Stage.WAIT_TO_ARRIVE;
				break;
			case WAIT_TO_ARRIVE:
				if(!gvh.plat.moat.inMotion)
					if(currentPoint == CIRCLE_SIZE)
						stage = Stage.DONE;
					else
						stage = Stage.GO;
				break;
			case DONE:
				return null;
			}
			gvh.sleep(100);
		}
	}
	
	private void goToPoint(int point) {
		gvh.plat.moat.goTo(CIRCLE[point]);
	}

}
