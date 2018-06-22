package edu.illinois.linemutex;


import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.TreeSet;


public class LineMutexCompute
{
	private static LineMutexCompute instance = new LineMutexCompute();
	
	
	
	class MutexSegment
	{
		public MutexSegment(Line2D.Double s, TreeSet <Integer> p1, TreeSet<Integer> p2)
		{
			seg = s;
			mutexP1 = p1;
			mutexP2 = p2;
		}
		
		Line2D.Double seg;
		TreeSet <Integer> mutexP1;
		TreeSet <Integer> mutexP2;
	}

	// returns true if we should increment next mutex id
	private static boolean ensureSameSet(TreeSet<Integer> a, TreeSet<Integer> b, int nextMutexId)
	{
		boolean rv = false;
		TreeSet<Integer> intersection = intersect(a, b);
		
		if (intersection.size() == 0)
		{
			// need to add a mutex to all three
			
			int min = Integer.MAX_VALUE;
			
			if (a.size() > 0)
				min = Math.min(min, a.first());
			
			if (b.size() > 0)
				min = Math.min(min, b.first());
			
			if (min == Integer.MAX_VALUE)
			{
				min = nextMutexId;
				rv = true;
			}
			
			// add min to all
			a.add(min);
			b.add(min);
		}
		
		return rv;
	}
	

	// set intersection
	private static TreeSet<Integer> intersect(TreeSet<Integer> a, TreeSet<Integer> b)
	{
		TreeSet<Integer> rv = new TreeSet<Integer>();
		
		for (int aInt : a)
		{
			if (b.contains(aInt))
				rv.add(aInt);
		}
		
		return rv;
	}
	
	class InternalWaypoint
	{
		public InternalWaypoint(Point p, String col)
		{
			wp = new WayPoint(p, col);
		}
		
		public InternalWaypoint(Point2D p, String col)
		{
			Point pInt = new Point((int)Math.round(p.getX()), (int)Math.round(p.getY()));
			
			wp = new WayPoint(pInt, col);
		}
		
		public String toString()
		{
			return "[InternalWayPoint: wp = " + wp + "]";
		}
		
		WayPoint wp;
		TreeSet <Integer> mutexSet = new TreeSet <Integer>();
	};

