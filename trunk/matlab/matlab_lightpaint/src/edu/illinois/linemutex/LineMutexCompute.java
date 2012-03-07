package edu.illinois.linemutex;


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
		private static boolean ensureSameSet(TreeSet<Integer> a, TreeSet<Integer> b, TreeSet<Integer> c, int nextMutexId)
		{
			boolean rv = false;
			TreeSet<Integer> intersection = intersect(a, b);
			intersection = intersect(intersection, c);
			
			if (intersection.size() == 0)
			{
				// need to add a mutex to all three
				
				int min = Integer.MAX_VALUE;
				
				if (a.size() > 0)
					min = Math.min(min, a.first());
				
				if (b.size() > 0)
					min = Math.min(min, b.first());
				
				if (c.size() > 0)
					min = Math.min(min, c.first());
				
				if (min == Integer.MAX_VALUE)
				{
					min = nextMutexId;
					rv = true;
				}
				
				// add min to all 3
				a.add(min);
				b.add(min);
				c.add(min);
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

	
	// get the segments around index i in list p
	private static ArrayList <MutexSegment> getSegmentsAroundWaypoint(ArrayList<Point2D.Double> allPoints, ArrayList<TreeSet <Integer>> allMutexes, int wayPointIndex)
	{
		ArrayList <MutexSegment> rv = new ArrayList <MutexSegment>();
		
		Point2D.Double p = allPoints.get(wayPointIndex);
		TreeSet <Integer> mutexP = allMutexes.get(wayPointIndex);
		
		// if there is a segment before i
		if (wayPointIndex != 0)
		{
			Point2D.Double before = allPoints.get(wayPointIndex-1);
			TreeSet <Integer> mutexBefore = allMutexes.get(wayPointIndex-1);
			
			rv.add(instance.new MutexSegment(new Line2D.Double(before, p), mutexBefore, mutexP));
		}
		
		// if there is a segment after i
		if (wayPointIndex != allPoints.size() - 1)
		{
			Point2D.Double after = allPoints.get(wayPointIndex+1);
			TreeSet <Integer> mutexAfter = allMutexes.get(wayPointIndex+1);
			
			rv.add(instance.new MutexSegment(new Line2D.Double(p, after), mutexP, mutexAfter));
		}
		
		return rv;
	}
	
	
	
	class LineInternalData
	{
		ArrayList <DoublePoint> points = new ArrayList <DoublePoint>();
		ArrayList <String> colors = new ArrayList <String>();
		ArrayList <TreeSet <Integer> > mutexSet = new ArrayList <TreeSet <Integer> >();
		ArrayList <Integer> mutexId = new ArrayList <Integer>();
	};
		

		public static ArrayList <LineOutputData> compute(ArrayList <LineInputData> in, int spacing, int robotRadius)
		{
			ArrayList <LineInternalData> rv = new ArrayList <LineInternalData>();
			int DEFAULT_MUTEX_ID = 0;
			
			// step 1: divide all lines into a sequence of points of at most length 'spacing'
			for (LineInputData l : in)
			{
				LineInternalData out = instance.new LineInternalData();
				
				Line2D.Double line = new Line2D.Double(l.start, l.end);
				double len = Geometry.getLength(line);
				double ang = Geometry.getAngle(line);
				
				// step through the line
				int numSteps = (int)Math.ceil(len / spacing);
				double distPerStep = len / numSteps;
				
				Point2D.Double orgin = l.start;
				
				for (int step = 0; step <= numSteps; ++step)
				{
					double mag = step * distPerStep; 
							
					// add point with magnitude mag at angle ang
					Point2D.Double p = Geometry.projectPoint(orgin, mag, ang);
					
					out.points.add(new DoublePoint(p.x, p.y));
					out.colors.add(l.color);
					out.mutexSet.add(new TreeSet <Integer>());
					
				}
				
				rv.add(out);
			}
			
			// step 1.5: combine all points and mutexes into a single list
			ArrayList <Point2D.Double> allPoints = new ArrayList <Point2D.Double>(); 
			ArrayList <TreeSet <Integer>> allMutexes = new ArrayList <TreeSet <Integer>>();
			
			boolean isFirstPoint = true;
			
			for (LineInternalData out : rv)
			{
				if (isFirstPoint)
				{
					allPoints.add(out.points.get(0));
					allMutexes.add(out.mutexSet.get(0));
					isFirstPoint = false;
				}
				
				// start these at index 1 since the last point of the previous line was already added
				for (int i = 1; i < out.points.size(); ++i)
				{
					allPoints.add(out.points.get(i));
					allMutexes.add(out.mutexSet.get(i));
				}
			}
			
			// step 1.75 make sure all points are unique
			while (true)
			{
				boolean allUnique = true;
				
				for (int i = 0; i < allPoints.size(); ++i)
				{
					for (int j = i+1; j < allPoints.size(); ++j)
					{
						if (allPoints.get(i).equals(allPoints.get(j)))
						{
							// move j slightly
							allPoints.get(j).x += Math.random() / 100000.0;
							
							// and repeat in case we moved it into another one
							allUnique = false;
						}
					}
				}
				
				if (allUnique)
					break;
			}
			
			// step 2: construct mutexs from points that are "close enough" together
			double closeEnoughDist = 2 * robotRadius;
			int nextMutexId = 1;
			
			for (int i = 0; i < allPoints.size(); ++i)
			{
				TreeSet <Integer> p_mutex = allMutexes.get(i);
				//Point2D.Double p = allPoints.get(i);
				
				ArrayList <MutexSegment> segs = getSegmentsAroundWaypoint(allPoints, allMutexes, i);
			
				for (MutexSegment seg : segs)
				{
					// check if seg intersects with any non-neighbor lines
					// if it does, update the mutex
					
					for (int x = 0; x < allPoints.size(); ++x)
					{
						ArrayList <MutexSegment> it_segs = getSegmentsAroundWaypoint(allPoints, allMutexes, x);
						
						for (MutexSegment it_seg : it_segs)
						{
							if (!it_seg.seg.getP1().equals(seg.seg.getP1()) // not the same segment 
									&& !it_seg.seg.getP1().equals(seg.seg.getP2()) // not the segment after 'seg'
									&& !it_seg.seg.getP2().equals(seg.seg.getP1()) // not the segment before 'seg'
									)
							{
								// check if it_seg is close enough to seg
								
								double dist = Geometry.segSegDist(seg.seg, it_seg.seg);
								
								if (dist <= closeEnoughDist)
								{
									/*System.out.println();
									System.out.println(": adding mutex, p = " + allPoints.get(i));
									System.out.println(": adding mutex between seg = " + seg.seg.getP1() + " -> " + seg.seg.getP2());
									System.out.println(": adding mutex between it_seg = " + it_seg.seg.getP1() + " -> " + it_seg.seg.getP2());
									System.out.println(": dist = " + dist);*/
									
									// p should be in a mutual mutex pair with both it_seg.p1 and it_seg.p2
									if (ensureSameSet(p_mutex, it_seg.mutexP1, it_seg.mutexP2, nextMutexId))
										++nextMutexId;
								}
							}
						}
						
					}
				}
			}
			
			// step 3: collapse mutexes such that each point has at most one mutex id
			
			// if any point has two mutex ids, actually they are the same mutex, so should be collapsed
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
			for (LineInternalData out : rv)
			{
				for (int i = 0; i < out.points.size(); ++i)
				{
					if (out.mutexSet.get(i).size() == 0)
						out.mutexId.add(DEFAULT_MUTEX_ID);
					else if (out.mutexSet.get(i).size() == 1)
						out.mutexId.add(out.mutexSet.get(i).first());
					else
						throw new RuntimeException("multiple mutex ids per point! (collapse stage failed)");
				}
			}
			
			// step 5: make sure numbers are consecutive
			int max = 0;
			
			for (LineInternalData out : rv)
			{
				for (int i = 0; i < out.points.size(); ++i)
				{
					int id = out.mutexId.get(i);
					
					if (id > max)
						max = id;
				}
			}
			
			for (int id = 1; id < max; /* increment inside loop */ )
			{
				boolean found = false;
				
				for (LineInternalData out : rv)
				{
					for (int i = 0; i < out.points.size(); ++i)
					{
						int thisId = out.mutexId.get(i);
						
						if (thisId == id)
						{
							found = true;
							break;
						}
					}
				}
				
				if (!found)
				{
					--max;
					
					// and decrease all ids above "id" by 1
					for (LineInternalData out : rv)
					{
						for (int i = 0; i < out.points.size(); ++i)
						{
							int thisId = out.mutexId.get(i);
							
							if (thisId > id)
								out.mutexId.set(i, thisId - 1);
						}
					}
				}
				else 
					++id;
			}
			
			// step 6, convert from LineInternalData to LineOutputData
			ArrayList <LineOutputData> finalRv = new ArrayList <LineOutputData>();
			
			for (LineInternalData i : rv)
			{
				LineOutputData out = new LineOutputData();
				
				out.colors = i.colors;
				out.mutexId = i.mutexId;
				out.points = i.points;
				
				finalRv.add(out);
			}
			
			return finalRv;
		}
}
