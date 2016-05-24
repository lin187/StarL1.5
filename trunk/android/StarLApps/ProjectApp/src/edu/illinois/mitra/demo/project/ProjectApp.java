package edu.illinois.mitra.demo.project;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.*;
import edu.illinois.mitra.starl.motion.*;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;

public class ProjectApp extends LogicThread {
    private static final boolean RANDOM_DESTINATION = false;
    public static final int ARRIVED_MSG = 22;
    private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
    private volatile MotionParameters param = DEFAULT_PARAMETERS;
    // this is an ArrayList of HashMap. Each HashMap element in the array will contain one set of waypoints
    final ArrayList<HashMap<String, ItemPosition>> destinations = new ArrayList<>();
    private int numSetsWaypoints = 4;
    private int idNum;
    int robotIndex;
    int currentSet;
    boolean goingToRandom;
    Random random = new Random();
    int botRadius;
    // used to find path through obstacles
    Stack<ItemPosition> pathStack;
    RRTNode kdTree = new RRTNode();
    ObstacleList obsList;
    //obsList is a local map each robot has, when path planning, use this map
    ObstacleList obEnvironment;
    //obEnvironment is the physical environment, used when calculating collisions
    ItemPosition currentDestination, midDestination;

    private enum Stage {
        PICK, DONE, ELECT, MIDWAY, PICKRANDOM, PLAN, GO, GOAL
    };

    private Stage stage = Stage.ELECT;

