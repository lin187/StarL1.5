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

import edu.illinois.mitra.starl.comms.LossyMessageSender;
import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.PickedLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;
import edu.illinois.mitra.starlSim.main.SimSettings;



public class FlockingWithoutComms extends LogicThread implements MessageListener {

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
	private int d_r=500; //Some distance that each robot will be from the nearest robot 
	private double theta_r=Math.PI/4; //0<theta_r<2*PI, theta_r != PI/2, 3*PI/2... Be wary of angles <PI/2.7
	private long t_0=0;
	int count=0;
	
	private long delta_t=10;
	private int a=100;
	
	
	ItemPosition leaderstart=new ItemPosition("goHere",1000, 1000, 0);
	private int numbots = gvh.id.getParticipants().size();

	private double[] ratio = new double[numbots];
	private Object[] names =gvh.id.getParticipants().toArray();

	
	String robotName = gvh.id.getName();
    Integer robotNum = Integer.parseInt(robotName.substring(3));
    LossyMessageSender lms = new LossyMessageSender(gvh);
	
	
	
	PositionList posList = new PositionList();
	private final static String SYNC_START = "1";
	
	
	private enum STAGE { START, SYNC, LE, MOVE,WAYPOINT_CALC,WAIT, INIT_CLOCK,BROADCAST, WAYPOINT_TRAVEL, WAIT_TO_ARRIVE, DONE };
	private STAGE stage= STAGE.START;
	
