package edu.illinois.mitra.test;

import java.util.LinkedList;

import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;

public class AppLogic extends LogicThread {

	private enum STAGE {START, GO, GONEXT, DONE};
	private STAGE stage = STAGE.START;
	
	private RobotMotion moat;
	
	public AppLogic(GlobalVarHolder gvh) {
		super(gvh);
		moat = gvh.plat.moat;
	}
	
	@Override
	public LinkedList<Object> call() throws Exception {
		System.out.println("LAUNCHING");
		while(true) {			
			Thread.sleep(50);
			switch(stage) {
			case START:
				stage = STAGE.GO;
				moat.goTo(gvh.gps.getWaypointPosition("middle"));
				gvh.log.i("MotionAutomaton", "Sent go command");
				break;
				
			case GO:
				if(!moat.inMotion) {
					moat.goTo(gvh.gps.getWaypointPosition("middle2"));
					stage = STAGE.GONEXT;
				}
				break;

			case GONEXT:
				if(!moat.inMotion) {
					stage = STAGE.DONE;
				}
				
			case DONE:
				return null;
			}
		}
	}
}