	public static ArrayList <LineOutput> compute(
			ArrayList <LineInput> lines, 
			int waypointSpacing, 
			int robotRadius,
			int numRobots,
			int minTravelDistance, 
			Rectangle world,
			String ghostLineColor)
	{
		int DEFAULT_MUTEX_ID = -1;
		
		//System.out.println(": compute started");
		
		// step 0.1 construct initial internal waypoints
		ArrayList <InternalWaypoint> initWaypoints = inputToWaypoints(lines);
		
		//System.out.println(": 0.1 done, initWaypoints.size = " + initWaypoints.size());
		
		// step 0.25 split internalWaypoints among all the robots
		ArrayList <ArrayList <InternalWaypoint> > dividedWaypoints = 
					splitAmongRobots(initWaypoints, numRobots, minTravelDistance);
		
		//System.out.println(": 0.25 done, dividedWaypoints size = " + dividedWaypoints.size());
		
		// step 0.33 add empty start points
		addStartPoints(dividedWaypoints, robotRadius, world, ghostLineColor);
		
		//System.out.println(": 0.33 done");
					
		// step 0.5: split all intersecting lines into multiple segments
		splitAlongIntersections(dividedWaypoints, robotRadius);
		
		//System.out.println(": 0.5 done");
		
		// step 1: divide all lines into a sequence of points of at most length 'spacing'
		createWaypoints(dividedWaypoints, waypointSpacing);
		
		//System.out.println(": 1 done, dividedWaypoints size = " + dividedWaypoints.size());
					
		// step 2: construct mutexs from points that are "close enough" together
		double closeEnoughDist = 2 * robotRadius;
		int nextMutexId = 0;
		
		for (int robotI = 0; robotI < dividedWaypoints.size(); ++robotI)
		{
			ArrayList <InternalWaypoint> waypointsI = dividedWaypoints.get(robotI);
		
			for (int i = 1; i < waypointsI.size(); ++i)
			{				
				// i should basically only worry about the segment that precedes it
				Line2D.Double iSegment = waypointLine(waypointsI.get(i-1), waypointsI.get(i));
				
				// check if iSegment intersects with any non-same-robot lines
				// if it does, update the mutex
				
				for (int robotJ = 0; robotJ < dividedWaypoints.size(); ++robotJ)
				{
					if (robotI == robotJ)
						continue; // skip the current robot
					
					ArrayList <InternalWaypoint> waypointsJ = dividedWaypoints.get(robotJ);
				
					for (int j = 1; j < waypointsJ.size(); ++j)
					{
						Line2D.Double jSegment = waypointLine(waypointsJ.get(j-1), waypointsJ.get(j));
						
						// check if it_seg is close enough to waypointsJ
						
						double dist = Geometry.segSegDist(iSegment, jSegment);
						
						if (dist <= closeEnoughDist)
						{
							/*System.out.println();
							System.out.println(": adding mutex between i_Seg = " + waypointsI.get(i-1) + " -> " + waypointsI.get(i));
							System.out.println(": adding mutex between j_seg = " + waypointsJ.get(j-1) + " -> " + waypointsJ.get(j));
							System.out.println(": dist = " + dist);*/
							
							// p should be in a mutual mutex pair
							if (ensureSameSet(waypointsI.get(i).mutexSet, waypointsJ.get(j).mutexSet, nextMutexId))
								++nextMutexId;
						}
					}
				}
			}
		}
		
		// step 3: collapse mutexes such that each point has at most one mutex id
		
		// if any point has two mutex ids, actually they are the same mutex, so should be collapsed
		ArrayList <TreeSet <Integer>> allMutexes = new ArrayList <TreeSet <Integer>>();
		
		for (ArrayList <InternalWaypoint> wps : dividedWaypoints)
		{
			for (InternalWaypoint wp : wps)
			{
				allMutexes.add(wp.mutexSet);
			}
		}
		
		while (true)
		{
			int mutexA = -1;
			int mutexB = -1;
			
			// find some point with more than one mutex
			for (TreeSet <Integer> mutex : allMutexes)
			{
				if (mutex.size() > 1)
				{
					// collapse
					mutexA = mutex.first();
					mutexB = mutex.last();
					
					break;
				}
			}
			
			// if not points have more than one mutex
			if (mutexA == -1)
				break;
			
			// replace all occurances of mutexB with mutexA
			for (TreeSet <Integer> mutex : allMutexes)
			{
				if (mutex.contains(mutexB))
				{
					mutex.remove(mutexB);
					mutex.add(mutexA);
				}
			}
		}
		
		// step 4: populate mutexId
		for (ArrayList <InternalWaypoint> wps : dividedWaypoints)
		{
			for (InternalWaypoint wp : wps)
			{
				if (wp.mutexSet.size() == 0)
					wp.wp.mutexId = DEFAULT_MUTEX_ID;
				else if (wp.mutexSet.size() == 1)
					wp.wp.mutexId = wp.mutexSet.first();
				else
					throw new RuntimeException("multiple mutex ids per point! (collapse stage failed)");
			}
		}
		
		// step 5: make sure numbers are consecutive
		ArrayList <InternalWaypoint> allWaypoints = new ArrayList <InternalWaypoint>();
		
		for (ArrayList <InternalWaypoint> wps : dividedWaypoints)
		{
			for (InternalWaypoint wp : wps)
			{
				allWaypoints.add(wp);
			}
		}		
		
		int max = 0;
		
		for (InternalWaypoint out : allWaypoints)
		{
			int id = out.wp.mutexId;
			
			if (id > max)
				max = id;
		}
		
		for (int id = 1; id < max; /* increment inside loop */ )
		{
			boolean found = false;
			
			for (InternalWaypoint out : allWaypoints)
			{
				int thisId = out.wp.mutexId;
				
				if (thisId == id)
				{
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				--max;
				
				// and decrease all ids above "id" by 1
				for (InternalWaypoint out : allWaypoints)
				{
					int thisId = out.wp.mutexId;
					
					if (thisId > id)
						out.wp.mutexId--;
				}
			}
			else 
				++id;
		}
		
		// step 6, convert from dividedWaypoints to output format
		
		
		ArrayList <LineOutput> rv = new ArrayList <LineOutput>();
		
		for (ArrayList <InternalWaypoint> wps : dividedWaypoints)
		{
			LineOutput out = new LineOutput();
			
			for (InternalWaypoint wp : wps)
			{
				out.waypoints.add(wp.wp);
			}
			
			rv.add(out);
		}
		
		//System.out.println(": compute ended");
		
		return rv;
	}


	public static void printWaypoints(ArrayList<ArrayList<InternalWaypoint>> dividedWaypoints)
	{
		for (ArrayList <InternalWaypoint> waypoints : dividedWaypoints)
		{
			for (InternalWaypoint i : waypoints)
			{
				System.out.println(i);
			}
			
			System.out.println();
		}
	}


	private static ArrayList<InternalWaypoint> inputToWaypoints(ArrayList<LineInput> lines)
	{
		boolean isFirst = true;
		ArrayList<InternalWaypoint> rv = new ArrayList<InternalWaypoint>();
		
		for (LineInput l : lines)
		{
			if (l.start.distance(l.end) < Geometry.SMALL) // skip lines of length zero
				continue;
			
			if (isFirst)
			{
				// add first
				rv.add(instance.new InternalWaypoint(l.start, l.color));
				
				isFirst = false;
			}
			
			// add second
			rv.add(instance.new InternalWaypoint(l.end, l.color));
		}
		
		return rv;
	}


	private static void addStartPoints(	ArrayList<ArrayList<InternalWaypoint>> dividedWaypoints,
			int robotRadius, Rectangle world, String ghostLineColor)
	{
		final int SPLIT = 200;
		
		// make the first waypoint use the ghost color since it will be from the start point
		for (ArrayList<InternalWaypoint> wps : dividedWaypoints)
		{
			if (wps.size() > 0)
			{	
				InternalWaypoint first = wps.get(0);
				first.wp.color = ghostLineColor;
			}
		}		
		
		for (int robotIndex = 0; robotIndex < dividedWaypoints.size(); ++ robotIndex)
		{
			// for each robot
			ArrayList<InternalWaypoint> thisRobotsWps = dividedWaypoints.get(robotIndex);
			
			Point start = thisRobotsWps.size() > 0 ? thisRobotsWps.get(0).wp.point : getDefaultRobotStart(robotIndex, world);
			int numWps = thisRobotsWps.size();
			Point end = thisRobotsWps.size() > 0 ? thisRobotsWps.get(numWps - 1).wp.point : getDefaultRobotStart(robotIndex, world);
			
			Point2D.Double closestStartPoint = null;
			double startDist = Double.MAX_VALUE;
			Point2D.Double closestEndPoint = null;
			double endDist = Double.MAX_VALUE;
			
			int stepX = world.width / SPLIT;
			int stepY = world.height / SPLIT;
			
			for (int x = world.x; x < world.width; x += stepX) for (int y = world.y; y < world.height; y += stepY)
			{			
				// try all these points in the world
				Point2D.Double p = new Point2D.Double(x,y);
				
				if (!insideOtherRobotsPath(p, dividedWaypoints, robotIndex, robotRadius))
				{
					// it is a possible start/end point
					
					double distToStart = p.distance(start);
					
					if (distToStart < startDist)
					{
						startDist = distToStart;
						closestStartPoint = p;
					}
					
					// now do end
					double distToEnd = p.distance(end);
					
					if (distToEnd < endDist)
					{
						endDist = distToEnd;
						closestEndPoint = p;
					}
				}
			}
			
			if (closestEndPoint == null)
				throw new RuntimeException("Could not find a robot start position which was outside of all mutex zones.");
			
			// otherwise, add the closest start and end points
			InternalWaypoint startWp = instance.new InternalWaypoint(closestStartPoint, ghostLineColor);
			startWp.wp.start = true;
			thisRobotsWps.add(0, startWp);
			
			InternalWaypoint endWp = instance.new InternalWaypoint(closestEndPoint, ghostLineColor);
			endWp.wp.end = true;
			thisRobotsWps.add(endWp);			
		}
	}


	private static Point getDefaultRobotStart(int r, Rectangle world)
	{
		r = r % 4;
		
		Point rv = new Point(world.x, world.y);
		
		if (r == 1)
			rv = new Point(world.x + world.width, world.y);
		else if (r == 2)
				rv = new Point(world.x + world.width, world.y + world.height);
		else if (r == 3)
			rv = new Point(world.x, world.y + world.height);
		
		return rv;
	}


	private static boolean insideOtherRobotsPath(Point2D.Double p, ArrayList<ArrayList<InternalWaypoint>> dividedWaypoints,
			int robotIndex, int robotRadius)
	{
		boolean rv = false;
		
		for (int robot = 0; robot < dividedWaypoints.size(); ++robot)
		{
			if (robot == robotIndex)
				continue; // don't check for intersections with itself
			
			ArrayList<InternalWaypoint> wps = dividedWaypoints.get(robot);
			
			for (int i = 1; i < wps.size(); ++i)
			{
				InternalWaypoint prev = wps.get(i-1);
				InternalWaypoint cur = wps.get(i);
				
				Line2D.Double l = waypointLine(prev,cur);
				
				if (l.ptSegDist(p) < 2 * robotRadius + 1)
				{
					rv = true;
				}
			}
		}
		
		return rv;
	}


	private static ArrayList<ArrayList<InternalWaypoint>> splitAmongRobots(
			ArrayList<InternalWaypoint> wps, int numRobots, int minTravelDistance)
	{
		ArrayList<ArrayList<InternalWaypoint>> rv = new ArrayList<ArrayList<InternalWaypoint>>();
		
		double totalDist = getTotalDist(wps);
		double distPerRobot = totalDist / numRobots;
		int numRobotsToAdd = 0;
		
		if (distPerRobot < minTravelDistance)
		{
			// some robots have to do nothing
			
			int possibleRobots = (int)Math.floor(totalDist / minTravelDistance); 
			
			if (possibleRobots == 0)
				possibleRobots = 1;
					
			numRobotsToAdd = numRobots - possibleRobots;
					
			numRobots = possibleRobots;
			distPerRobot = totalDist / numRobots;
		}
		
		// iterate through the waypoints
		ArrayList<InternalWaypoint> cur = new ArrayList<InternalWaypoint>();
		double curDist = 0;
		
		InternalWaypoint wp = null;
		int nextIndex = 0;
		
		while (true)
		{
			if (wp == null)
			{
				if (nextIndex >= wps.size())
					break;
				
				wp = wps.get(nextIndex++);
			}
			
			if (cur.size() == 0)
			{
				// always add the first waypoint
				cur.add(wp);
				wp = null; // increment to next waypoint
			}
			else
			{
				// check if we should add it based on the distance
				
				double wpDist = wayPointDistance(cur.get(cur.size() - 1), wp);
				
				if (curDist + wpDist < distPerRobot)
				{
					curDist += wpDist;
					cur.add(wp);
					
					wp = null; // increment to next waypoint
				}
				else
				{
					// add a new waypoint and move on to the next robot
					double mag = distPerRobot - curDist;
					InternalWaypoint prevWaypoint = cur.get(cur.size() - 1);
					Line2D.Double l = waypointLine(prevWaypoint, wp);
					
					double angle = Geometry.getAngle(l);
					
					Point2D.Double cutPoint = Geometry.projectPoint(new Point2D.Double(prevWaypoint.wp.point.x, prevWaypoint.wp.point.y), mag, angle);
					
					cur.add(instance.new InternalWaypoint(new Point((int)Math.round(cutPoint.x), (int)Math.round(cutPoint.y)), wp.wp.color));
					
					rv.add(cur);

					// prepare for the next one
					cur = new ArrayList<InternalWaypoint>();
					curDist = 0;
					cur.add(instance.new InternalWaypoint(new Point((int)Math.round(cutPoint.x), (int)Math.round(cutPoint.y)), wp.wp.color));
					
					
					// wp = wp; // do not change wp since we need to process it again in case distance is still too large
				}
			}
		}
		
		if (rv.size() < numRobots)
		{			
			// add last segment
			rv.add(cur);
		}
	
		// add robots without segments
		for (int i = 0; i < numRobotsToAdd;++i)
			rv.add(new ArrayList<InternalWaypoint>());
		
		return rv;
	}

	private static Line2D.Double waypointLine(InternalWaypoint a,	InternalWaypoint b)
	{
		return new Line2D.Double(a.wp.point, b.wp.point);
	}


	private static double wayPointDistance(InternalWaypoint a, InternalWaypoint b)
	{
		Line2D.Double l = new Line2D.Double(a.wp.point, b.wp.point);
		
		return Geometry.getLength(l);
	}

	private static double getTotalDist(ArrayList<InternalWaypoint> wps)
	{
		double rv = 0;
		
		for (int i = 1; i < wps.size(); ++i)
		{
			InternalWaypoint before = wps.get(i - 1);
			InternalWaypoint current = wps.get(i);
			
			Line2D.Double l = new Line2D.Double(before.wp.point, current.wp.point);
			
			rv += Geometry.getLength(l);
		}
		
		return rv;
	}


	private static void createWaypoints(ArrayList <ArrayList <InternalWaypoint> > dividedWaypoints, int waypointSpacing)
	{
		for (int robot = 0; robot < dividedWaypoints.size(); ++robot)
		{
			ArrayList <InternalWaypoint> robotWaypoints = dividedWaypoints.get(robot);
		
			for (int waypointIndex = 1; waypointIndex < robotWaypoints.size(); ++waypointIndex)
			{								
				Line2D.Double line = waypointLine(robotWaypoints.get(waypointIndex - 1), robotWaypoints.get(waypointIndex));
				String color = robotWaypoints.get(waypointIndex).wp.color;
				double len = Geometry.getLength(line);
				
				double ang;
				if (len < Geometry.SMALL)
					ang = 0.0;
				else
					ang = Geometry.getAngle(line);
				
				// step through the line
				int numSteps = (int)Math.ceil(len / waypointSpacing);
				double distPerStep = len / numSteps;
				
				Point2D.Double origin = new Point2D.Double(line.x1, line.y1);
				
				for (int step = 1; step < numSteps; ++step)
				{
					double mag = step * distPerStep; 
							
					// add point with magnitude mag at angle ang
					Point2D.Double p = Geometry.projectPoint(origin, mag, ang);
					Point pInt = new Point((int)Math.round(p.x), (int)(Math.round(p.y)));
					
					InternalWaypoint iwp = instance.new InternalWaypoint(pInt, color);
	
					robotWaypoints.add(waypointIndex++, iwp);
				}
			}
		}
	}

	private static void splitAlongIntersections(ArrayList <ArrayList <InternalWaypoint> > dividedWaypoints,	int robotRadius)
	{
		// find lines which intersect and split them to minimize waypoint area
		int distanceFromIntersection = 3 * robotRadius; // minimum we should consider is 2 * radius
		
		for (int robotI = 0; robotI < dividedWaypoints.size(); ++robotI)
		{
			ArrayList <InternalWaypoint> waypointsI = dividedWaypoints.get(robotI);
			
			for (int indexI = 1; indexI < waypointsI.size(); ++indexI)
			{		
				Point2D.Double intersection = null;
				InternalWaypoint prevI = waypointsI.get(indexI - 1);
				InternalWaypoint curI = waypointsI.get(indexI);
				
				Line2D.Double iLine = waypointLine(prevI, curI);
				
				for (int robotJ = 0; robotJ < dividedWaypoints.size(); ++robotJ)
				{
					if (robotJ == robotI)
						continue; // dont bother if the robot's intersecting with itself
					
					ArrayList <InternalWaypoint> waypointsJ = dividedWaypoints.get(robotJ);
					
					for (int indexJ = 1; indexJ < waypointsJ.size(); ++indexJ)
					{							
						Line2D.Double jLine = waypointLine(waypointsJ.get(indexJ - 1), waypointsJ.get(indexJ));
						
						Point2D.Double possibleIntersection = Geometry.segSegIntersection(iLine, jLine);
						
						if (possibleIntersection != null)
						{
							// make sure its not one of the endpoints
							Point2D.Double firstEndpoint = new Point2D.Double(iLine.x1, iLine.y1);
							Point2D.Double secondEndpoint = new Point2D.Double(iLine.x2, iLine.y2);
							
							if (possibleIntersection.distance(firstEndpoint) > 1 && possibleIntersection.distance(secondEndpoint) > 1)
							{
								// it's not one of the endpoints
								intersection = possibleIntersection;
								
								/*System.out.println("! found intersection at " + intersection);
								System.out.println("jLine = " + lineString(iLine));
								System.out.println("iLine = " + lineString(jLine));*/
								
								break;
							}
						}
					}
					
					if (intersection != null)
					{
						// split line into multiple segments and replace the current segment
						
						String color = waypointsI.get(indexI).wp.color;
						boolean isEnd = waypointsI.get(indexI).wp.end;
						// since we start at index 1, we never remove the start waypoint
						
						int nextIndexI = indexI - 1; // after we split, we will recheck the new segments for intersections
						
						waypointsI.remove(indexI); // remove current one
						// Q: do we ant to remove here?
						
						Point intersectionPoint = new Point((int)Math.round(intersection.x), (int)Math.round(intersection.y));
						double angle = Geometry.getAngle(iLine);
						
						// possibly four segments,  before, approach, <intersection>, depart, after
											
						////////////////////////// BEFORE INTERSECTION ///////////////////////////
						Line2D.Double beforeIntersection = new Line2D.Double(iLine.getP1(), intersection);
						
						if (Geometry.getLength(beforeIntersection) < distanceFromIntersection)
						{
							// add a single segment for before
							
							waypointsI.add(indexI++, instance.new InternalWaypoint(intersectionPoint, color));
						}
						else
						{
							// add a before and approach segment
							
							Point2D.Double approachPoint2D = Geometry.projectPoint(intersection, -distanceFromIntersection, angle);
							
							waypointsI.add(indexI++, instance.new InternalWaypoint(approachPoint2D, color));
							waypointsI.add(indexI++, instance.new InternalWaypoint(intersectionPoint, color));
						}
						
						
						//////////////////////// AFTER INTERSECTION ///////////////////////////
						Line2D.Double afterIntersection = new Line2D.Double(intersection, iLine.getP2());
						
						if (Geometry.getLength(afterIntersection) < distanceFromIntersection)
						{
							// add a single segment for after
							InternalWaypoint iwp =  instance.new InternalWaypoint(iLine.getP2(), color);
							iwp.wp.end = isEnd;
							
							waypointsI.add(indexI++,iwp);
						}
						else
						{
							// add a depart and after segment
							
							Point2D.Double departPoint2D = Geometry.projectPoint(intersection, distanceFromIntersection, angle);
							Point departPoint = new Point((int)Math.round(departPoint2D.x), (int)Math.round(departPoint2D.y));
							
							waypointsI.add(indexI++, instance.new InternalWaypoint(departPoint, color));
							
							InternalWaypoint iwp = instance.new InternalWaypoint(iLine.getP2(), color);
							iwp.wp.end = isEnd;
							waypointsI.add(indexI++,iwp);
						}
						
						indexI = nextIndexI; // after we split, we need to recheck the segments
						
						break;
					}
				}
			}
		}
	}


	public static String lineString(Line2D.Double l)
	{
		return "[Line2D:" + l.getP1() + " -> " + l.getP2() + "]";
	}
}