	public FlockingWithoutComms(GlobalVarHolder gvh) {
		super(gvh);
		
		
		// Get the list of position to travel to
		for(ItemPosition ip : gvh.gps.getWaypointPositions().getList()) {
			toVisit.add(ip.name);
		}
		
		// Progress messages are broadcast with message ID 99
		gvh.comms.addMsgListener(99, this);
		gvh.comms.addMsgListener(98, this);
		gvh.comms.addMsgListener(97, this);
		gvh.comms.addMsgListener(96, this);

		// Make sure waypoints were provided
		if(gvh.gps.getWaypointPositions().getNumPositions() == 0) System.out.println("This application requires waypoints to travel to!");
	
		sync = new BarrierSynchronizer(gvh);
		le = new PickedLeaderElection(gvh);
		for(int i=0; i<numbots;i++)
			{arrived.add("bot"+i);
			ratio[i]=1;
			}
	}

	
	@Override
	public List<Object> callStarL() {
		//System.out.print(gvh.id.getParticipants().toArray());
			//Declares leader
	    	while (running){
				gvh.sleep(100);
				gvh.plat.setDebugInfo(gvh.id.getParticipants().toString());
				switch (stage) {
				case START:
					sync.barrierSync(SYNC_START);
					stage = STAGE.SYNC;
					gvh.log.d(TAG, "Syncing...");
					System.out.println("Syncing..." + name);
					break;
				case SYNC:
					if (sync.barrierProceed(SYNC_START)) {
						stage = STAGE.LE;
						le.elect();
						gvh.log.d(TAG, "Synced!");
						
					}
					break;
				case LE:
					if(le.getLeader() != null) {
						gvh.log.d(TAG, "Electing...");
						leader = le.getLeader();
						iamleader = leader.equals(name);
						System.out.println("Robot Leader? "+leader);
						stage = STAGE.MOVE;
					
					}
					break;
					
				case MOVE:
					
					gvh.plat.moat.goTo(startpoint());
					boolean motionSuccess = true;
					while(gvh.plat.moat.inMotion) { 
						gvh.sleep(10);
						if(!toVisit2.contains("START POINT" + robotNum)) {
							motionSuccess = false;
							break;
						}				
					}
					
					// If Arrival of the LAST robot
					if(motionSuccess && toVisit2.isEmpty()) {
						stage = STAGE.MOVE;
						
					}
					else if (!toVisit2.isEmpty()) {
					toVisit2.remove("START POINT" + robotNum);
					}
					
					if(toVisit2.isEmpty()) { 
						stage = STAGE.WAIT_TO_ARRIVE;
					} else {
						stage = STAGE.MOVE;
					}
					break;
					
				case WAIT_TO_ARRIVE:
					
					motionSuccess = true;
					while(gvh.plat.moat.inMotion) { 
						gvh.sleep(10);
						if(!toVisit2.contains(destname)) {
							motionSuccess = false;
							break;
						}				
					}
					
					// If Arrives
					if(motionSuccess) {
						RobotMessage inform = new RobotMessage("ALL", name, 99, "bot"+robotNum);
						gvh.comms.addOutgoingMessage(inform);
						arrived.remove("bot"+robotNum);
						}
					
						stage = STAGE.WAYPOINT_CALC;
						
					
					break;
					
				case WAYPOINT_CALC:
					//Calculation of waypoints for other robots
					if (arrived.isEmpty()) 
					{
					stage=STAGE.INIT_CLOCK;
					break;}
					else{
						stage=STAGE.WAYPOINT_CALC;
						gvh.sleep(10);

						}
					break;
					
				case INIT_CLOCK:
				t_0=gvh.time();
				stage=STAGE.BROADCAST;
				break;
				
				case BROADCAST:
					
					if (!iamleader) {
						MessageContents rm = new MessageContents(
								gvh.gps.getMyPosition().x + " "
										+ gvh.gps.getMyPosition().y + " " + robotNum + " " + gvh.gps.getMyPosition().velocity);
						RobotMessage inform = new RobotMessage(leader, name, 98, rm);
						gvh.comms.addOutgoingMessage(inform);
					}
					
					
					stage= STAGE.WAYPOINT_TRAVEL;
					break;
				
				case WAYPOINT_TRAVEL:
					if (!toVisit.isEmpty())
					{ if (iamleader && count==0)
					{ItemPosition point = newpoint(0);
					MessageContents Message = new MessageContents(point.toString() + " 0 "+ratio[0]);
					RobotMessage go_to = new RobotMessage((String) names[0], name, 97, Message);
					gvh.comms.addOutgoingMessage(go_to);
					
					 point = newpoint(1);
					 Message = new MessageContents(point.toString()+ " 1 "+ratio[1]);
					 go_to = new RobotMessage((String) names[1], name, 97, Message);
						gvh.comms.addOutgoingMessage(go_to);
					
					point = newpoint(2);
					 Message = new MessageContents(point.toString()+ " 2 "+ratio[2]);
					 go_to = new RobotMessage((String) names[2], name, 97, Message);
						gvh.comms.addOutgoingMessage(go_to);
					
					point = newpoint(3);
					 Message = new MessageContents(point.toString()+ " 3 "+ratio[3]);
					 go_to = new RobotMessage((String) names[3], name, 97, Message);
						gvh.comms.addOutgoingMessage(go_to);

						count++;
					}	
					if (iamleader && count>0)
					{ItemPosition point = newpoint(0);
					MessageContents Message = new MessageContents(point.toString() + " 0 "+ratio[0]);
					RobotMessage go_to = new RobotMessage((String) names[0], name, 97, Message);
					gvh.comms.addOutgoingMessage(go_to);
					
					 point = newpoint(1);
					 Message = new MessageContents(point.toString()+ " 1 "+ratio[1]);
					 go_to = new RobotMessage((String) names[1], name, 97, Message);
					 lms.setStaticLossRate((String) names[1], 0.9); 
					 lms.send(go_to);	
					
					point = newpoint(2);
					 Message = new MessageContents(point.toString()+ " 2 "+ratio[2]);
					 go_to = new RobotMessage((String) names[2], name, 97, Message);
					 lms.setStaticLossRate((String) names[2], 0.9); 
					 lms.send(go_to);	
					
					point = newpoint(3);
					 Message = new MessageContents(point.toString()+ " 3 "+ratio[3]);
					 go_to = new RobotMessage((String) names[3], name, 97, Message);
					 lms.setStaticLossRate((String) names[3], 0.2); 
					 lms.send(go_to);	
					}	
						
						while (gvh.plat.moat.inMotion) 
						{
							gvh.sleep(1);
						}
						if(!toVisit.isEmpty())
						toVisit.remove(toVisit.first());
						
					} 
					
										
					if(toVisit.isEmpty() && iamleader) { 
						RobotMessage end = new RobotMessage("ALL", name, 96, " ");
						gvh.comms.addOutgoingMessage(end);
						stage = STAGE.DONE;
						break;
					} 
					else 
						{stage = STAGE.BROADCAST;
						gvh.sleep(20);
						break;}
					
				case WAIT:
					gvh.plat.moat.motion_stop();
					gvh.sleep(100);
					gvh.plat.moat.motion_resume();
					stage=STAGE.WAYPOINT_TRAVEL;
					break;
					
								
				case DONE:
					System.out.println(name + " is done.");
					gvh.plat.moat.motion_stop();
					le.cancel();
					sync.cancel();
					gvh.comms.removeMsgListener(99);
					gvh.comms.removeMsgListener(98);
					gvh.comms.removeMsgListener(97);
					gvh.comms.removeMsgListener(96);
					return Arrays.asList(results);
				}
						
		
	    	}
		return null;
	}


