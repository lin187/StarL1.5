package edu.illinois.mitra;

import android.util.Log;
import edu.illinois.mitra.Objects.LeaderElection;
import edu.illinois.mitra.Objects.MutualExclusion;
import edu.illinois.mitra.Objects.Synchronizer;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.Objects.itemPosition;
import edu.illinois.mitra.bluetooth.RobotMotion;
import edu.illinois.mitra.comms.RobotMessage;
import edu.illinois.mitra.lightpaint.AssignedLines;
import edu.illinois.mitra.lightpaint.BotProgressThread;
import edu.illinois.mitra.lightpaint.DivideLines;

public class LogicThread extends Thread {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private boolean running = true;
	private globalVarHolder gvh = null;
	private RobotMotion motion = null;
	private Synchronizer sync = null;
	private MutualExclusion mutex = null;
	private BotProgressThread prog = null;
	private int stage = 0;
	private String name = null;
	private String leader = null;
	
	// Maximum angle at which robots can curve to their destination.
	// This prevents "soft" corners and forces robots to turn in place at sharper angles
	private static final int MAXCURVEANGLE = 25;
	
	// Constant stage names
	public static final int STAGE_START 					= 0;
	public static final int STAGE_LEADERELECT_BARRIER 		= 1;
	public static final int STAGE_LEADERELECT 				= 2;
	public static final int STAGE_DIVIDE_LINES				= 3;
	public static final int STAGE_GET_LINE_ASSIGNMENT		= 4;
	public static final int STAGE_GO_TO_START				= 5;
	public static final int STAGE_CALC_NEXT_POINT_BARRIER 	= 6;
	public static final int STAGE_CALC_NEXT_POINT 			= 7;
	public static final int STAGE_GO_NEXT_POINT 			= 8;
	public static final int STAGE_WAIT_AT_INTERSECTION 		= 9;
	public static final int STAGE_FRAME_DONE				= 10;
	public static final int STAGE_DONE = 99;

	//---------------------
	// Constant message IDs
	public static final int MSG_BARRIERSYNC 			= 5;
	public static final int MSG_MUTEX_TOKEN_OWNER_BCAST = 6;
	public static final int MSG_MUTEX_TOKEN 			= 7;
	public static final int MSG_MUTEX_TOKEN_REQUEST 	= 8;
	public static final int MSG_LEADERELECT 			= 9;
	public static final int MSG_LEADERELECT_ANNOUNCE	= 10;
	
	// Application specific
	public static final int MSG_INFORMLINE 				= 11;
	public static final int MSG_LINEPROGRESS 			= 12;
	
	//----------------------------
	// Barrier synchronization IDs
	public static final String SYNC_BEGIN = "1";
	public static final String SYNC_START_DRAWING = "2";
	
	// Values for tracking current position, start, and destination
	private int cur_frame = 0;
	private int intersection_num = -1;
	private boolean new_line_seg = false;
	private AssignedLines assignment = new AssignedLines();
	
	private boolean iamleader = false;
	DivideLines div = null;
	itemPosition dest = null;
	
	public LogicThread(globalVarHolder gvh, RobotMotion motion) {
		this.gvh = gvh;
		this.motion = motion;
		name = gvh.getName();
		div = new DivideLines(gvh);
		sync = new Synchronizer(gvh);
		running = true;
		Log.i(TAG, "I AM " + name);
	}