    public ProjectApp(GlobalVarHolder gvh) {
        super(gvh);
        // instantiates each HashMap object in the array
        for(int i = 0; i < numSetsWaypoints; i++) {
            destinations.add(new HashMap<String, ItemPosition>());
        }
        MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
        settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLBACK);
        //settings = settings.COLAVOID_MODE(MotionParameters.COLAVOID_MODE_TYPE.USE_COLAVOID);
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);
        botRadius = param.ROBOT_RADIUS;
        // this loop adds each set of waypoints i to the hashmap at destinations(i)
        for(ItemPosition i : gvh.gps.getWaypointPositions()) {
            String setNumStr = i.getName().substring(0,1);
            int setNum = Integer.parseInt(setNumStr);
            destinations.get(setNum).put(i.getName(), i);
        }

        //point the environment to internal data, so that we can update it
        obEnvironment = gvh.gps.getObspointPositions();

        //download from environment here so that all the robots have their own copy of visible ObstacleList
        // this list seems to always be empty. possibly a bug
        obsList = gvh.gps.getViews().elementAt(robotIndex) ;

        gvh.comms.addMsgListener(this, ARRIVED_MSG);
        String intValue = name.replaceAll("[^0-9]", "");
        idNum = Integer.parseInt(intValue);
        robotIndex = idNum;
    }

    @Override
    public List<Object> callStarL() {
        // assume environment is closed: find maximum positions of obstacles in environment
        int environmentMinX = Integer.MAX_VALUE;
        int environmentMaxX = Integer.MIN_VALUE;
        int environmentMinY = Integer.MAX_VALUE;
        int environmentMaxY = Integer.MIN_VALUE;
        int environmentXSize;
        int environmentYSize;

        for (Obstacles o : obEnvironment.ObList) {
            for (Point3d obstaclePoints : o.getObstacleVector()) {
                environmentMinX = (int)Math.min(obstaclePoints.getX(), environmentMinX);
                environmentMaxX = (int)Math.max(obstaclePoints.getX(), environmentMaxX);
                environmentMinY = (int)Math.min(obstaclePoints.getY(), environmentMinY);
                environmentMaxY = (int)Math.max(obstaclePoints.getY(), environmentMaxY);
            }
        }
        environmentXSize = environmentMaxX - environmentMinX;
        environmentYSize = environmentMaxY - environmentMinY;

        currentSet = 0;
        while(true) {

            obEnvironment.updateObs();
            //obsList.updateObs();
            // if current set is empty, increment to next set, quit if no sets left
            if(destinations.get(currentSet).isEmpty()) {
                if(currentSet+1 >= numSetsWaypoints) {
                    stage = Stage.DONE;
                }
                else {
                    currentSet++;
                    obsList.ObList.clear();
                    stage = Stage.ELECT;
                }
            }

                switch(stage) {
                    // select bot0 to be the leader
                    case ELECT:
					    if(idNum== 0) {
                            stage = Stage.PICK;
                        }
                        else {
                            stage = Stage.PICKRANDOM;
                        }
                        break;
                    // pick a destination point
                    case PICK:
                                if(gvh.plat.moat.inMotion) {
                                    gvh.plat.moat.motion_stop();
                                }
                                currentDestination = getRandomElement(destinations.get(currentSet));
                                goingToRandom = false;
                                stage = Stage.PLAN;
                                break;
                    // pick a random point
                    case PICKRANDOM:
                                goingToRandom = true;
                                int x = random.nextInt(environmentXSize);
                                int y = random.nextInt(environmentYSize);
                                currentDestination = new ItemPosition("random", x, y, 0);
                                stage = Stage.PLAN;
                                break;
                    case PLAN:

                                RRTNode path = new RRTNode(gvh.gps.getPosition(name).x, gvh.gps.getPosition(name).y);
                                // TODO: change this dynamically based on current obstacles
                                // obEnvrinoment vs. obsList

                                //obsList.updateObs();
                                obEnvironment.updateObs();
                                ObstacleList allObstacles = new ObstacleList();
                                // how to add all the current robot positions to plan around these?
                                // think that is what is done below
                                allObstacles.addObstacles(obEnvironment.ObList);
                                for(int i = 0; i< gvh.gps.get_robot_Positions().getNumPositions(); i++){
                                    int otherX = gvh.gps.get_robot_Positions().getList().get(i).getX();
                                    int otherY = gvh.gps.get_robot_Positions().getList().get(i).getY();
                                    int radius = botRadius;
                                    // only add other robots
                                    if (i != robotIndex) {
                                        Obstacles oTmp = new Obstacles(otherX, otherY);
                                        oTmp.add(otherX + radius, otherY);
                                        oTmp.add(otherX - radius, otherY);
                                        oTmp.add(otherX, otherY + radius);
                                        oTmp.add(otherX, otherY - radius);
                                        oTmp.timeFrame = System.currentTimeMillis() + 10000;
                                        allObstacles.addObstacle(oTmp);
                                    }
                                }
                                allObstacles.updateObs();
                                // TODO: want to use smallest size in RRT search as possible (otherwise, the found path may be very large)
                                // one strategy: start small, try to find, if it fails, double x, and y sizes?
                                // TODO: perform re-planning when en route to the target? e.g., in the Stage.MIDWAY state?
                                pathStack = path.findRoute(currentDestination, 50, allObstacles, 0, environmentXSize, 0, environmentYSize, (gvh.gps.getPosition(name)), (int) botRadius);

                                kdTree = RRTNode.stopNode;
                                // if path found, go to MIDWAY
                                if (pathStack != null) {
                                    midDestination = null;
                                    stage = Stage.MIDWAY;
                                }
                                // if path not found, go back to PICK or PICKRANDOM
                                if (pathStack == null && goingToRandom) {
                                    stage = Stage.PICKRANDOM;
                                }
                                if (pathStack == null && !goingToRandom) {
                                    stage = Stage.PICK;
                                }
                        break;

                    case MIDWAY:
                        if(!gvh.plat.moat.inMotion) {
                            // motion towards first point in pathStack hasn't started yet
                            if (midDestination == null) {
                                stage = Stage.GO;
                                break;
                            }
                            // bot has arrived at a midDestination point, and now should go to next one
                            if (atDestination(midDestination) && !pathStack.empty()) {
                                stage = Stage.GO;
                                break;
                            }
                            // did not reach midDestination, so go back to pick
                            if (!atDestination(midDestination)) {
                                pathStack.clear();
                                if (!goingToRandom) {
                                    stage = Stage.PICK;
                                } else {
                                    stage = Stage.PICKRANDOM;
                                }
                                break;
                            }
                            // reached destination, which is the last point in pathStack
                            if (atDestination(midDestination) && pathStack.empty()) {
                                stage = Stage.GOAL;
                                break;
                            }
                        }
                        // if in motion, don't change stage to remain in MIDWAY
                        break;

                    case GO:
                        midDestination = pathStack.pop();
                        gvh.plat.moat.goTo(midDestination, obsList);
                        stage = Stage.MIDWAY;
                        break;

                    case GOAL:
                        if(goingToRandom) {
                            stage = Stage.PICKRANDOM;
                        }
                        // if goal waypoint reached, remove from destinatins and send message to other bots
                        else {
                            destinations.get(currentSet).remove(currentDestination.getName());
                            RobotMessage inform = new RobotMessage("ALL", name, ARRIVED_MSG, currentDestination.getName());
                            gvh.comms.addOutgoingMessage(inform);
                            // if this is the highest numbered bot and there are more points, go to pick
                            if (idNum == gvh.id.getParticipants().size() - 1 && !destinations.get(currentSet).isEmpty()) {
                                stage = Stage.PICK;
                            } else {
                                stage = Stage.PICKRANDOM;
                            }
                        }
                        break;

                    case DONE:
                        gvh.plat.moat.motion_stop();
                        return null;
                }
            sleep(100);
        }
    }

    @Override
    protected void receive(RobotMessage m) {
        String posName = m.getContents(0);
        // remove destination other bot has arrived at
        if(destinations.get(currentSet).containsKey(posName))
            destinations.get(currentSet).remove(posName);

       String fromID = m.getFrom().replaceAll("[^0-9]", "");
       int fromInt = Integer.parseInt(fromID);
       // if i'm next robot, or the last robot and there are more points, go to pick
       if(idNum == fromInt + 1) {
           stage = Stage.PICK;
       }
    }

    private static final Random rand = new Random();

    @SuppressWarnings("unchecked")
    private <X, T> T getRandomElement(Map<X, T> map) {
        if(RANDOM_DESTINATION)
            return (T) map.values().toArray()[rand.nextInt(map.size())];
        else
            return (T) map.values().toArray()[0];
    }

    // return true if robot is within goal radius of dest
    private boolean atDestination(ItemPosition dest) {
        if(midDestination == null) {
            return false;
        }
        return gvh.gps.getPosition(name).distanceTo(dest)<param.GOAL_RADIUS;
    }
}