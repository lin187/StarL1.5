package edu.illinois.mitra.lightpaint;

import java.awt.Graphics2D;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.illinois.mitra.lightpaint.algorithm.LpAlgorithm;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;
import edu.illinois.mitra.lightpaint.utility.WptParser;
import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class MainActivity extends LogicThread implements MessageListener {

	// Algorithm constants
	private static final double POINT_SNAP_RADIUS = 25;
	private static final double MAX_DRAW_LENGTH = 500;
	private static final double UNSAFE_RADIUS = 180;

	private static final long MAX_REQUEST_WAIT_TIME = 2000;

	// Message IDs
	private static final int ASSIGNMENT_REQ_ID = 50;
	private static final int POSITION_UPDATE_ID = 51;
	private static final int ASSIGNMENT_ID = 52;

	private static final MotionParameters motionParameters = new MotionParameters();
	static {
		motionParameters.COLAVOID_MODE = MotionParameters.COLAVOID_MODE_TYPE.STOP_ON_COLLISION;
		motionParameters.STOP_AT_DESTINATION = true;
	}

	private static enum Stage {
		INIT, ELECT_LEADER, REQUEST_ASSIGNMENT, WAIT_FOR_ASSIGNMENT, DO_ASSIGNMENT, WAIT_TO_ARRIVE, DONE
	};

	private volatile Stage stage = Stage.INIT;

	public MainActivity(GlobalVarHolder gvh) {
		super(gvh);

		// Register as message listeners
		gvh.comms.addMsgListener(ASSIGNMENT_ID, this);
		gvh.comms.addMsgListener(POSITION_UPDATE_ID, this);
		gvh.comms.addMsgListener(ASSIGNMENT_REQ_ID, this);

		election = new RandomLeaderElection(gvh);

		// Set up motion parameters
		gvh.plat.moat.setParameters(motionParameters);
	}

	private String leader;
	private boolean iAmLeader = false;
	private LeaderElection election;
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
				election.elect();
				setStage(Stage.ELECT_LEADER);
				break;
			case ELECT_LEADER:
				if((leader = election.getLeader()) != null) {
					iAmLeader = leader.equals(gvh.id.getName());
					if(iAmLeader) {
						System.out.println(name + " is leader!");
						// Create the algorithm, inform it of all robot positions
						alg = new LpAlgorithm(WptParser.parseWaypoints(gvh), POINT_SNAP_RADIUS, MAX_DRAW_LENGTH, UNSAFE_RADIUS);

						for(String robot : gvh.id.getParticipants())
							alg.setRobotPosition(robot, ImagePoint.fromItemPosition(gvh.gps.getPosition(robot)));
					}
					setStage(Stage.REQUEST_ASSIGNMENT);
				}
				break;
			case REQUEST_ASSIGNMENT:
				// Send a request to the leader
				assignment.clear();
				if(iAmLeader) {
					assignment.addAll(alg.assignSegment(super.name, gvh.gps.getMyPosition()));
					if(!assignment.isEmpty()) {
						lastVisitedPoint = assignment.remove(0);
						setStage(Stage.DO_ASSIGNMENT);
					} else if(alg.isDone() && (doneInformedCount == (gvh.id.getParticipants().size() - 1))) {
						stage = Stage.DONE;
					} else {
						reqSentTime = gvh.time();
						setStage(Stage.WAIT_FOR_ASSIGNMENT);
					}
				} else {
					RobotMessage req = new RobotMessage(leader, name, ASSIGNMENT_REQ_ID, "");
					gvh.comms.addOutgoingMessage(req);
					reqSentTime = gvh.time();
					setStage(Stage.WAIT_FOR_ASSIGNMENT);
				}
				break;
			case WAIT_FOR_ASSIGNMENT:
				if(!assignment.isEmpty()) {
					setStage(Stage.DO_ASSIGNMENT);
					break;
				}
				if((gvh.time() - reqSentTime) > MAX_REQUEST_WAIT_TIME) {
					// Request timed out, request again
					setStage(Stage.REQUEST_ASSIGNMENT);
				}
				break;
			case DO_ASSIGNMENT:
				gvh.plat.moat.goTo(currentDestination = assignment.remove(0));
				setStage(Stage.WAIT_TO_ARRIVE);
				break;
			case WAIT_TO_ARRIVE:
				if(!gvh.plat.moat.inMotion) {
					RobotMessage informProgress = new RobotMessage(leader, super.name, POSITION_UPDATE_ID, new MessageContents(lastVisitedPoint.toMessage(), currentDestination.toMessage()));
					gvh.comms.addOutgoingMessage(informProgress);
					lastVisitedPoint = currentDestination;
					if(assignment.isEmpty())
						setStage(Stage.REQUEST_ASSIGNMENT);
					else
						setStage(Stage.DO_ASSIGNMENT);
					break;
				}

				break;
			case DONE:
				return null;
			}

			gvh.sleep(100);
		}
	}

	private static final MessageContents EMPTY_MSG_CONTENTS = new MessageContents("NONE");
	private static final MessageContents FINISHED_MSG_CONTENTS = new MessageContents("DONE");

	private int doneInformedCount = 0;

	@Override
	public void messageReceied(RobotMessage msg) {
		switch(msg.getMID()) {
		case ASSIGNMENT_ID:
			if(msg.getContents().equals(EMPTY_MSG_CONTENTS)) {
				return;
			} else if(msg.getContents().equals(FINISHED_MSG_CONTENTS)) {
				stage = Stage.DONE;
				return;
			}

			for(String content : msg.getContentsList()) {
				try {
					assignment.add(ItemPosition.fromMessage(content));
				} catch(IllegalArgumentException e) {
					System.err.println(msg);
				}

			}

			// Assignment[0] is the current robot position
			lastVisitedPoint = assignment.remove(0);
			break;
		case ASSIGNMENT_REQ_ID:
			if(!iAmLeader)
				return;

			List<ItemPosition> assigned = alg.assignSegment(msg.getFrom(), gvh.gps.getPosition(msg.getFrom()));

			RobotMessage response = new RobotMessage(msg.getFrom(), super.name, ASSIGNMENT_ID, (MessageContents) null);

			if(alg.isDone()) {
				response.setContents(FINISHED_MSG_CONTENTS);
				doneInformedCount++;
			} else if(assigned != null && assigned.size() > 0) {
				String[] assignedPieces = new String[assigned.size()];
				for(int i = 0; i < assigned.size(); i++)
					assignedPieces[i] = assigned.get(i).toMessage();
				response.setContents(new MessageContents(assignedPieces));
			} else {
				response.setContents(EMPTY_MSG_CONTENTS);
			}

			gvh.comms.addOutgoingMessage(response);
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
			break;
		}
	}

	private void setStage(Stage newstage) {
		//		System.out.println(super.name + "\t" + stage + "->" + newstage);
		stage = newstage;
	}

	@Override
	public void draw(Graphics2D g) {
		if(iAmLeader)
			alg.draw(g);
	}

}
