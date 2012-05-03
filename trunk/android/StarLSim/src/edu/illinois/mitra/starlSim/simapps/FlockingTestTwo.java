package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.List;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.RobotMotion;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public class FlockingTestTwo extends LogicThread {

		private enum STAGE { START, MOVE, DONE }
        private STAGE stage = STAGE.START;

        private RobotMotion moat;

        private int n_waypoints;
        private int cur_waypoint = 0;
        PositionList pl = new PositionList();
        String wpn = "wp";
        
        public FlockingTestTwo(GlobalVarHolder gvh) {
                super(gvh);
                
                moat = gvh.plat.moat;
                MotionParameters param = new MotionParameters();
                param.COLAVOID_MODE = MotionParameters.BUMPERCARS;
                moat.setParameters(param);
                //n_waypoints = gvh.gps.getWaypointPositions().getNumPositions();
                n_waypoints = Integer.MAX_VALUE;
                String n = wpn + gvh.id.getName() + cur_waypoint;
                pl.update(new ItemPosition(n, 2000, 2000, 0));
        }

        @Override
        public List<Object> callStarL() {
                String robotName = gvh.id.getName();
                Integer robotNum = Integer.parseInt(robotName.substring(3)); // assumes: botYYY
                
                while(true) {                   
                        switch (stage) {
                        case START:
                                //gvh.trace.traceStart();
                                stage = STAGE.MOVE;
                               //moat.goTo(gvh.gps.getWaypointPosition("DEST"+cur_waypoint));
                                //System.out.println(robotName + ": Starting motion!");
                                break;
                        
                        case MOVE:
                                if(!moat.inMotion) {
                                        if(cur_waypoint < n_waypoints) {
                                                //System.out.println(robotName + ": I've stopped moving!");
                                                
                                                //moat.goTo(gvh.gps.getWaypointPosition(wpn + cur_waypoint));
                                                String n = wpn + gvh.id.getName() + cur_waypoint;
                                                moat.goTo(pl.getPosition(n));
                                                
                                                //System.out.println(robotName + ": New destination is (" + pl.getPosition(n).x + ", " + pl.getPosition(n).y + ")!");

                                                cur_waypoint ++;
                                                n = wpn + gvh.id.getName() + cur_waypoint;
                                                
                                                
                                                // circle formation
                                                int x = 0, y = 0, theta = 0;
                                                
                                                PositionList plAll = gvh.gps.getPositions();
                                                for (ItemPosition rp : plAll.getList()) {
                                                        x += rp.x;
                                                        y += rp.y;
                                                        theta += rp.angle;
                                                }
                                                int N = plAll.getNumPositions();
                                                int r = 130; // ~30mm
                                                x /= N;
                                                y /= N; // x and y define the centroid of a circle with radius N*r
                                                theta /= N;
                                                
                                                //double m = (robotNum % 3 == 0) ? 0.33 : 1; // todo: concentric circles?
                                                double m = 1.0;
                                                
                                                x += N*m*r*Math.sin(robotNum);
                                                y += N*m*r*Math.cos(robotNum);
                                                //pl.update(new ItemPosition(n, robotNum * 100, 100 * ((robotNum % 2 == 0) ? 0 : 1), 0));
                                                pl.update(new ItemPosition(n, x, y, theta));
                                                
                                                
                                                // todo: after circle formation, calculate new position based on positions of nearest left and right neighbor (on the circle)
                                        } else {
                                                stage = STAGE.DONE;
                                        }
                                }
                                break;

                        case DONE:
                                gvh.trace.traceEnd();
                                return Arrays.asList(results);
                        }
                        
                        gvh.sleep(100);
                }
        }
}