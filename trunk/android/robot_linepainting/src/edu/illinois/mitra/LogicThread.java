package edu.illinois.mitra;

import android.util.Log;
import edu.illinois.mitra.Objects.LeaderElection;
import edu.illinois.mitra.Objects.MutualExclusion;
import edu.illinois.mitra.Objects.Synchronizer;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.Objects.itemPosition;
import edu.illinois.mitra.bluetooth.RobotMotion;
import edu.illinois.mitra.comms.RobotMessage;
import edu.illinois.mitra.lightpaint.DivideLines;

public class LogicThread extends Thread {
	private static final String TAG = "Logic";
	private static final String ERR = "Critical Error";
	
	private globalVarHolder gvh;
	private RobotMotion motion;
	private Synchronizer sync;
	private MutualExclusion mutex;
	private int stage = 0;
	private String name = null;
	private String leader = null;
	
	// Constant stage names
	public static final int STAGE_START 					= 0;
	public static final int STAGE_LEADERELECT_BARRIER 		= 1;
	public static final int STAGE_LEADERELECT 				= 2;
	public static final int STAGE_DIVIDE_LINES				= 3;
	public static final int STAGE_GO_TO_START				= 4;
	public static final int STAGE_CALC_NEXT_POINT_BARRIER 	= 5;
	public static final int STAGE_CALC_NEXT_POINT 			= 6;
	public static final int STAGE_GO_NEXT_POINT 			= 7;
	public static final int STAGE_WAIT_AT_INTERSECTION 		= 8;
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
	private int my_startline = 0;
	private int my_endline = 0;
	private int my_startpoint = 0;
	private int my_endpoint = 0;
	private int intersection_num = -1;
	
	private int my_currentline = 0;
	private int my_currentpoint = 0;
	private boolean iamleader = false;
	DivideLines div = null;
	itemPosition dest = null;
	
	public LogicThread(globalVarHolder gvh, RobotMotion motion) {
		this.gvh = gvh;
		this.motion = motion;
		name = gvh.getName();
		div = new DivideLines(gvh);
		sync = new Synchronizer(gvh);
	}

