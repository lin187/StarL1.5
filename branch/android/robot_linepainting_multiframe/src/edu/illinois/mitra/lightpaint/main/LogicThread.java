package edu.illinois.mitra.lightpaint.main;

import java.util.HashSet;
import java.util.Set;

import android.util.Log;
import edu.illinois.mitra.lightpaint.AssignedLines;
import edu.illinois.mitra.lightpaint.BotProgressThread;
import edu.illinois.mitra.lightpaint.DivideLines;
import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.functions.SingleHopMutualExclusion;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.MutualExclusion;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.globalVarHolder;
import edu.illinois.mitra.starl.objects.itemPosition;

public class LogicThread extends Thread {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private boolean running = true;
	private globalVarHolder gvh = null;
	private RobotMotion motion = null;
	private Synchronizer sync = null;
	private MutualExclusion mutex = null;
	private LeaderElection le = null;
	//private int stage = 0;
	private String name = null;
	private String leader = null;
	private boolean iamleader = false;
	
	// Maximum angle at which robots can curve to their destination.
	// This prevents "soft" corners and forces robots to turn in place at sharper angles
	private static final int MAXCURVEANGLE = 25;
	
	// Radius of intersection collection. When entering an intersection, tokens for
	// all intersections within this radius must be collected as well.
	private static final int INTERSECTION_RADIUS = 1;
	
	//---------------------
	// Constant stage names

	public enum STAGE {
		START,LEADERELECT_BARRIER,LEADERELECT,DIVIDE_LINES,GET_LINE_ASSIGNMENT,GO_TO_START,CALC_NEXT_POINT_BARRIER,CALC_NEXT_POINT,GO_NEXT_POINT,WAIT_AT_INTERSECTION,FRAME_DONE,DONE
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
	private int cur_frame = 0;
	private Set<Integer> intersections = new HashSet<Integer>();
	private Set<Integer> new_intersections = new HashSet<Integer>();
	private AssignedLines assignment = new AssignedLines();
	private int timeSpentWaiting = 0;
	