	@Override
public void messageReceied(RobotMessage m) {
		if (m.getMID() == 99) {
		synchronized(arrived) {
			// Checks if all robots have arrived
			arrived.remove(m.getContents(0));
						
		}
		}
		if (m.getMID() == 98) {
			if(!toVisit.isEmpty())
				{
				gvh.plat.moat.motion_resume();
				String[] data = m.getContents(0).split(" ");  
				int x_coordinate= Integer.parseInt(data[0]);
				int y_coordinate= Integer.parseInt(data[1]);
				int robotnumber=Integer.parseInt(data[2]);
				int current_velocity=Integer.parseInt(data[3]);
				
				double ratio_velocity;
				
				double distance_y=gvh.gps.getPosition(leader).y -y_coordinate;
				//double distance_x=gvh.gps.getPosition(leader).x-x_coordinate;
				
				int divide_two=(robotnumber+1)/2;
				
				ratio_velocity=distance_y/(d_r*divide_two*Math.cos(theta_r));
				
				if (y_coordinate > gvh.gps.getMyPosition().y)
				{
					synchronized(stage)
					{
						stage=STAGE.WAIT;
					}
				} 

				
					synchronized(ratio)
					{
						ratio[robotnumber]=ratio_velocity;
					}	
						
				
			}
		}

		
		if (m.getMID() == 97) {
			
			
			String output = m.getContents(0).replaceAll("," , "");
			String[] data = output.split(" ");
			MotionParameters M;
			M=new MotionParameters();
			M.LINSPEED_MAX=(int) (Double.parseDouble(data[5])*M.LINSPEED_MAX);
			M.LINSPEED_MIN=(int) (Double.parseDouble(data[5])*M.LINSPEED_MIN);

			ItemPosition go_to= startpoint();
			go_to.x=Integer.parseInt(data[1]);
			go_to.y=Integer.parseInt(data[2]);
			gvh.plat.moat.goTo(go_to,M);
	
		}
		if (m.getMID() == 96) {
			synchronized (stage)
			{
				stage=STAGE.DONE;
			}

		}
		}
	
	
	
private ItemPosition startpoint() {
		//IF USING SIMULATION UNCOMMENT
		String robotName = gvh.id.getName();
        Integer robotNum = Integer.parseInt(robotName.substring(3)); // assumes: botYYY
    	
        //IF ON ACUTAL ROBOTS UNCOMMENT
        /*
         *  Integer robotNum=0;
        Object [] botarray=gvh.id.getParticipants().toArray();
        for(int i=0; i<gvh.id.getParticipants().size();i++)
        {
        if(name != (String) botarray[i]){}
        else 
        	{  	robotNum=i;
        	}
          }
         */
        
        
        toVisit2.add("START POINT" + robotNum);
        if(iamleader){
		return new ItemPosition("goHere",1000, 1000, 0);
		}
        else {
        	if (robotNum % 2 == 0 && !iamleader)
    		{return new ItemPosition("goHere",(int) (1000 +d_r*robotNum/2*Math.cos(theta_r)), (int) (1000 -d_r*robotNum/2*Math.sin(theta_r)), 0);
    		}
        	
        	if (robotNum % 2 == 1)
    		{return new ItemPosition("goHere",(int) (1000 -d_r*(robotNum+1)/2*Math.cos(theta_r)), (int) (1000 -d_r*(robotNum+1)/2*Math.sin(theta_r)), 0);
    		}
        	else 
        	{return null;}

        }
	}
private ItemPosition newpoint(int input) {
	
    Integer robotNum = input; // assumes: botYYY
	if(!toVisit.isEmpty())
		destname = toVisit.first();

    if(robotNum==0){
    	
		return new ItemPosition("goHere", gvh.gps.getWaypointPosition(destname).x,  gvh.gps.getWaypointPosition(destname).y, 0);
	}
    else {
    	if (robotNum % 2 == 0)
		{return new ItemPosition("goHere",(int) (gvh.gps.getWaypointPosition(destname).x +d_r*robotNum/2*Math.cos(theta_r)), (int) (gvh.gps.getWaypointPosition(destname).y -d_r*robotNum/2*Math.sin(theta_r)), 0);
		}
    	
    	if (robotNum % 2 == 1)
		{return new ItemPosition("goHere",(int) (gvh.gps.getWaypointPosition(destname).x -d_r*(robotNum+1)/2*Math.cos(theta_r)), (int) (gvh.gps.getWaypointPosition(destname).y -d_r*(robotNum+1)/2*Math.sin(theta_r)), 0);
		}
    	else 
    	{return null;}

    }
}
	


	
	
	
}