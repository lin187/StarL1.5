//Written by Lucas Buccafusca
//05-31-2012
//What the App does: 
//At the beginning of implementation, the robots are synced together to allow for communication between them.
//A leader is chosen through a determined leader selection (bot0 will always be the leader)  
//The robots will then travel (while maintaining the arrow shape) to a series of waypoints

package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.comms.LossyMessageSender;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.PickedLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;
import edu.illinois.mitra.starlSim.main.SimSettings;

public class LucasApp2 extends LogicThread implements MessageListener {

	SortedSet<String> toVisit = new TreeSet<String>();
	SortedSet<String> toVisit2 = new TreeSet<String>();
	SortedSet<String> arrived = new TreeSet<String>();

	private static final String TAG = "Logic";

	private String destname = null;
	private String leader = null;
	private boolean running = true;

	private boolean iamleader = false;
	private LeaderElection le;
	private Synchronizer sync;
	private LossyMessageSender lms;
	private int d_d = 500; // Some distance that each robot will be from the
							// nearest robot
	private int d_r = 550;// Thickness of Tube
	private double theta_r = Math.PI / 4; // 0<theta_r<2*PI, theta_r != PI/2,
											// 3*PI/2... Be wary of angles
											// <PI/2.7
	ItemPosition leaderstart = new ItemPosition("goHere",
			SimSettings.GRID_XSIZE / 2, SimSettings.GRID_YSIZE / 2, 0);

	String robotName = gvh.id.getName();
	Integer robotNum = Integer.parseInt(robotName.substring(3));
	private int numbots = gvh.id.getParticipants().size();
	private Integer[] detail = new Integer[numbots];

	PositionList posList = new PositionList();
	private final static String SYNC_START = "1";

	private enum STAGE {
		START, SYNC, LE, WAIT, MOVE, WAYPOINT_CALC, WAYPOINT_TRAVEL, WAYPOINT_BROADCAST, WAIT_TO_ARRIVE, DONE
	};

	private STAGE stage = STAGE.START;

	public LucasApp2(GlobalVarHolder gvh) {
		super(gvh);

		// Get the list of position to travel to
		for (ItemPosition ip : gvh.gps.getWaypointPositions().getList()) {
			toVisit.add(ip.name);
		}

		// Progress messages are broadcast with message ID 99 and 98
		gvh.comms.addMsgListener(99, this);
		gvh.comms.addMsgListener(98, this);

		// Make sure waypoints were provided
		if (gvh.gps.getWaypointPositions().getNumPositions() == 0)
			System.out
					.println("This application requires waypoints to travel to!");

		sync = new BarrierSynchronizer(gvh);
		le = new PickedLeaderElection(gvh);
		lms = new LossyMessageSender(gvh);

		for (int i = 0; i < numbots; i++)
			arrived.add("bot" + i);

	}

