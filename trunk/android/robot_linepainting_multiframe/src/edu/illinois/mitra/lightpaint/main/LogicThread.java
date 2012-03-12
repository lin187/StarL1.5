package edu.illinois.mitra.lightpaint.main;

import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;

import android.util.Log;
import edu.illinois.mitra.lightpaint.BotProgressThread;
import edu.illinois.mitra.lightpaint.ImagePoint;
import edu.illinois.mitra.lightpaint.PointManager;
import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.functions.SingleHopMutualExclusion;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.globalVarHolder;
import edu.illinois.mitra.starl.objects.itemPosition;

public class LogicThread extends Thread implements MessageListener {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private boolean running = true;
	private globalVarHolder gvh = null;
	private RobotMotion motion = null;
	private Synchronizer sync = null;
	private MutualExclusion mutex = null;
	private LeaderElection le = null;
	private String name = null;
	private String leader = null;
	private boolean iamleader = false;
	
	// Maximum angle at which robots can curve to their destination.
	// This prevents "soft" corners and forces robots to turn in place at sharper angles
	private static final int MAXCURVEANGLE = 25;
	
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
	private BotProgressThread prog = null;
	private PointManager points = null;
	private ImagePoint dest = null;
	private int assignment = -1;
	private int intersection = -1;
	private SortedSet<ImagePoint> myPoints = null;
	private Iterator<ImagePoint> pointIter = null;
	
	public LogicThread(globalVarHolder gvh, RobotMotion motion) {
		this.gvh = gvh;
		this.motion = motion;
		name = gvh.getName();
		points = new PointManager(gvh);
		sync = new BarrierSynchronizer(gvh);
		le = new RandomLeaderElection(gvh);
		gvh.removeMsgListener(MSG_INFORMLINE);
		gvh.addMsgListener(MSG_INFORMLINE, this);
		Log.i(TAG, "I AM " + name);
	}
	
	@Override
	public void run() {
		while(running) {
			switch(stage) {
			case START:
				// Initially the screen should be dark
				screenColor("000000");
				screenDark();
				
				sync.barrier_sync(SYNC_BEGIN);
				stage = STAGE.LEADERELECT_BARRIER;
				Log.i(TAG, "Leaderelect barrier...");
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
				gvh.sendMainToast(leader);
				Log.d(TAG, "Leader elected!");
				stage = STAGE.DIVIDE_LINES;
				break;
			
			case DIVIDE_LINES:
				points.parseWaypoints();
				Log.d(TAG, "Waypoints processed");
				
				// Leader distributes line segments
				if(iamleader) {
					points.Assign();
					Log.d(TAG, "Waypoints divided");
				}
				
				stage = STAGE.GET_LINE_ASSIGNMENT;
				break;
				
			case GET_LINE_ASSIGNMENT:				
				// Receive line assignments
				while(assignment == -1) {}

				Log.d(TAG, "Assigned as robot " + assignment);			
				
				// Start the mutual exclusion thread
				mutex = new SingleHopMutualExclusion(points.getNumMutex(),gvh,leader);
				mutex.start();

				stage = STAGE.GO_TO_START;
				
				myPoints = points.getPoints(assignment);
				pointIter = myPoints.iterator();
				break;
				
			case GO_TO_START:
				// Go to the first assigned waypoint
				ImagePoint start = pointIter.next();
				if(!start.isStart()) throw new RuntimeException("My start point wasn't a start point! SortedSet might not be working, yeah?");
				dest = start;
				
				motion.go_to(dest.getPos());
				Log.d(TAG, "Going to start...");
				motionHold();
				
//				Log.d(TAG, "Turning to face next point...");
//				nextdest = pointIter.next();
//				motion.turn_to(nextdest.getPos());
//				motionHold();
				
				sync.barrier_sync(SYNC_START_DRAWING);
				stage = STAGE.CALC_NEXT_POINT_BARRIER;
				break;
				
			case CALC_NEXT_POINT_BARRIER:
				sleep(50);
				if(sync.barrier_proceed(SYNC_START_DRAWING)){					
					// Wait to give the photographer enough time to press the shutter
					sleep(1000);
					motion.song();
					
					// Create the progress tracker
					prog = new BotProgressThread(gvh);
					
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
				stage = intersectionWait();
				break;
				
			case GO_NEXT_POINT:
				Log.d(TAG, "Next point: " + dest.getPoint());
				
				// Travel to the next point, keep curving to a minimum to prevent wavy images
				// Don't use collision avoidance, keep everyone on their lines.
				motion.go_to(dest.getPos(), MAXCURVEANGLE, false);
				updateScreen();
				motionHold();		
				stage = STAGE.CALC_NEXT_POINT;
				break;				
				
			case FINISH:
				mutex.exitAll();
				prog.sendDone();
				stage = STAGE.DONE;
				break;
				
			case DONE:
				//Nothing!
				break;
				
			default:
				Log.e(ERR, "LogicThread somehow ended up in an uncovered stage: " + stage);
				break;
			}	
		}
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

	// Set the screen color and brightness for the current line
	private void updateScreen() {
		screenColor(dest.getColor());
		
		if(!dest.getColor().equals("000000")) {
			screenBright();
		}	
	}
	
	private void screenColor(String color) {
		gvh.sendMainMsg(RobotsActivity.MESSAGE_SCREEN_COLOR, color);
	}

	private void screenBright() {
		gvh.sendMainMsg(RobotsActivity.MESSAGE_SCREEN, 100);
	}
	
	private void screenDark() {
		screenColor("000000");
		gvh.sendMainMsg(RobotsActivity.MESSAGE_SCREEN, 0);
	}
	
	private int motionHold() {
		while(motion.inMotion) {sleep(10);}
		return 0;
	}
	
	@Override
	public synchronized void start() {
		super.start();
		running = true;
	}
	
	public void cancel() {
		Log.d(TAG, "CANCELLING LOGIC THREAD");
		
		running = false;
		if(prog != null) {
			prog.cancel();
		}
		if(mutex != null) {
			mutex.cancel();
		}
		if(sync != null) {
			sync.cancel();
		}
	}

	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}	
	}

	public void messageReceied(RobotMessage m) {
		// TODO Auto-generated method stub
		
	}	
}
