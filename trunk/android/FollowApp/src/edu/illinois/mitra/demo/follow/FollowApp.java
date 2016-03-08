package edu.illinois.mitra.demo.follow;

/**
 * Created by VerivitalLab on 2/26/2016.
 * This app was created to test the drones. The bots will each go to an assigned waypoint.
 * Once both bots have arrived at their respective waypoints, they will then go to the next waypoints.
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class FollowApp extends LogicThread {
    public static final int ARRIVED_MSG = 22;
    private int destIndex;
    private int messageCount = 0;
    private int numBots;
    private int numWaypoints;
    private boolean arrived = false;
    private boolean goForever = true;

    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
    ItemPosition currentDestination;

    private enum Stage {
        INIT, PICK, GO, DONE, WAIT
    };

    private Stage stage = Stage.INIT;

    public FollowApp(GlobalVarHolder gvh) {
        super(gvh);
        MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
        settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);
        for(ItemPosition i : gvh.gps.getWaypointPositions())
            destinations.put(i.getName(), i);
        gvh.comms.addMsgListener(this, ARRIVED_MSG);
        // bot names must be bot0, bot1, ... botn for this to work
        String intValue = name.replaceAll("[^0-9]", "");
        destIndex = Integer.parseInt(intValue);
        numBots = gvh.id.getParticipants().size();
    }

    @Override
    public List<Object> callStarL() {
        while(true) {
            switch(stage) {
                case INIT:
                    for(ItemPosition i : gvh.gps.getWaypointPositions())
                        destinations.put(i.getName(), i);
                    numWaypoints = destinations.size();
                    stage = Stage.PICK;
                case PICK:
                    arrived = false;
                    if(destinations.isEmpty()) {
                        stage = Stage.DONE;
                    } else {
                        currentDestination = getDestination(destinations, destIndex);
                        destIndex++;
                        if(destIndex >= numWaypoints) {
                            destIndex = 0;
                        }
                        gvh.plat.moat.goTo(currentDestination);
                        stage = Stage.GO;
                    }
                    break;
                case GO:
                    if(!gvh.plat.moat.inMotion) {
                        if(!goForever) {
                            if (currentDestination != null)
                                destinations.remove(currentDestination.getName());
                        }
                        RobotMessage inform = new RobotMessage("ALL", name, ARRIVED_MSG, currentDestination.getName());
                        gvh.comms.addOutgoingMessage(inform);
                        arrived = true;
                        stage = Stage.WAIT;
                    }
                    break;
                case WAIT:
                    if((messageCount == numBots - 1) && arrived) {
                        messageCount = 0;
                        stage = Stage.PICK;
                    }
                    break;
                case DONE:
                    return null;
            }
            sleep(100);
        }
    }

    @Override
    protected void receive(RobotMessage m) {
        messageCount++;
        if((messageCount == numBots - 1) && arrived) {
            messageCount = 0;
            stage = Stage.PICK;
        }
    }

    public ItemPosition getCurrentDestination() {
        return currentDestination;
    }

    @SuppressWarnings("unchecked")
    private <X, T> T getDestination(Map<X, T> map, int index) {
        return (T) map.values().toArray()[index];
    }
}
