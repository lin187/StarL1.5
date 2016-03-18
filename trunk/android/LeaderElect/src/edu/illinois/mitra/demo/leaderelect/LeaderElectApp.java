package edu.illinois.mitra.demo.leaderelect;

import java.util.List;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.DSMMultipleAttr;
import edu.illinois.mitra.starl.functions.GroupSetMutex;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.DSM;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class LeaderElectApp extends LogicThread {
	private MutualExclusion mutex;
	private DSM dsm;
	int robotIndex;
	public ItemPosition position;
	private boolean wait1 = false;
	public int LeaderId = -1;
	public boolean elected = false;
	private boolean voted = false;
	public LeaderElectApp(GlobalVarHolder gvh){
		super(gvh);
		robotIndex = Integer.parseInt(name.replaceAll("[^0-9]", ""));
		mutex = new GroupSetMutex(gvh, 0);
		//mutex = new SingleHopMutualExclusion(1, gvh, "bot0");
		dsm = new DSMMultipleAttr(gvh);
	}
	@Override
	public List<Object> callStarL() {
		position = gvh.gps.getMyPosition();
		dsm.createMW("leader", -1);
		while(true){
			sleep(100);
			if(!wait1){
				mutex.requestEntry(0);
				wait1 = true;
			}
			if(!voted && mutex.clearToEnter(0)){
				voted = true;
				dsm.put("leader", "*", robotIndex);	
				//mutex.exit(0);
				//do not exit unless giving up being a leader
			}
			if(!elected){
				if(Integer.parseInt(dsm.get("leader","*")) != -1){
					LeaderId = Integer.parseInt(dsm.get("leader","*"));
				}
			}
		}
	}

	@Override
	protected void receive(RobotMessage m) {
	}
}
