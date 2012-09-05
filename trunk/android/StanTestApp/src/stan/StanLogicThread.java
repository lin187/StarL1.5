package stan;

import java.util.LinkedList;
import java.util.List;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.RobotMotion;
import edu.illinois.mitra.starl.objects.ItemPosition;

/**
 * Stanley Bak's test application. Source copied from other projects, 
 * initially from GpsTestApp
 * 
 * @author Stankey Bak (sbak2@illinois.edu)
 *
 */
public class StanLogicThread extends LogicThread implements MessageListener
{
	boolean isLeader;
	final String LEADER_NAME = "bot0"; // only works for simulation
	final String FOLLOWER_NAME = "bot1"; // only works for simulation
	final String FOLLOWER2_NAME = "bot2"; // only works for simulation
	
	private enum STAGE { START, MOVE }
	private STAGE stage = STAGE.START;
	
	// message ids
	private final int MSG_WAYPOINT_UPDATE = 1000;
	private final int MSG_DONE = 1001;

	private RobotMotion motion;
	
	// for leader
	private int numWaypoints;
	private int curWaypoint = 0;
	
	private ItemPosition waypoints[] = 
	{
		new ItemPosition("waypoint0", 100, 100, 0),
		new ItemPosition("waypoint0", 1000, 100, 0),
		new ItemPosition("waypoint0", 1000, 1000, 0),
		new ItemPosition("waypoint0", 100, 1000, 0),
		new ItemPosition("waypoint0", 200, 500, 0),
	};
	
	// for follower
	private boolean finishedFollowing = false;
	private int gotoX = Integer.MAX_VALUE;
	private int gotoY = 0;
	
	int goingToX = Integer.MAX_VALUE;
	int goingToY = 0;
	
	// for testing concurrent modification issues
	int lastSum = 0;
	LinkedList <Integer> longList = new LinkedList <Integer>(); 
	
	public StanLogicThread(GlobalVarHolder gvh) 
	{
		super(gvh);
		
		isLeader = gvh.id.getName().equals(LEADER_NAME);
		
		//gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		motion = gvh.plat.moat;
		numWaypoints = waypoints.length;
		
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
			if (gotoX != Integer.MAX_VALUE)
			{
				ItemPosition ip = new ItemPosition("follower_pos", gotoX, gotoY, 0);
								
				motion.goTo(ip);
				
				goingToX = gotoX; // for debug drawing
				goingToY = gotoY; // for debug drawing
				
				gotoX = Integer.MAX_VALUE;
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
				
				leaderGoto(waypoints[curWaypoint]);
				break;
			
			case MOVE:
				if(!motion.inMotion) {
					if(curWaypoint + 1 < numWaypoints) 
						curWaypoint ++;
					else 
						curWaypoint = 0;
					
					longList.clear();
					
					for (int x = 0; x < 100000; ++x)
						longList.add((int)Math.random());
					
					leaderGoto(waypoints[curWaypoint]);
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
			
			gotoX = x;
			gotoY = y;
			
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
	
	int replaceWaypointIndex = 0;
	
	public void receivedPointInput(int x, int y)
	{
		if (isLeader)
		{
			waypoints[replaceWaypointIndex].x = x;
			waypoints[replaceWaypointIndex].y = y;
					
			if(replaceWaypointIndex + 1 < numWaypoints) 
				replaceWaypointIndex ++;
			else 
				replaceWaypointIndex = 0;
		}
	}
}
