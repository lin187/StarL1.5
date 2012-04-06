package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;
import edu.illinois.mitra.starlSim.main.SimApp;

public class FlockingTestApp extends SimApp {

	private enum STAGE { START, MOVE, DONE }
	private STAGE stage = STAGE.START;

	private RobotMotion moat;
	
	
	private int n_waypoints;
	private int cur_waypoint = 0;
	PositionList pl = new PositionList();
	String wpn = "wp";
	
	public FlockingTestApp(String name, HashMap<String,String> participants, SimComChannel channel, IdealSimGpsProvider gps, ItemPosition initpos) {
		super(name, participants, channel, gps, initpos, "C:\\");
		
		moat = gvh.plat.moat;
		//n_waypoints = gvh.gps.getWaypointPositions().getNumPositions();
		n_waypoints = Integer.MAX_VALUE;
		String n = wpn + gvh.id.getName() + cur_waypoint;
		pl.update(new ItemPosition(n, 500, 500, 0));
	}

	public List<String> call() throws Exception {
		String robotName = gvh.id.getName();
		Integer robotNum = Integer.parseInt(robotName.substring(3, robotName.length())); // assumes: botYYY
		
		while(true) {			
			switch (stage) {
			case START:
				//gvh.trace.traceStart();
				stage = STAGE.MOVE;
				moat.goTo(gvh.gps.getWaypointPosition("DEST"+cur_waypoint));
				System.out.println(robotName + ": Starting motion!");
				break;
			
			case MOVE:
				if(!moat.inMotion) {
					if(cur_waypoint < n_waypoints) {
						System.out.println(robotName + ": I've stopped moving!");
						
						//moat.goTo(gvh.gps.getWaypointPosition(wpn + cur_waypoint));
						String n = wpn + gvh.id.getName() + cur_waypoint;
						moat.goTo(pl.getPosition(n));
						
						System.out.println(robotName + ": New destination is (" + pl.getPosition(n).getX() + ", " + pl.getPosition(n).getY() + ")!");

						cur_waypoint ++;
						n = wpn + gvh.id.getName() + cur_waypoint;
						
						
						// circle formation
						int x = 0, y = 0, theta = 0;
						
						PositionList plAll = gvh.gps.getPositions();
						for (ItemPosition rp : plAll.getList()) {
							x += rp.getX();
							y += rp.getY();
							theta += rp.getAngle();
						}
						int N = plAll.getNumPositions();
						int r = 30; // ~30mm
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
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}
}
