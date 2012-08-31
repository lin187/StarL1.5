package edu.illinois.mitra.starlSim.simapps;

import java.util.List;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class StraightLineMotion extends LogicThread {

	public StraightLineMotion(GlobalVarHolder gvh) {
		super(gvh);
	}

	private enum Stage {INIT,WAIT_TO_ARRIVE,DONE};
	private Stage stage = Stage.INIT;
	
	private static ItemPosition DESTINATION = new ItemPosition("Dest", 2000, 2000, 0);
	
	@Override
	public List<Object> callStarL() {
		while(true) {
			switch(stage) {
			case INIT:
				gvh.plat.moat.goTo(DESTINATION);
				stage = Stage.WAIT_TO_ARRIVE;
				break;
			case WAIT_TO_ARRIVE:
				if(!gvh.plat.moat.inMotion)
					stage = Stage.DONE;
				break;
			case DONE:
				return null;
			}
			gvh.sleep(100);
		}
	}

}
