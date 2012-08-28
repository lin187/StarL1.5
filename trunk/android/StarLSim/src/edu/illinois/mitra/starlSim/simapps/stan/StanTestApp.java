package edu.illinois.mitra.starlSim.simapps.stan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.RobotMotion;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimSettings;

/**
 * Stanley Bak's test application. Source copied from other projects, 
 * initially from GpsTestApp
 * 
 * @author Stankey Bak (sbak2@illinois.edu)
 *
 */
public class StanTestApp extends LogicThread implements MessageListener
{
	private boolean isLeader;
	final String LEADER_NAME = SimSettings.BOT_NAME + "0"; // only works for simulation
	final String FOLLOWER_NAME = SimSettings.BOT_NAME + "1"; // only works for simulation
	final String FOLLOWER2_NAME = SimSettings.BOT_NAME + "2"; // only works for simulation
	
	private enum STAGE { START, MOVE }
	private STAGE stage = STAGE.START;
	
	// message ids
	private final int MSG_WAYPOINT_UPDATE = 1000;
	private final int MSG_DONE = 1001;

	private RobotMotion motion;
	
	// for leader
	private int numWaypoints;
	private int curWaypoint = 0;
	
	// for follower
	private boolean finishedFollowing = false;
	private Point gotoPoint = null; 
	private Point goingToPoint = null;
	
	public StanTestApp(GlobalVarHolder gvh) 
	{
		super(gvh);
		
		isLeader = gvh.id.getName().equals(LEADER_NAME);
		
		//gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		motion = gvh.plat.moat;
		numWaypoints = gvh.gps.getWaypointPositions().getNumPositions()-1;
		
		if(numWaypoints == -1) 
			throw new RuntimeException("Waypoints were not defined in input file");
		
		// set motion parameters
		
		if (isLeader)
		{
			MotionParameters leaderParams = new MotionParameters();
			leaderParams.LINSPEED_MAX = leaderParams.LINSPEED_MIN = 200;
			motion.setParameters(leaderParams);
		}
		else
		{
			MotionParameters mp = new MotionParameters();
			mp.STOP_AT_DESTINATION = true;
			mp.LINSPEED_MAX = mp.LINSPEED_MIN = 150;
			motion.setParameters(mp);
		}
		
		registerListeners();
	}

	@Override
	public List<Object> callStarL() 
	{
		List <Object> rv = null;
		
		if (isLeader)
			rv = leaderLogic();
		else
			rv = followerLogic();
		
		return rv;
	}

	private List<Object> followerLogic()
	{
		finishedFollowing = false;
		
		while (!finishedFollowing) 
		{
			if (gotoPoint != null)
			{
				ItemPosition ip = new ItemPosition("follower_pos", gotoPoint.x, gotoPoint.y, 0);
								
				motion.goTo(ip);
				
				goingToPoint = gotoPoint; // for debug drawing
				gotoPoint = null;
			}
			
			gvh.sleep(100);
		}
		
		return returnResults();
	}

	private List<Object> leaderLogic()
	{
		boolean keepLooping = true;
		
		while(keepLooping == true) {			
			switch (stage) {
			case START:
				gvh.trace.traceSync("LAUNCH");
				stage = STAGE.MOVE;
				
				leaderGoto(gvh.gps.getWaypointPosition("DEST"+curWaypoint));
				break;
			
			case MOVE:
				if(!motion.inMotion) {
					if(curWaypoint < numWaypoints) 
						curWaypoint ++;
					else 
						curWaypoint = 0;
					
					leaderGoto(gvh.gps.getWaypointPosition("DEST"+curWaypoint));
				}
				break;
			}
			
			gvh.sleep(100);
		}
		
		return returnResults();
	}

	private void leaderGoto(ItemPosition ip)
	{
		int SEND_OFFSET_X = -300;
		int SEND_OFFSET_Y_1 = 500;
		int SEND_OFFSET_Y_2 = -500;
		
		MessageContents msg = new MessageContents("" + (ip.x + SEND_OFFSET_X), "" + (ip.y + SEND_OFFSET_Y_1));
		RobotMessage updateMsg = new RobotMessage(FOLLOWER_NAME, name, MSG_WAYPOINT_UPDATE, msg);
		
		MessageContents msg2 = new MessageContents("" + (ip.x + SEND_OFFSET_X), "" + (ip.y + SEND_OFFSET_Y_2));
		RobotMessage updateMsg2 = new RobotMessage(FOLLOWER2_NAME, name, MSG_WAYPOINT_UPDATE, msg2);
		
		gvh.comms.addOutgoingMessage(updateMsg);
		gvh.comms.addOutgoingMessage(updateMsg2);
		
		motion.goTo(ip);
	}

	@Override
	public void messageReceied(RobotMessage m)
	{
		switch(m.getMID()) {
		case MSG_WAYPOINT_UPDATE:
			List <String> parts = m.getContentsList();
			
			int x = Integer.parseInt(parts.get(0));
			int y = Integer.parseInt(parts.get(1));
			
			gotoPoint = new Point(x,y);
			
			break;
			
		case MSG_DONE:
			finishedFollowing = true;
			break;
		}
	}
	
	public void cancel() 
	{
		unregisterListeners();
	}
	
	private void registerListeners()
	{
		gvh.comms.addMsgListener(MSG_WAYPOINT_UPDATE, this);
		gvh.comms.addMsgListener(MSG_DONE, this);
	}
	
	private void unregisterListeners()
	{
		gvh.comms.removeMsgListener(MSG_WAYPOINT_UPDATE);
		gvh.comms.removeMsgListener(MSG_DONE);		
	}
	
	public void draw(Graphics2D g)
	{
		final int SIZE = 20; // for drawing the X
		
		if (!isLeader)
		{
			if (goingToPoint != null)
			{
				g.setColor(Color.gray);
				g.setStroke(new BasicStroke(10));
				
				int x = goingToPoint.x;
				int y = goingToPoint.y;
				
				g.drawString(x + ", " + y, x + 2*SIZE, y);
				
				g.drawLine(x - SIZE, y - SIZE, x + SIZE, y + SIZE);
				g.drawLine(x - SIZE, y + SIZE, x + SIZE, y - SIZE);
			}
		}
	}
	
	int replaceWaypointIndex = 0;
	
	public void receivedPointInput(Point p)
	{
		if (isLeader)
		{
			// replace replaceWaypointIndex
			gvh.gps.getWaypointPosition("DEST"+replaceWaypointIndex).x = p.x;
			gvh.gps.getWaypointPosition("DEST"+replaceWaypointIndex).y = p.y;

			if(replaceWaypointIndex < numWaypoints) 
				replaceWaypointIndex ++;
			else 
				replaceWaypointIndex = 0;
		}
	}
}
