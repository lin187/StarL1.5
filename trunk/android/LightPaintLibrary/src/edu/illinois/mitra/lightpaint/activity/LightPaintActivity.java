package edu.illinois.mitra.lightpaint.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import edu.illinois.mitra.lightpaint.algorithm.LpAlgorithm;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;
import edu.illinois.mitra.lightpaint.utility.WptParser;
import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.RobotEventListener;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class LightPaintActivity extends LogicThread implements RobotEventListener {
	private static final String TAG = "LP";
	private static final boolean ENABLE_X = true;
	// Algorithm constants
	private static final double POINT_SNAP_RADIUS = 50;
	private static final double MAX_DRAW_LENGTH = 1200;
	private static final double UNSAFE_RADIUS = 230;

	private static final long MAX_REQUEST_WAIT_TIME = 5000;

	// Message IDs
	private static final int ASSIGNMENT_REQ_ID = 50;
	private static final int POSITION_UPDATE_ID = 51;
	private static final int ASSIGNMENT_ID = 52;
	private static final String BEGIN_BARRIER = "BEGIN";

	// Handler message
	public static final int HANDLER_SCREEN = 9724;
	public static final int SCREEN_X = 9725;

	private static final MotionParameters motionParameters;
	static {
		MotionParameters.Builder builder = new MotionParameters.Builder();
		builder.GOAL_RADIUS(50);
		builder.ARCANGLE_MAX(15);
		builder.COLAVOID_MODE(MotionParameters.COLAVOID_MODE_TYPE.STOP_ON_COLLISION);
		builder.STOP_AT_DESTINATION(true);
		motionParameters = builder.build();
	}

	private static enum Stage {
		INIT, ELECT_BARRIER, ELECT_LEADER, REQUEST_ASSIGNMENT, WAIT_FOR_ASSIGNMENT, DO_ASSIGNMENT, WAIT_TO_ARRIVE, DONE
	};

	private volatile Stage stage = Stage.INIT;

	public LightPaintActivity(GlobalVarHolder gvh) {
		super(gvh);

		// Register as message listeners
		gvh.comms.addMsgListener(this, ASSIGNMENT_ID, POSITION_UPDATE_ID, ASSIGNMENT_REQ_ID);

		election = new RandomLeaderElection(gvh);
		sync = new BarrierSynchronizer(gvh);

		// Set up motion parameters
		gvh.plat.moat.setParameters(motionParameters);

		gvh.addEventListener(this);
	}

	private String leader;
	private BarrierSynchronizer sync;
	private LeaderElection election;
	
	private int requestCount = 0;
	private int assignmentsDelivered = 0;

	// Public to be accessed by Drawer
	private boolean iAmLeader = false;
	private LpAlgorithm alg;

	private List<ItemPosition> assignment = Collections.synchronizedList(new LinkedList<ItemPosition>());

	private long reqSentTime = 0l;

	private ItemPosition currentDestination;
	private ItemPosition lastVisitedPoint;

	@Override
	public List<Object> callStarL() {
		while(true) {
			switch(stage) {
			case INIT:
				sync.barrierSync(BEGIN_BARRIER);
				setStage(Stage.ELECT_BARRIER);
				break;
			case ELECT_BARRIER:
				if(sync.barrierProceed(BEGIN_BARRIER)) {
					election.elect();
					setStage(Stage.ELECT_LEADER);
				}
				break;
			case ELECT_LEADER:
				if((leader = election.getLeader()) != null) {
					iAmLeader = leader.equals(gvh.id.getName());
					gvh.log.d(TAG, leader + " is leader!");
					if(iAmLeader) {
						gvh.log.d(TAG, "I'm the leader!");
						// Create the algorithm, inform it of all robot positions
						alg = new LpAlgorithm(WptParser.parseWaypoints(gvh), POINT_SNAP_RADIUS, MAX_DRAW_LENGTH, UNSAFE_RADIUS);
						for(String robot : gvh.id.getParticipants()) {
							ItemPosition ip = gvh.gps.getPosition(robot);
							alg.setRobotPosition(robot, new ImagePoint(ip.getX(), ip.getY(), 0, 0));
						}

						responseService = Executors.newSingleThreadExecutor();
						responseService.submit(new Assigner());
						assignmentRequesters = new LinkedBlockingQueue<String>();
					}
					setStage(Stage.REQUEST_ASSIGNMENT);
				}
				break;
			case REQUEST_ASSIGNMENT:
				screenX(true);
				// Send a request to the leader
				assignment.clear();
				gvh.log.e(TAG, "Requesting an assignment");
				if(iAmLeader) {
					requestCount ++;
					assignmentRequesters.add(name);
				} else {
					RobotMessage req = new RobotMessage(leader, name, ASSIGNMENT_REQ_ID, "");
					gvh.comms.addOutgoingMessage(req);
				}
				reqSentTime = gvh.time();
				setStage(Stage.WAIT_FOR_ASSIGNMENT);
				break;
			case WAIT_FOR_ASSIGNMENT:
				screenX(true);
				if((gvh.time() - reqSentTime) > MAX_REQUEST_WAIT_TIME) {
					// Request timed out, request again
					setStage(Stage.REQUEST_ASSIGNMENT);
					gvh.log.e(TAG, "Assignment request had timed out!");
				}
				break;
			case DO_ASSIGNMENT:
				screenX(false);
				gvh.plat.moat.goTo(currentDestination = assignment.remove(0));
				screenColor = getColorFromPosition(lastVisitedPoint);
				screenLineSize = getSizeFromPosition(lastVisitedPoint);
				updateScreen();
				gvh.log.d(TAG, "Assignment has " + assignment.size() + " points remaining.");
				setStage(Stage.WAIT_TO_ARRIVE);
				break;
			case WAIT_TO_ARRIVE:
				if(!gvh.plat.moat.inMotion) {
					RobotMessage informProgress = new RobotMessage(leader, super.name, POSITION_UPDATE_ID, new MessageContents(lastVisitedPoint.toMessage(), currentDestination.toMessage()));
					if(iAmLeader) {
						receive(informProgress);
					} else {
						gvh.comms.addOutgoingMessage(informProgress);
					}
					lastVisitedPoint = currentDestination;
					if(assignment.isEmpty()) {
						setStage(Stage.REQUEST_ASSIGNMENT);
					} else {
						setStage(Stage.DO_ASSIGNMENT);
					}
					break;
				}

				break;
			case DONE:
				if(iAmLeader) {
					List<Object> retval = new ArrayList<Object>();
					retval.add(requestCount);
					retval.add(assignmentsDelivered);
					retval.add(alg.deadlocked());
					return retval;
				}
				return null;
			}

			sleep(100);
		}
	}

	private int getColorFromPosition(ItemPosition pos) {
		return pos.getAngle();
	}

	private static int getSizeFromPosition(ItemPosition pos) {
		return Integer.parseInt(pos.getName());
	}

	private static final MessageContents EMPTY_MSG_CONTENTS = new MessageContents("NONE");
	private static final MessageContents FINISHED_MSG_CONTENTS = new MessageContents("DONE");

	private int doneInformedCount = 0;

	@Override
	protected void receive(RobotMessage msg) {
		switch(msg.getMID()) {
		case ASSIGNMENT_ID:
			if(msg.getContents().equals(EMPTY_MSG_CONTENTS)) {
				return;
			} else if(msg.getContents().equals(FINISHED_MSG_CONTENTS)) {
				setStage(Stage.DONE);
				return;
			} else if(stage != Stage.WAIT_FOR_ASSIGNMENT) {
				gvh.log.e(TAG, "Rejected assignment! I'm not expecting to receive one!");
				return;
			}

			for(String content : msg.getContentsList()) {
				try {
					assignment.add(ItemPosition.fromMessage(content));
				} catch(IllegalArgumentException e) {
					System.err.println(msg);
				}
			}
			gvh.log.d(TAG, name + " new assignment has " + assignment.size() + " points");

			// Assignment[0] is the current robot position
			lastVisitedPoint = assignment.remove(0);

			setStage(Stage.DO_ASSIGNMENT);
			break;
		case ASSIGNMENT_REQ_ID:
			if(!iAmLeader || alg == null || assignmentRequesters == null)
				return;

			gvh.log.i(TAG, msg.getFrom() + " requesting a new assignment...");
			requestCount ++;
			if(!assignmentRequesters.contains(msg.getFrom()))
				assignmentRequesters.add(msg.getFrom());
			break;
		case POSITION_UPDATE_ID:
			if(!iAmLeader)
				return;

			// Inform the algorithm of the progress made
			ItemPosition startIp = ItemPosition.fromMessage(msg.getContents(0));
			ItemPosition endIp = ItemPosition.fromMessage(msg.getContents(1));
			ImagePoint start = ImagePoint.fromItemPosition(startIp);
			ImagePoint end = ImagePoint.fromItemPosition(endIp);
			alg.markSafeDrawn(msg.getFrom(), start, end);
			gvh.log.e(TAG, "Marked point from " + msg.getFrom() + " clear");
			break;
		}
	}

	private BlockingQueue<String> assignmentRequesters;
	private ExecutorService responseService;

	private class Assigner implements Runnable {
		@Override
		public void run() {
			while(true) {
				try {
					gvh.log.d(TAG, "Waiting for a new assignment...");
					assign(assignmentRequesters.take());
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Detect deadlocks
	private Map<String, Boolean> isAssignmentEmpty = new HashMap<String, Boolean>();

	private void assign(String from) {
		gvh.log.e(TAG, "Assigning to " + from);
		long now = System.nanoTime();
		List<ItemPosition> assigned = alg.assignSegment(from, gvh.gps.getPosition(from));
		gvh.log.i(TAG, "Assignment took " + (System.nanoTime() - now) / (double) 1e9 + " seconds for " + assignment.size() + " points.");

		// If the requester was another robot, respond with a message
		if(!from.equals(name)) {
			isAssignmentEmpty.put(from, false);
			RobotMessage response = new RobotMessage(from, super.name, ASSIGNMENT_ID, (MessageContents) null);
			if(alg.isDone()) {
				response.setContents(FINISHED_MSG_CONTENTS);
				doneInformedCount++;
			} else if(assigned != null && assigned.size() > 0) {
				assignmentsDelivered ++;
				String[] assignedPieces = new String[assigned.size()];
				for(int i = 0; i < assigned.size(); i++)
					assignedPieces[i] = assigned.get(i).toMessage();
				response.setContents(new MessageContents(assignedPieces));
			} else {
				response.setContents(EMPTY_MSG_CONTENTS);
				isAssignmentEmpty.put(from, true);
			}
			gvh.comms.addOutgoingMessage(response);
		} else {
			// If the requester was me, skip the message handling.
			isAssignmentEmpty.put(name, false);
			assignment.addAll(assigned);
			if(!assignment.isEmpty()) {
				assignmentsDelivered ++;
				lastVisitedPoint = assignment.remove(0);
				setStage(Stage.DO_ASSIGNMENT);
			} else if(alg.isDone() && (doneInformedCount == (gvh.id.getParticipants().size() - 1))) {
				setStage(Stage.DONE);
			} else {
				reqSentTime = gvh.time();
				setStage(Stage.WAIT_FOR_ASSIGNMENT);
				isAssignmentEmpty.put(name, true);
			}
		}

		if(isDeadlocked()) {
			System.err.println("DEADLOCK with " + alg.deadlocked() + " line segments remaining");
		}
	}

	private boolean isDeadlocked() {
		if(isAssignmentEmpty.size() != gvh.id.getParticipants().size()) {
			return false;
		}

		for(Boolean b : isAssignmentEmpty.values()) {
			if(!b)
				return false;
		}
		return true;
	}

	private void setStage(Stage newstage) {
		gvh.plat.setDebugInfo(newstage.toString() + " - " + leader + " - " + assignment.size());
		stage = newstage;
		gvh.log.d("LightPaint", "Stage: " + stage);
	}

	private boolean inMotion = false;
	private int screenColor = 0;
	private int screenLineSize = 0;

	public LpAlgorithm getAlgorithm() {
		return alg;
	}

	public int getScreenColor() {
		return screenColor;
	}

	public ItemPosition getMyPosition() {
		return gvh.gps.getMyPosition();
	}

	@Override
	public void robotEvent(Event eventType, int eventData) {
		if(eventType == Event.MOTION) {
			switch(eventData) {
			case Common.MOT_STOPPED:
			case Common.MOT_TURNING:
				inMotion = false;
				break;
			default:
				inMotion = true;
			}
		}
		updateScreen();
	}

	private int currentScreenColor = 0;

	private void updateScreen() {
		if(inMotion) {
			if(currentScreenColor != screenColor) {
				currentScreenColor = screenColor;
				gvh.plat.sendMainMsg(HANDLER_SCREEN, screenColor, screenLineSize);
				if(iAmLeader)
					gvh.log.d(TAG, "Screen on!");
			}
		} else if(currentScreenColor != 0) {
			currentScreenColor = 0;
			gvh.plat.sendMainMsg(HANDLER_SCREEN, 0, screenLineSize);
			if(iAmLeader)
				gvh.log.d(TAG, "** SCREEN OFF **");
		}
	}
	
	private void screenX(boolean xOn) {
		gvh.log.i("LightPaint", "Setting X to " + (ENABLE_X && xOn));
		gvh.plat.sendMainMsg(SCREEN_X, ENABLE_X && xOn);
	}

	@Override
	public void cancel() {
		sync.cancel();
		election.cancel();
		gvh.removeEventListener(this);
		setStage(Stage.DONE);
		if(iAmLeader)
			responseService.shutdownNow();
		gvh.comms.removeMsgListener(ASSIGNMENT_ID);
		gvh.comms.removeMsgListener(POSITION_UPDATE_ID);
		gvh.comms.removeMsgListener(ASSIGNMENT_REQ_ID);
	}

}
