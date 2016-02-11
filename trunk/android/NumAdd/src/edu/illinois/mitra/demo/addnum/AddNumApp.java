package edu.illinois.mitra.demo.addnum;

import java.util.List;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.DSMMultipleAttr;
import edu.illinois.mitra.starl.functions.GroupSetMutex;
import edu.illinois.mitra.starl.functions.SingleHopMutualExclusion;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.DSM;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class AddNumApp extends LogicThread {
	private MutualExclusion mutex;
	private DSM dsm;
	int robotIndex;
	private boolean added = false;
	private boolean wait = false;
	public int finalSum = 0;
	private int numBots = 0;
	public int numAdded = 0; 
	public int currentTotal = 0;
	public ItemPosition position;
	private boolean isFinal = false;
	public AddNumApp(GlobalVarHolder gvh){
		super(gvh);
		robotIndex = Integer.parseInt(name.substring(3,name.length()));
		mutex = new GroupSetMutex(gvh, 0);
		//mutex = new SingleHopMutualExclusion(1, gvh, "bot0");
		dsm = new DSMMultipleAttr(gvh);
	}
		@Override
		public List<Object> callStarL() {
			position = gvh.gps.getMyPosition();
			dsm.createMW("numAdded", 0);
			dsm.createMW("currentTotal", 0);
			while(true){
				sleep(100);
				//stage adding
				if(!added){
					if(!wait){	
						// get total number of robots
						numBots = gvh.gps.getPositions().getNumPositions();
						// call mutex and then wait
						mutex.requestEntry(0);
						wait = true;
					}
					if(mutex.clearToEnter(0)){
						System.out.println(name);
						added = true;
						numAdded = (Integer.parseInt(dsm.get("numAdded","*")));
						dsm.put("numAdded", "*", numAdded + 1);
						currentTotal = Integer.parseInt(dsm.get("currentTotal", "*"));
						dsm.put("currentTotal", "*", currentTotal + robotIndex);	
						mutex.exit(0);
					}
					continue;
				}
				//stage allAdded
				numAdded = Integer.parseInt(dsm.get("numAdded", "*"));
				if(!isFinal && Integer.parseInt(dsm.get("numAdded", "*")) == numBots){
					finalSum = Integer.parseInt(dsm.get("currentTotal", "*"));
					isFinal = true;
					System.out.print("Final Sum is: " + finalSum);
					continue;
				}
				//stage exit
				if(isFinal){
					// don't do anything, we can exit if we want	
					continue;
				}
			
			}
		}
	
	@Override
	protected void receive(RobotMessage m) {
	}
}