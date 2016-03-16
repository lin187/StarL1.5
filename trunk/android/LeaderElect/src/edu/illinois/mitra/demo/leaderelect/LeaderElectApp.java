package edu.illinois.mitra.demo.leaderelect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.DSMMultipleAttr;
import edu.illinois.mitra.starl.functions.GroupSetMutex;
import edu.illinois.mitra.starl.functions.SingleHopMutualExclusion;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.DSM;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class LeaderElectApp extends LogicThread {
	private MutualExclusion mutex;
	private DSM dsm;
	int robotIndex;
	public ItemPosition position;
	private int numBots = 0;
	private boolean wait1 = false;
	private boolean wait2 = false;
	public int LeaderId = -1;
	public int numVotes = 0; 
	public int candidate = -1;
	private boolean elected = false;
	private boolean voted = false;
	private boolean added = false;
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
			while(true){
				sleep(100);
				if(!voted && ((dsm.get("candidate","*") == null)|| (robotIndex > Integer.parseInt(dsm.get("candidate","*"))))){
					if(!wait1){
						numBots = gvh.gps.get_robot_Positions().getNumPositions();
						mutex.requestEntry(0);
						wait1 = true;
					}
					if(mutex.clearToEnter(0)){
						voted = true;
						dsm.put("candidate", "*", robotIndex);	
						mutex.exit(0);
					}
					continue;
				}
				if(!voted && robotIndex < Integer.parseInt(dsm.get("candidate","*"))){
					voted = true;
					continue;
				}

				if(!added){
					if(!wait2){
						numBots = gvh.gps.get_robot_Positions().getNumPositions();
						mutex.requestEntry(0);
						wait2 = true;
					}
					if(mutex.clearToEnter(0)){
						added = true;
						if(dsm.get("numVotes","*") == null){
							dsm.put("numVotes","*", 0);
						}
						numVotes = Integer.parseInt(dsm.get("numVotes", "*"));
						dsm.put("numVotes", "*", numVotes + 1);	
						mutex.exit(0);
					}
					continue;
				
				}
				if(Integer.parseInt(dsm.get("numVotes","*")) == numBots && !elected) {
					elected = true;
					if(dsm.get("candidate","*") == null){
							dsm.put("candidate","*", 0);
					}
					candidate = (Integer.parseInt(dsm.get("candidate","*")));
					dsm.put("LeaderId","*", candidate);

				}

				if(elected){
					LeaderId = Integer.parseInt(dsm.get("LeaderId","*"));
					continue;
				}
			
			}
}
	
	@Override
	protected void receive(RobotMessage m) {
	}
}