	private BotProgressThread prog = null;
	private DivideLines div = null;
	private itemPosition dest = null;
	
	
	public LogicThread(globalVarHolder gvh, RobotMotion motion) {
		this.gvh = gvh;
		this.motion = motion;
		name = gvh.getName();
		div = new DivideLines(gvh);
		sync = new BarrierSynchronizer(gvh);
		le = new RandomLeaderElection(gvh);
		gvh.removeMsgListener(MSG_INFORMLINE);
		gvh.addMsgListener(MSG_INFORMLINE, assignment);
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
				div.processWaypoints();
				Log.d(TAG, "Waypoints processed");
				
				// Leader distributes line segments
				if(iamleader) {
					div.assignLineSegments();
					Log.d(TAG, "Waypoints divided");
				}
				
				stage = STAGE.GET_LINE_ASSIGNMENT;
				break;
				
			case GET_LINE_ASSIGNMENT:
				if(iamleader) {
					div.sendAssignments(cur_frame);
					Log.d(TAG, "Waypoints sent for frame " + cur_frame);
				}
				
				// Receive line assignments
				while(!assignment.hasNextAssignment()) {}
				assignment.parseNextAssignment();

				Log.d(TAG, "Frame " + cur_frame + ": Assigned lines " + assignment.rangeString());			
				
				// Start the mutual exclusion thread
				mutex = new SingleHopMutualExclusion(div.getFrame(cur_frame).getNumIntersections(),gvh,leader);
				mutex.start();
				
				// If this robot doesn't have an assignment for this frame, skip straight to the end
				if(!assignment.includedInFrame()) {
					Log.e(TAG, "I'm not included in frame " + cur_frame + "!");

					// Assume center is at 1750,1750
					int centerX = 1750;
					int centerY = 1750;
					int nameloc = (name.toLowerCase().charAt(0)-97)*500;
					int exitX = nameloc;
					int exitY = 300;
					itemPosition me = gvh.getMyPosition();
					if(me.getY() > centerY) exitY = 3200;
					if(me.getX() > centerX) exitX = 3200-nameloc;
					itemPosition outOfFrame = new itemPosition("OUT",exitX,exitY,0);
					motion.go_to(outOfFrame);
					Log.e(TAG, "Going to my out of frame position: " + outOfFrame);
					motionHold();

					// "Send out a sync message so other robots don't wait around for me"
					sync.barrier_sync(SYNC_START_DRAWING);
					if(!intersections.isEmpty()) {
						intersections.clear();
						mutex.exitAll();
					}
					stage = STAGE.FRAME_DONE;
				} else {
					Log.i(TAG, "I'm included in frame " + cur_frame + "!");
					stage = STAGE.GO_TO_START;
					
					if(prog != null) {
						prog.cancel();
					}
				}
				break;
				
			case GO_TO_START:
				// Go to the first assigned waypoint
				assignment.setCurToStart();
				dest = div.getFrame(cur_frame).getLinePoint(assignment.getCurPos());
				motion.go_to(dest);
				Log.d(TAG, "Going to start...");
				motionHold();
				
				Log.d(TAG, "Turning to face next point...");

				// If the next point is at the same location as the current point, jump two points ahead
				itemPosition nextPt = div.getFrame(cur_frame).getNextLinePoint(assignment.getStartPos());
				if(nextPt.equals(dest)) {
					nextPt = div.getFrame(cur_frame).getNextLinePoint(nextPt);
				}
				dest = nextPt;				

				motion.turn_to(dest);
				motionHold();
				
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
				// Calculate the next point to go to
				int nextline = div.getFrame(cur_frame).getNextLineNum(assignment.getCurPos());
				int nextpoint = div.getFrame(cur_frame).getNextPointNum(assignment.getCurPos());
				
				// If the end of the artwork has been reached
				if(nextline == -1 || nextpoint == -1) {
					Log.e(TAG, "Reached the end of the artwork!");
					stage = STAGE.FRAME_DONE;
					screenDark();
					
					// Send a progress update
					prog.sendDone();
					break;
				}
				
				// If the end of the robot's current section has been reached
				if(assignment.equalsEndPos(nextline, nextpoint)) {
					Log.e(TAG, "Reaching the end of my section!");
					dest = div.getFrame(cur_frame).getLinePoint(nextline, nextpoint);
					
					motion.go_to(dest,MAXCURVEANGLE, false);
					motionHold();
					
					stage = STAGE.FRAME_DONE;
					screenDark();
					
					// Send a progress update
					prog.sendDone();					
					break;
				}
				
				// Send a progress update
				prog.updateMyProgress(assignment.getCurPos());
				
				assignment.setCurPos(nextline, nextpoint);
				stage = STAGE.GO_NEXT_POINT;
				
				// Handle the case when we're heading into an intersection
				if(div.getFrame(cur_frame).isIntersection(assignment.getCurPos())) {
					new_intersections = div.getFrame(cur_frame).intersectionNumbers(assignment.getCurPos(), INTERSECTION_RADIUS);
					Log.i(TAG, "  Intersection #" + new_intersections.toString());
					
					// Return tokens no longer needed
					HashSet<Integer> exitme = new HashSet<Integer>(intersections);
					exitme.removeAll(new_intersections);
					mutex.exit(exitme);
					intersections.removeAll(exitme);
					
					// If we don't hold the token(s) needed to continue, request them and wait
					if(!intersections.containsAll(new_intersections)) {
						Log.i(TAG, "  I requested the token(s). Holding.");
						
						// Remove any intersections already held
						new_intersections.removeAll(intersections);
						mutex.requestEntry(new_intersections);
						motion.song(1);
						
						stage = STAGE.WAIT_AT_INTERSECTION;
					} else {
						Log.i(TAG, "  I hold the token(s): " + intersections.toString());
					}
				} else if(!intersections.isEmpty()) {
					// If the next point isn't an intersection but tokens are still held, release the tokens
					Log.d(TAG, "  Returning all tokens.");
					mutex.exitAll();
					intersections.clear();
					motion.song(2);
				}
				break;
				
			case WAIT_AT_INTERSECTION:
				screenDark();	
				stage = intersectionWait();
				break;
				
			case GO_NEXT_POINT:
				Log.d(TAG, "Next point: " + assignment.curString());
				dest = div.getFrame(cur_frame).getLinePoint(assignment.getCurPos());
				
				// Travel to the next point, keep curving to a minimum to prevent wavy images
				// Don't use collision avoidance, keep everyone on their lines.
				motion.go_to(dest,MAXCURVEANGLE, false);
				updateScreen();
				motionHold();		
				stage = STAGE.CALC_NEXT_POINT;
				break;				
			
			case FRAME_DONE:
				// If the last frame has been completed, end
				// Otherwise, continue looping
				if(cur_frame == div.getNumFrames()-1) {
					stage = STAGE.DONE;
					Log.d(TAG, "  Returning all tokens.");
					intersections.clear();
					mutex.exitAll();
				} else {
					cur_frame ++;
					stage = STAGE.GET_LINE_ASSIGNMENT;
				}
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
		if(mutex.clearToEnter(new_intersections)) {
			intersections.clear();
			intersections.addAll(new_intersections);
			timeSpentWaiting = 0;
			return STAGE.GO_NEXT_POINT;
		} else {
			sleep(50);
			timeSpentWaiting += 50;
			if(timeSpentWaiting >= 3000) {
				prog.updateMyProgress(assignment.getCurPos());
				timeSpentWaiting = 0;
			}
			return STAGE.WAIT_AT_INTERSECTION;
		}
	}

	// Set the screen color and brightness for the current line
	private void updateScreen() {
		screenColor(div.getFrame(cur_frame).lineColor(assignment.getCurLine()));
		
		if(!div.getFrame(cur_frame).isGhost(assignment.getCurLine())) {
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
}
