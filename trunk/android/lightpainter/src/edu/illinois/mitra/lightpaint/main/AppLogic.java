package edu.illinois.mitra.lightpaint.main;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;

import android.util.Log;
import edu.illinois.mitra.lightpaint.BotProgressTracker;
import edu.illinois.mitra.lightpaint.ImagePoint;
import edu.illinois.mitra.lightpaint.PointManager;
import edu.illinois.mitra.starl.bluetooth.MotionParameters;
import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.functions.SingleHopMutualExclusion;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class AppLogic extends LogicThread implements MessageListener {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private Collection<Cancellable> created;
	
	private boolean running = true;
	private RobotMotion motion = null;
	private Synchronizer sync = null;
	private MutualExclusion mutex = null;
	private LeaderElection le = null;
	private String name = null;
	private String leader = null;
	private boolean iamleader = false;
	
	//---------------------
	// Constant stage names
	public enum STAGE {
		START,LEADERELECT_BARRIER,LEADERELECT,DIVIDE_LINES,GET_LINE_ASSIGNMENT,GO_TO_START,CALC_NEXT_POINT_BARRIER,CALC_NEXT_POINT,GO_NEXT_POINT,WAIT_AT_INTERSECTION,FINISH,DONE
	}
	private STAGE stage = STAGE.START;

	// Application specific
	public static final int MSG_INFORMLINE 				= 20;
	public static final int MSG_LINEPROGRESS 			= 21;
	
	//----------------------------
	// Barrier synchronization IDs
	public static final String SYNC_BEGIN = "BEGIN";
	public static final String SYNC_START_DRAWING = "START_DRAWING";
	
	// Application specific:
	private int timeSpentWaiting = 0;
	private BotProgressTracker prog = null;
	private PointManager points = null;
	private ImagePoint dest = null;
	private int assignment = -1;
	private int intersection = -1;
	private SortedSet<ImagePoint> myPoints = null;
	private Iterator<ImagePoint> pointIter = null;
	
	// Motion parameters
	private MotionParameters param = new MotionParameters();
	
	public AppLogic(GlobalVarHolder gvh) {
		super(gvh);
		created = new HashSet<Cancellable>();
		
		this.gvh = gvh;
		this.motion = gvh.plat.moat;
		
		name = gvh.id.getName();
		points = new PointManager(gvh);
		sync = new BarrierSynchronizer(gvh);
		le = new RandomLeaderElection(gvh);
		
		gvh.comms.addMsgListener(MSG_INFORMLINE, this);
		gvh.log.i(TAG, "I AM " + name);
		
		created.add(sync);
		created.add(le);
		
		// Maximum angle at which robots can curve to their destination.
		// This prevents "soft" corners and forces robots to turn in place at sharper angles		
		param.ARCANGLE_MAX = 25;
		param.COLAVOID_MODE = MotionParameters.STOP_ON_COLLISION;
	}
	
	@Override
	public void cancel() {
		gvh.log.d(TAG, "CANCELLING LOGIC THREAD");
		gvh.comms.removeMsgListener(MSG_INFORMLINE);
		running = false;
		
		// Cancel all created elements
		for(Cancellable c: created) {
			try {
				c.cancel();
			} catch(NullPointerException e) {}
		}
		created.clear();
	}

	public void messageReceied(RobotMessage m) {
		gvh.log.i(TAG, "Received assignment message " + m.getContents(0));
		assignment = Integer.parseInt(m.getContents(0));
	}	
	
	// Waits until all tokens have been obtained for the next intersection
	// Triggers progress updates every 3000 milliseconds spent waiting
	private STAGE intersectionWait() {
		if(mutex.clearToEnter(intersection)) {
			timeSpentWaiting = 0;
			return STAGE.GO_NEXT_POINT;
		} else {
			sleep(50);
			timeSpentWaiting += 50;
			if(timeSpentWaiting >= 3000) {
				prog.updateMyProgress(assignment,dest.getPoint());
				timeSpentWaiting = 0;
			}
			return STAGE.WAIT_AT_INTERSECTION;
		}
	}
	
	public LinkedList<Object> call() throws Exception {
		while(running) {
			switch(stage) {
			case START:
				// Initially the screen should be dark
				screenColor("000000");
				screenDark();
				
				sync.barrier_sync(SYNC_BEGIN);
				stage = STAGE.LEADERELECT_BARRIER;
				gvh.log.i(TAG, "Leaderelect barrier...");
				break;
				
			case LEADERELECT_BARRIER:
				sleep(50);
				if(sync.barrier_proceed(SYNC_BEGIN)) {
					stage = STAGE.LEADERELECT;
				}
				break;
				
			case LEADERELECT:
				leader = le.elect();
				iamleader = (leader.equals(name));
				gvh.log.e(TAG, "Leader is " + leader + ". Is it me? " + iamleader);
				gvh.plat.sendMainToast(leader);
				gvh.log.d(TAG, "Leader elected!");
				stage = STAGE.DIVIDE_LINES;
				break;
			
			case DIVIDE_LINES:
				points.parseWaypoints();
				gvh.log.d(TAG, "Waypoints processed");
				
				// Leader distributes line segments
				if(iamleader) {
					gvh.log.d(TAG, "I'm the leader! Sending assignments...");
					points.Assign();
					gvh.log.d(TAG, "Waypoints divided");
				}
				
				stage = STAGE.GET_LINE_ASSIGNMENT;
				break;
				
			case GET_LINE_ASSIGNMENT:				
				// Receive line assignments
				gvh.log.d(TAG, "Waiting to receive assignment...");
				while(assignment == -1) {}

				gvh.log.d(TAG, "Assigned as robot " + assignment);			
				
				// Start the mutual exclusion thread
				mutex = new SingleHopMutualExclusion(points.getNumMutex(),gvh,leader);
				mutex.start();
				created.add(mutex);

				stage = STAGE.GO_TO_START;
				
				myPoints = points.getPoints(assignment);
				pointIter = myPoints.iterator();
				break;
				
			case GO_TO_START:
				// Go to the first assigned waypoint
				ImagePoint start = pointIter.next();
				if(!start.isStart()) throw new RuntimeException("My start point wasn't a start point!");
				dest = start;
				
				motion.goTo(dest.getPos());
				gvh.log.d(TAG, "Going to start...");
				motionHold();
				
				gvh.log.d(TAG, "Turning to face next point...");
				ImagePoint nextdest = pointIter.next();
				
				// Reset the iterator
				pointIter = myPoints.iterator();pointIter.next();
				
				motion.turnTo(nextdest.getPos());
				motionHold();
			
				sync.barrier_sync(SYNC_START_DRAWING);
				stage = STAGE.CALC_NEXT_POINT_BARRIER;
				break;
				
			case CALC_NEXT_POINT_BARRIER:
				sleep(50);
				if(sync.barrier_proceed(SYNC_START_DRAWING)){					
					// Wait to give the photographer enough time to press the shutter
					sleep(1000);
					//motion.song();
					
					// Create the progress tracker
					prog = new BotProgressTracker(gvh);
					created.add(prog);
					
					stage = STAGE.CALC_NEXT_POINT;
				}
				break;
				
			case CALC_NEXT_POINT:
				// dest is where we are now
				if(dest.isEnd()) {
					stage = STAGE.FINISH;
					break;
				}
				
				dest = pointIter.next();
				
				// Send a progress update
				prog.updateMyProgress(assignment,dest.getPoint());
				
				// If the next point requires a different mutex token than the currently held, release the current
				if(dest.getMutex() != intersection && intersection != -1) {
					mutex.exit(intersection);
					intersection = -1;
				}
				
				// Handle the case when we're heading into an intersection
				if(dest.getMutex() != -1) {
					intersection = dest.getMutex();
					if(!mutex.clearToEnter(intersection)) {
						mutex.requestEntry(intersection);
						stage = STAGE.WAIT_AT_INTERSECTION;
						break;
					}
				}
				
				stage = STAGE.GO_NEXT_POINT;
				break;
				
			case WAIT_AT_INTERSECTION:
				screenDark();	
				gvh.plat.setDebugInfo("Waiting for mutex " + intersection);
				stage = intersectionWait();
				break;
				
			case GO_NEXT_POINT:
				gvh.plat.setDebugInfo("");
				gvh.log.d(TAG, "Next point: " + dest.getPoint());
				
				motion.goTo(dest.getPos(), param);
				updateScreen();
				motionHold();		
				stage = STAGE.CALC_NEXT_POINT;
				break;				
				
			case FINISH:
				gvh.plat.setDebugInfo("Done!");
				mutex.exitAll();
				prog.sendDone();
				stage = STAGE.DONE;
				break;
				
			case DONE:
				return null;
				
			default:
				gvh.log.e(ERR, "LogicThread somehow ended up in an uncovered stage: " + stage);
				break;
			}	
		}
		return null;
	}
	
	
	
	// Set the screen color and brightness for the current line
	private void updateScreen() {
		screenColor(dest.getColor());
		
		if(!dest.getColor().equals("000000")) {
			screenBright();
		}	
	}
	
	private void screenColor(String color) {
		gvh.plat.sendMainMsg(RobotsActivity.MESSAGE_SCREEN_COLOR, color);
	}

	private void screenBright() {
		gvh.plat.sendMainMsg(RobotsActivity.MESSAGE_SCREEN, 100);
	}
	
	private void screenDark() {
		screenColor("000000");
		gvh.plat.sendMainMsg(RobotsActivity.MESSAGE_SCREEN, 0);
	}
	
	private int motionHold() {
		while(motion.inMotion) {sleep(10);}
		return 0;
	}
	
	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}	
	}
}
