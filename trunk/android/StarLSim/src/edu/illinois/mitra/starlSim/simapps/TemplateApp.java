package edu.illinois.mitra.starlSim.simapps;

import java.util.List;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;

public class TemplateApp extends LogicThread {

	private enum STAGE { START, DO_STUFF, DONE }
	private STAGE stage = STAGE.START;

	
	public TemplateApp(GlobalVarHolder gvh) {
		super(gvh);
	}
	
	@Override
	public List<Object> callStarL() {
		while(true) {
			switch(stage) {
			case START:
				stage = STAGE.DO_STUFF;
				break;
			case DO_STUFF:
				// TODO: Do stuff here
				stage = STAGE.DONE;
				break;
			case DONE:
				return returnResults();
			}
			gvh.sleep(100);
		}
	}
}