	@Override
	public void run() {
		while(running) {
			switch(stage) {
			case STAGE_START:				
				sync.barrier_sync(SYNC_BEGIN);
				stage = STAGE_LEADERELECT_BARRIER;
				break;
				
			case STAGE_LEADERELECT_BARRIER:
				sleep(50);
				if(sync.barrier_proceed(SYNC_BEGIN)) {
					stage = STAGE_LEADERELECT;
				}
				break;
				
			case STAGE_LEADERELECT:
				LeaderElection le = new LeaderElection(gvh);
				leader = le.elect();
				iamleader = (leader.equals(name));
				gvh.sendMainToast(leader);
				Log.d(TAG, "Leader elected!");
				stage = STAGE_DIVIDE_LINES;
				break;
			
			case STAGE_DIVIDE_LINES:
				div.processWaypoints();
				Log.d(TAG, "Waypoints processed");
				
				// Leader distributes line segments
				if(iamleader) {
					div.assignLineSegments();
					Log.d(TAG, "Waypoints divided");
				}
				
				stage = STAGE_GET_LINE_ASSIGNMENT;
				break;
				
			case STAGE_GET_LINE_ASSIGNMENT:
				if(iamleader) {
					div.sendAssignments(cur_frame);
					Log.d(TAG, "Waypoints sent for frame " + cur_frame);
				}
				
				// Receive line assignments
				while(gvh.getIncomingMessageCount(MSG_INFORMLINE) == 0) {}
				RobotMessage assignmsg = gvh.getIncomingMessage(MSG_INFORMLINE);
				assignment.parseAssignmentMessage(assignmsg);

				Log.d(TAG, "Frame " + cur_frame + ": Assigned lines " + assignment.rangeString());			
				
				// If this robot doesn't have an assignment for this frame, skip straight to the end
				if(!assignment.includedInFrame()) {
					Log.e(TAG, "I'm not included in frame " + cur_frame + "!");

					// TODO: Make sure this works and doesn't send non-participants into the abyss
					// TODO: "I should really get out of the way here"
					itemPosition outOfFrame = new itemPosition("OUT",(name.toLowerCase().charAt(0)-97)*300,0,0);
					motion.go_to(outOfFrame);
					while(motion.inMotion) {sleep(10);}
					Log.e(TAG, "I'm at my designated non-participant point. Sad robot.");
					
					// "Send out a sync message so other robots don't wait around for me"
					sync.barrier_sync(SYNC_START_DRAWING);
					stage = STAGE_FRAME_DONE;
				} else {
					Log.i(TAG, "I'm included in frame " + cur_frame + "!");
					stage = STAGE_GO_TO_START;
					
					if(prog != null) {
						prog.cancel();
						prog = null;
					}
				}
				
				// Start the mutual exclusion thread
				mutex = new MutualExclusion(div.getFrame(cur_frame).getNumIntersections(),gvh,leader);
				mutex.start();
				break;
				
			case STAGE_GO_TO_START:
				// Go to the first assigned waypoint
				assignment.setCurToStart();
				dest = div.getFrame(cur_frame).getLinePoint(assignment.getCurPos());
				
				motion.go_to(dest);
				Log.d(TAG, "Going to start...");
				while(motion.inMotion) {sleep(10);}
				
				Log.d(TAG, "Turning to face next point...");
				
				dest = div.getFrame(cur_frame).getNextLinePoint(assignment.getStartPos());
				
				Log.d(TAG, "Facing my next point!");
				
				if(dest == null) {
					Log.e(TAG, "I was only assigned ONE POINT for this frame! I quit!");
					// TODO: Make sure this works and doesn't send non-participants into the abyss
					// TODO: "I should really get out of the way here"
					//itemPosition outOfFrame = new itemPosition("OUT",(name.toLowerCase().charAt(0)-97)*300,200,0);
					//motion.go_to(outOfFrame);
					// "Send out a sync message so other robots don't wait around for me"
					sync.barrier_sync(SYNC_START_DRAWING);
					//while(motion.inMotion) {sleep(10);}
					//Log.e(TAG, "I'm at my designated non-participant point. Sad robot.");
					stage = STAGE_FRAME_DONE;
					break;
				}
				
//				int destLine = div.getFrame(cur_frame).getNextLineNum(assignment.getStartPos());
//				int destPoint = div.getFrame(cur_frame).getNextPointNum(assignment.getStartPos());
//				
//				Log.e(TAG, "Currently at " + dest.toString());
//				Log.e(TAG, "Next point SHOULD be frame " + cur_frame + " line " + destLine + " point " + destPoint);
//				
//				dest = div.getFrame(cur_frame).getLinePoint(destLine, destPoint);
//				if(dest == null) {
//					Log.e(TAG, "NULL NULL NULL NULL NULL NULL NULL");
//				} else {
//					Log.d(TAG, "  " + dest.toString());
//				}
				motion.turn_to(dest);
				while(motion.inMotion) {sleep(10);}

				sync.barrier_sync(SYNC_START_DRAWING);
				stage = STAGE_CALC_NEXT_POINT_BARRIER;
				break;
				
			case STAGE_CALC_NEXT_POINT_BARRIER:
				sleep(50);
				if(sync.barrier_proceed(SYNC_START_DRAWING)){
					Log.d(TAG, "Barrier sync complete!");
					stage = STAGE_CALC_NEXT_POINT;
					
					// Wait to give the photographer enough time to press the shutter
					sleep(400);
					motion.song();
					
					// Create and start the progress tracking thread
					prog = new BotProgressThread(gvh);
					prog.start();
				}
				break;
				
			case STAGE_CALC_NEXT_POINT:								
				// Calculate the next point to go to
				int nextline = div.getFrame(cur_frame).getNextLineNum(assignment.getCurPos());
				int nextpoint = div.getFrame(cur_frame).getNextPointNum(assignment.getCurPos());
				
				// If the end of the artwork has been reached
				if(nextline == -1 || nextpoint == -1) {
					Log.e(TAG, "Reached the end of the artwork!");
					stage = STAGE_FRAME_DONE;
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
					while(motion.inMotion) {sleep(10);}
					
					stage = STAGE_FRAME_DONE;
					screenDark();
					
					// Send a progress update
					prog.sendDone();					
					break;
				}
				
				// Send a progress update
				prog.updateMyProgress(assignment.getCurPos());
				
				// If we're moving to a new line segment, the flag the turn to be done with the screen darkened 
				if(nextline != assignment.getCurLine()) {
					Log.i(TAG, "Going dark for this corner!");
					new_line_seg = true;
				}
				
				assignment.setCurPos(nextline, nextpoint);
				Log.i(TAG, "My next point is " + assignment.curString());

				// If the next point is an intersection, request access.
				// If it's not an intersection and we're holding on to an intersection token, let go of the token.
				if(div.getFrame(cur_frame).isIntersection(assignment.getCurPos())) {
					intersection_num = div.getFrame(cur_frame).intersectionNumber(assignment.getCurPos());
					if(!mutex.clearToEnter(intersection_num)) {
						mutex.requestEntry(intersection_num);
						stage = STAGE_WAIT_AT_INTERSECTION;
						motion.song(1);
					} else {
						stage = STAGE_GO_NEXT_POINT;
					}					
				} else if(intersection_num != -1) {
					mutex.exit(intersection_num);
					intersection_num = -1;
					motion.song(2);
				}
					
				stage = STAGE_GO_NEXT_POINT;
				break;
				
			case STAGE_WAIT_AT_INTERSECTION:
				screenDark();
				Log.d(TAG, "Holding at intersection " + intersection_num);
				if(mutex.clearToEnter(intersection_num)) {
					stage = STAGE_GO_NEXT_POINT;
				} else {
					sleep(50);
				}
				break;
				
			case STAGE_GO_NEXT_POINT:
				//screenDark();
				Log.d(TAG, "Next point: " + assignment.curString());
				dest = div.getFrame(cur_frame).getLinePoint(assignment.getCurPos());
				
				// TODO: See if this code can be replaced by activity level code
				// If we're moving to a new line segment, keep the screen dark until we're ready to move forward
//				if(new_line_seg) {
//					new_line_seg = false;
//					motion.turn_to(dest);
//					screenColor("ff0000");
//					screenBright();
//					while(motion.inMotion) {sleep(10);}
//				}
				
				// Travel to the next point, keep curving to a minimum to prevent wavy images
				// Don't use collision avoidance, keep everyone on their lines.
				motion.go_to(dest,MAXCURVEANGLE, false);
				
				// Set the screen color
				screenColor(div.getFrame(cur_frame).lineColor(assignment.getCurLine()));
				
				// Illuminate the screen if it's not a ghost line
				if(!div.getFrame(cur_frame).isGhost(assignment.getCurLine())) {
					screenBright();
				}
				
				while(motion.inMotion) {sleep(10);}				
				stage = STAGE_CALC_NEXT_POINT;
				break;				
			
			case STAGE_FRAME_DONE:
				// If the last frame has been completed, end
				// Otherwise, continue looping
				if(cur_frame == div.getNumFrames()-1) {
					stage = STAGE_DONE;
				} else {
					cur_frame ++;
					stage = STAGE_GET_LINE_ASSIGNMENT;
				}
				break;
				
			case STAGE_DONE:
				//Nothing!
				break;
				
			default:
				Log.e(ERR, "LogicThread somehow ended up in an uncovered stage: " + stage);
				break;
			}	
		}
	}

	private void screenColor(String color) {
		Log.i(TAG, "  Colored " + color);
		gvh.sendMainMsg(RobotsActivity.MESSAGE_SCREEN_COLOR, color);
	}

	private void screenBright() {
		Log.i(TAG, "  Bright!");
		gvh.sendMainMsg(RobotsActivity.MESSAGE_SCREEN, 100);
	}
	
	private void screenDark() {
		Log.i(TAG, "  Dark.");
		screenColor("000000");
		gvh.sendMainMsg(RobotsActivity.MESSAGE_SCREEN, 0);
	}
	
	@Override
	public synchronized void start() {
		super.start();
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
	}

	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}	
	}	
}