	@Override
	public List<Object> callStarL() {

		// Declares leader
		while (running) {
			gvh.sleep(100);
			gvh.plat.setDebugInfo(gvh.id.getParticipants().toString());
			switch (stage) {
			case START:
				sync.barrier_sync(SYNC_START);
				stage = STAGE.SYNC;
				gvh.log.d(TAG, "Syncing...");
				System.out.println("Syncing..." + name);
				break;
			case SYNC:
				if (sync.barrier_proceed(SYNC_START)) {
					stage = STAGE.LE;
					le.elect();
					gvh.log.d(TAG, "Synced!");
				}
				break;
			case LE:
				if (le.getLeader() != null) {
					gvh.log.d(TAG, "Electing...");
					leader = le.getLeader();
					iamleader = leader.equals(name);
					System.out.println("Robot Leader? " + leader);
					stage = STAGE.MOVE;

				}
				break;

			case MOVE:

				gvh.plat.moat.goTo(startpoint_line());
				boolean motionSuccess = true;
				while (gvh.plat.moat.inMotion) {
					gvh.sleep(10);
					if (!toVisit2.contains("START POINT" + robotNum)) {
						motionSuccess = false;
						break;
					}
				}

				// If Arrival of the LAST robot
				if (motionSuccess && toVisit2.isEmpty()) {
					stage = STAGE.MOVE;

				} else if (!toVisit2.isEmpty()) {
					toVisit2.remove("START POINT" + robotNum);
				}

				if (toVisit2.isEmpty()) {
					stage = STAGE.WAIT_TO_ARRIVE;
				} else {
					stage = STAGE.MOVE;
				}
				break;

			case WAIT_TO_ARRIVE:

				motionSuccess = true;
				while (gvh.plat.moat.inMotion) {
					gvh.sleep(10);
					if (!toVisit2.contains(destname)) {
						motionSuccess = false;
						break;
					}
				}

				// If Arrives
				if (motionSuccess) {
					RobotMessage inform = new RobotMessage("ALL", name, 99,
							"bot" + robotNum);
					gvh.comms.addOutgoingMessage(inform);
					arrived.remove("bot" + robotNum);
				}

				stage = STAGE.WAIT;

				break;

			case WAIT:
				// Stall until all robots have arrived
				if (arrived.isEmpty()) {
					stage = STAGE.WAYPOINT_BROADCAST;
					break;
				} else {
					stage = STAGE.WAIT;
					gvh.sleep(10);

				}
				break;

			case WAYPOINT_BROADCAST:
				if (!iamleader) {
					MessageContents rm = new MessageContents(
							gvh.gps.getMyPosition().x + " "
									+ gvh.gps.getMyPosition().y);
					RobotMessage inform = new RobotMessage(leader, name, 98, rm);
					// Insert any message modification code here
					// lms.setStaticLossRate("Bob", 0.5); // 50% of messages to
					// recipient Bob will be dropped
					// lms.newTemporalLoss("Bob", gvh.time() + 1000, 5000, 0.5)
					// Starting 1 second from now (1000 ms) and ending 6 seconds
					// from now, drop an additional 50% of messages to Bob
					lms.send(inform);

				}

				gvh.sleep(100);
				stage = STAGE.WAYPOINT_TRAVEL;

				break;

			case WAYPOINT_TRAVEL:

				// BROADCAST NEW POSITION TO TRAVEL TO

				// GO TO NEW POSITION

				// Code to make each robot calculate it's own waypoint
				/*
				 * gvh.sleep(10); gvh.plat.moat.turnTo(newpoint_line());
				 * gvh.sleep(2100); if (arrived.isEmpty()) {
				 * 
				 * gvh.plat.moat.goTo(newpoint_line()); while
				 * (gvh.plat.moat.inMotion) { gvh.sleep(1); }
				 * toVisit.remove(toVisit.first());
				 * 
				 * 
				 * }
				 */

				// checks if all waypoints are gone
				/*
				 * if(toVisit.isEmpty()) { stage = STAGE.DONE; break; } else {
				 * stage = STAGE.WAYPOINT_TRAVEL; break; }
				 */
				stage = STAGE.DONE;

			case DONE:
				System.out.println(name + " is done.");

				gvh.plat.moat.motion_stop();
				le.cancel();
				sync.cancel();
				gvh.comms.removeMsgListener(99);
				gvh.comms.removeMsgListener(98);
				return Arrays.asList(results);

			}

		}
		return null;
	}

	@Override
	public void messageReceied(RobotMessage m) {
		if (m.getMID() == 99) {
			synchronized (arrived) {
				// Remove the received waypoint from the list of waypoints to
				// visit
				arrived.remove(m.getContents(0));

			}
		}
		// This will only get called for the leader
		if (m.getMID() == 98) {
			synchronized (detail) {

			}

			// Ideal location for that robot:

		}

	}

	private ItemPosition startpoint_line() {

		toVisit2.add("START POINT" + robotNum);
		if (iamleader) {
			return new ItemPosition("goHere", SimSettings.GRID_XSIZE / 2,
					SimSettings.GRID_YSIZE / 2, 0);
		} else {

			{
				return new ItemPosition("goHere",
						(int) (SimSettings.GRID_XSIZE / 2 + d_d * robotNum
								* Math.cos(theta_r)),
						(int) (SimSettings.GRID_YSIZE / 2 - d_d * robotNum
								* Math.sin(theta_r)), 0);
			}

		}
	}

	private ItemPosition newpoint_line() {

		String robotName = gvh.id.getName();
		Integer robotNum = Integer.parseInt(robotName.substring(3)); // assumes:
																		// botYYY
		destname = toVisit.first();

		ItemPosition New_leader_position = new ItemPosition("goHere",
				gvh.gps.getWaypointPosition(destname).x,
				gvh.gps.getWaypointPosition(destname).y, 0);
		if (iamleader) {
			gvh.plat.moat.turnTo(New_leader_position);
			gvh.sleep(100);
			gvh.plat.moat.motion_stop();
		} else {
			gvh.sleep(100);
			gvh.plat.moat.motion_stop();
		}

		if (iamleader) {

			return New_leader_position;
		} else {
			int angle_of_leader = gvh.gps.getPosition(leader).angle;
			int new_angle_of_leader = gvh.gps.getPosition(leader).angleTo(
					New_leader_position);
			double difference = new_angle_of_leader - angle_of_leader + 90;
			difference = Math.toRadians(difference);

			if (robotNum % 2 == 0 && !iamleader) {

				return new ItemPosition(
						"goHere",
						(int) (gvh.gps.getWaypointPosition(destname).x + d_d
								* robotNum * Math.cos(theta_r + difference)),
						(int) (gvh.gps.getWaypointPosition(destname).y - d_d
								* robotNum / 2 * Math.sin(theta_r + difference)),
						0);

			}

			else {
				return null;
			}

		}
	}

}