	@Override
	public void run() {
		//super.run();
		while(true) {
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
				leader = le.elect(); //"Alice";
				iamleader = (leader.equals(name));
				gvh.sendMainToast(leader);
				Log.d(TAG, "Leader elected!");
				stage = STAGE_DIVIDE_LINES;
				motion.blink(1);
				break;
			
			case STAGE_DIVIDE_LINES:
				div.processWaypoints();
				Log.d(TAG, "Waypoints processed");
				
				// Leader distributes line segments
				if(iamleader) {
					div.assignLineSegments();
					Log.d(TAG, "Waypoints divided");
					
					// Send assignments to all robots
					div.sendAssignments();
					Log.d(TAG, "Waypoints sent");
				}
				
				// Receive line assignments
				while(gvh.getIncomingMessageCount(MSG_INFORMLINE) == 0) {}
				RobotMessage assignments = gvh.getIncomingMessage(MSG_INFORMLINE);
				String[] parts = assignments.getContents().split(",");
				String[] startParts = parts[0].split(":");
				String[] endParts = parts[1].split(":");
				
				Log.d(TAG, "Assigned lines " + parts[0] + " to " + parts[1]);

				my_startline = Integer.parseInt(startParts[0]);
				my_startpoint = Integer.parseInt(startParts[1]);
				my_endline = Integer.parseInt(endParts[0]);
				my_endpoint = Integer.parseInt(endParts[1]);
				
				stage = STAGE_GO_TO_START;
				
				// Start the mutual exclusion thread
				mutex = new MutualExclusion(div.getNumIntersections(),gvh,leader);
				mutex.start();
				
				gvh.setDebugInfo(my_startline + ", " + my_startpoint + " -> " + my_endline + ", " + my_endpoint);
				break;
				
			case STAGE_GO_TO_START:
				// Go to the first assigned waypoint
				dest = div.getLinePoint(my_startline, my_startpoint);
				my_currentline = my_startline;
				my_currentpoint = my_startpoint;
				motion.go_to(dest);
				Log.d(TAG, "Going to start...");
				Log.i(TAG, dest.toString());
				gvh.setDebugInfo(my_startline + ", " + my_startpoint + " -> " + my_endline + ", " + my_endpoint + "\nGOING TO START");
				while(motion.inMotion) {sleep(10);}
				
				Log.d(TAG, "Turning to face next point...");
				dest = div.getNextLinePoint(my_startline, my_startpoint);
				Log.i(TAG, dest.toString());
				motion.turn_to(dest);
				gvh.setDebugInfo(my_startline + ", " + my_startpoint + " -> " + my_endline + ", " + my_endpoint + "\nTURNING TO NEXT");
				while(motion.inMotion) {sleep(10);}

				sync.barrier_sync(SYNC_START_DRAWING);
				stage = STAGE_CALC_NEXT_POINT_BARRIER;
				break;
				
			case STAGE_CALC_NEXT_POINT_BARRIER:
				sleep(50);
				if(sync.barrier_proceed(SYNC_START_DRAWING)){
					Log.d(TAG, "Barrier sync complete!");
					stage = STAGE_CALC_NEXT_POINT;
					sleep(1000);
					motion.song();
				}
				break;
				
			case STAGE_CALC_NEXT_POINT:
				// Calculate the next point to go to
				int nextline = div.getNextLineNum(my_currentline, my_currentpoint);
				int nextpoint = div.getNextPointNum(my_currentline, my_currentpoint);
				
				if(nextline == -1 || nextpoint == -1) {
					Log.e(TAG, "Reached the end of the artwork!");
					stage = STAGE_DONE;
					screenDark();
					break;
				}
				
				if(nextline == my_endline && nextpoint == my_endpoint) {
					Log.e(TAG, "Reaching the end of my section!");
					dest = div.getLinePoint(nextline, nextpoint);
					motion.go_to(dest);
					while(motion.inMotion) {sleep(10);}				
					stage = STAGE_DONE;
					screenDark();
					break;
				}
				
				my_currentline = nextline;
				my_currentpoint = nextpoint;

				// If the next point is an intersection, request access.
				// If it's not an intersection and we're holding on to an intersection token, let go of the token.
				if(div.isIntersection(my_currentline, my_currentpoint)) {
					intersection_num = div.intersectionNumber(my_currentline, my_currentpoint);
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
				
				gvh.setDebugInfo(my_startline + ", " + my_startpoint + " -> " + my_endline + ", " + my_endpoint + "\n" + my_currentline + ", " + my_currentpoint);				
				stage = STAGE_GO_NEXT_POINT;
				break;
				
			case STAGE_WAIT_AT_INTERSECTION:
				Log.d(TAG, "Holding at intersection " + intersection_num);
				screenDark();
				if(mutex.clearToEnter(intersection_num)) {
					stage = STAGE_GO_NEXT_POINT;
				}
				break;
				
			case STAGE_GO_NEXT_POINT:
				screenDark();
				Log.d(TAG, "Next point: " + my_currentline + " " + my_currentpoint);
				dest = div.getLinePoint(my_currentline, my_currentpoint);
				motion.go_to(dest);
				screenColor(div.lineColor(my_currentline));
				while(motion.inMotion) {sleep(10);}				
				stage = STAGE_CALC_NEXT_POINT;
				break;				
				
			case STAGE_DONE:
				//Nothing!
				break;
				
			default:
				//Nothing here!
				break;
			}	
		}
	}

	private void screenColor(int color) {
		gvh.sendMainMsg(RobotsActivity.MESSAGE_SCREEN, (color*100));
	}
	
	private void screenDark() {
		gvh.sendMainMsg(RobotsActivity.MESSAGE_SCREEN, 0);
	}
	
	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		super.start();
	}
	
	public void cancel() {
		
	}

	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}	
	}	
}
