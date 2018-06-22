package edu.illinois.mitra.starl.motion;

import java.util.Stack;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.interfaces.TrackedRobot;
import edu.illinois.mitra.starl.models.Model_3DR;
import edu.illinois.mitra.starl.models.Model_GhostAerial;
import edu.illinois.mitra.starl.models.Model_Mavic;
import edu.illinois.mitra.starl.models.Model_Phantom;
import edu.illinois.mitra.starl.models.Model_iRobot;
import edu.illinois.mitra.starl.models.Model_quadcopter;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;


public class ReachAvoid extends Thread implements Cancellable {
	protected static final String TAG = "ReachAvoid";
	protected static final String ERR = "Critical Error";
	/**
	 * default 5 retries using different parameters, can be set to other values 
	 */
	public int tries = 5;
	public int radius;
	protected enum STAGE_R {
		IDLE, PLAN, PICK, MOVE
	}
	protected GlobalVarHolder gvh;
	public boolean running;
	public RRTNode kdTree;
	ItemPosition start;
	ItemPosition dest;
	ObstacleList unsafe;
	ObstacleList planObs;
	public boolean activeFlag, doneFlag, failFlag;
	STAGE_R stage;
	public Stack<ItemPosition> pathStack;
	int counter;
	int xLower, xUpper, yLower, yUpper;
	
	
	public ReachAvoid(GlobalVarHolder gvh){
		this.gvh = gvh;
		stage = STAGE_R.IDLE;
		TrackedRobot model = gvh.plat.model;
		if(model instanceof Model_iRobot){
			radius = ((Model_iRobot) model).radius;
		}else if(model instanceof Model_quadcopter){
			radius = 110 + ((Model_quadcopter) model).radius;
		}else if(model instanceof Model_GhostAerial){
			radius = 110 + ((Model_GhostAerial) model).radius;
		}else if(model instanceof Model_Mavic){
			radius = 110 + ((Model_Mavic) model).radius;
		}else if(model instanceof Model_Phantom){
			radius = 110 + ((Model_Phantom) model).radius;
		}else if(model instanceof Model_3DR){
			radius = 110 + ((Model_3DR) model).radius;
		}

		else{
			//default value here;
			radius = 200;
		}
		activeFlag = false;
		doneFlag = failFlag = false;
	}
	
	public void doReachAvoid(ItemPosition start, ItemPosition dest, ObstacleList unsafe){
		activeFlag = true;
		doneFlag = failFlag = false;
		this.start = start;
		this.dest = dest;
		// planObs is created by passing reference, such that if that obsList changes, re_plan can happen
		planObs = unsafe;
		// unsafe is cloned such that unsafe set is unchanged through out execution
		this.unsafe = unsafe.clone();
		stage = STAGE_R.PLAN;
		running = true;
		if(!this.isAlive()){
			start();
		}
		counter = 0;
	}
	
	public void cancel(){
		gvh.plat.moat.cancel();
		stage = STAGE_R.IDLE;
	}
	
	@Override
	public synchronized void start() {
		super.start();
		gvh.log.d(TAG, "STARTED!");
	}

	@Override
	public void run() {
		super.run();
		gvh.threadCreated(this);
		while(running){
			switch(stage){
			case IDLE:
				//do nothing
				break;
			case PLAN:
				double magic = .0011;
				int xRange = Math.abs(start.x - dest.x);
				int yRange = Math.abs(start.y - dest.y);
				xLower = Math.min(start.x, dest.x) - (int)((xRange+radius)*(counter+1)*radius*magic);
				xUpper = Math.max(start.x, dest.x) + (int)((xRange+radius)*(counter+1)*radius*magic);
				yLower = Math.min(start.y, dest.y) - (int)((yRange+radius)*(counter+1)*radius*magic);
				yUpper = Math.max(start.y, dest.y) + (int)((yRange+radius)*(counter+1)*radius*magic);
				
				RRTNode path = new RRTNode(start.x, start.y);
				System.out.println("Getting pathStack from " + start  + " to " + dest);
				pathStack = path.findRoute(dest, 1000, planObs, xLower, xUpper, yLower,yUpper, start, radius);
				if(pathStack == null){
					counter ++ ; 
					if(counter > tries){
						activeFlag = false;
						gvh.log.i(TAG, "RRT could not find solution, giving up");
						return;
					}
					//else plan again using wider search
				}
				else{
					gvh.log.i(TAG, "RRT has found a solution");
					kdTree =  path.stopNode;
					stage = STAGE_R.PICK;
				}
				try {
					System.out.println("Found a pathStack of size: " + pathStack.size());
				} catch(Exception e){
					System.out.println("Accessing pathStack produced exception: " + e);
					return;
				}
				break;
				
			case PICK:
				if(!pathStack.empty()){
					System.out.println("Picking a new point");
					//if did not reach last midway point, go back to path planning
					ItemPosition goMidPoint = pathStack.pop();
					gvh.plat.moat.goTo(goMidPoint);
					gvh.log.i(TAG, " go to called to: " + goMidPoint.toString());
					stage = STAGE_R.MOVE;
				}
				else{
					System.out.println("pathStack is empty");
					if(gvh.plat.moat.done){
						System.out.println("ReachAvoid Done");
						gvh.log.i(TAG, " dest Reached: " + dest.toString());
						stage = STAGE_R.IDLE;
						doneFlag = true;
						activeFlag = false;
					}
					else if(!gvh.plat.moat.inMotion){
						// add fail flag check here
						System.out.println("reachAvoid replan");
						gvh.log.i(TAG, " Failed: " + dest.toString());
						stage = STAGE_R.IDLE;
						failFlag = false;
						activeFlag = false;
					}
					//running = false;
				}
				break;
			case MOVE:
				System.out.println("MOVING");
				if(!unsafe.validstarts(gvh.gps.getMyPosition(), radius +1) ){
					System.out.println("reachAvoid failed, safty ");
					gvh.log.i(TAG, " Failed: " + dest.toString());
					failFlag = false;
					activeFlag = false;
				}
				//check fail flag here
				if(gvh.plat.moat.done) {
					System.out.println("Switching to PICK");
					stage = STAGE_R.PICK;
				}
				else if (!gvh.plat.moat.inMotion){
					System.out.print("Switching to PLAN");
					stage = STAGE_R.PLAN;
				}
				break;
			}
			gvh.sleep(100);
		}
	}
}

