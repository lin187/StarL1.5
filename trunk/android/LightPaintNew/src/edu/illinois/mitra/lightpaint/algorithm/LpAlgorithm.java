package edu.illinois.mitra.lightpaint.algorithm;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.geometry.ImageGraph;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;
import edu.illinois.mitra.lightpaint.utility.Utility;
import edu.illinois.mitra.starl.objects.ItemPosition;

/**
 * @author Adam Zimmerman
 */
public class LpAlgorithm {
	private final double unsafeRadius;
	private final double pointSnapRadius;
	private final double maxDrawPathLength;

	private final ImageGraph drawing = new ImageGraph();
	private ImageGraph unpainted;
	private final ImageGraph painted = new ImageGraph();
	private final ImageGraph unsafe = new ImageGraph();
	private final Map<String, ImagePoint> unsafeRobots = new HashMap<String, ImagePoint>();

	private final PrmPathFinder prm;

	public LpAlgorithm(Set<ImageEdge> edges, double pointSnapRadius, double maxDrawPathLength, double unsafeRadius) {
		this.unsafeRadius = unsafeRadius;
		this.pointSnapRadius = pointSnapRadius;
		this.maxDrawPathLength = maxDrawPathLength;

		prm = new PrmPathFinder(2000, 2000, unsafeRadius);
		initialize(edges);
	}

	private void initialize(Set<ImageEdge> edges) {
		for(ImageEdge edge : edges) {
			// Don't add edges that are shorter than the point snap radius
			if(edge.getLength() < pointSnapRadius)
				continue;

			ImagePoint start = edge.getStart();
			ImagePoint end = edge.getEnd();

			// If the drawing already has points within snap radius of start and
			// end, change the endpoints to use those preexisting points
			if(drawing.isColliding(start, pointSnapRadius))
				start = drawing.getNearestPoint(start);

			if(drawing.isColliding(end, pointSnapRadius))
				end = drawing.getNearestPoint(end);

			drawing.addEdge(start, end);
		}
		// Unpainted is a clone of the base image at this point
		unpainted = new ImageGraph(drawing);
	}

	private static final List<ItemPosition> EMPTY_LIST = new ArrayList<ItemPosition>(0);

	public List<ItemPosition> assignSegment(String currentRobot, ItemPosition robotPosition) {
		List<ImagePoint> points = assignSegment(currentRobot, new ImagePoint(robotPosition.x, robotPosition.y));
		if(points == null)
			return EMPTY_LIST;
		List<ItemPosition> positions = new ArrayList<ItemPosition>(points.size());
		for(ImagePoint point : points) {
			positions.add(new ItemPosition(point.toString(), (int) point.getX(), (int) point.getY(), 0));
		}
		return positions;
	}

	public List<ImagePoint> assignSegment(String currentRobot, ImagePoint robotPosition) {
		if(isDone())
			return null;

		setRobotPosition(currentRobot, robotPosition);

		// First make sure they're not in an unsafe region already
		// TODO figure out how to get them out of an unsafe situation
		ImageGraph unsafeForCurrent = new ImageGraph(unsafe);
		unsafeForCurrent.addAll(unsafeRobotGraph(currentRobot));

		if(unsafeForCurrent.isColliding(robotPosition, unsafeRadius)) {
			System.err.println(currentRobot + " is in an unsafe starting position");
			return null;
		}

		// Get the closest vertices, removing any that are unsafe
		List<ImagePoint> availableStartPoints = unsafeForCurrent.removeCollidingPoints(unpainted.getPointsInDistanceOrder(robotPosition), unsafeRadius);

		// Score each available starting point
		// Score = (distance of path available from start point) / (distance to
		// start)
		// Remove any starting points with zero path available
		double maxScore = Double.NEGATIVE_INFINITY;
		List<ImageEdge> bestPath = null;
		for(int i = 0; i < availableStartPoints.size(); i++) {
			ImagePoint start = availableStartPoints.get(i);

			List<ImageEdge> pathToStart = new ArrayList<ImageEdge>();

			// If no straight path is available, use PRM
			// If no PRM path is available, give up
			if(unsafeForCurrent.isColliding(new ImageEdge(start, robotPosition), unsafeRadius)) {
				if((pathToStart = prm.findPath(unsafeForCurrent, robotPosition, start)) == null) {
					continue;
				}
			} else {
				pathToStart.add(new ImageEdge(robotPosition, start));
			}

			// Find distance to start
			double distToStart = Utility.cap(getPathLength(pathToStart), 1d, Double.POSITIVE_INFINITY);

			// Find distance of achievable path
			List<ImageEdge> path = getPathFromStart(start, new ImageGraph(unpainted), unsafeForCurrent, maxDrawPathLength, true);
			double pathLength = getPathLength(path);

			// Discard if there are no viable paths from this start point
			if(pathLength == 0)
				continue;

			// Compute score
			double score = pathLength / distToStart;
			
			if(score > maxScore) {
				maxScore = score;
				List<ImageEdge> fullPath = new ArrayList<ImageEdge>();
				fullPath.addAll(pathToStart);
				fullPath.addAll(path);
				bestPath = fullPath;
			}
		}

		// If no acceptable starts were found, return null
		if(bestPath == null)
			return null;
		
		// Mark the best path as unsafe, remove it from unpainted
		for(ImageEdge edge : bestPath) {
			unsafe.addEdge(edge);
			if(unpainted.hasEdge(edge))
				unpainted.removeEdge(edge);
		}

		return edgesToPoints(bestPath);
	}

	public boolean isDone() {
		return unsafe.isEmpty() && drawing.equals(painted);
	}

	public void markSafeDrawn(String robotName, ImagePoint start, ImagePoint end) {
		unsafe.removeEdge(start, end);
		if(drawing.hasEdge(start, end))
			painted.addEdge(start, end);

		setRobotPosition(robotName, end);
	}

	private List<ImageEdge> getPathFromStart(ImagePoint start, ImageGraph unpainted, ImageGraph unsafe, double maxLength, boolean forceAcceptPath) {
		List<ImageEdge> path = new ArrayList<ImageEdge>();

		ImagePoint entry = start;
		SortedSet<ImageEdge> possibleEdges = getDistanceSortedEdgeSet(unpainted.getEdgesOf(entry));

		double pathLength = 0;

		// If the shortest edge available is longer than the maximum length,
		// take it only if this is the first iteration
		if(possibleEdges.last().getLength() > maxLength && forceAcceptPath) {
			ImageEdge edgeToAdd = possibleEdges.last();
			path.add(edgeToAdd);
			unpainted.removeEdge(edgeToAdd);
			pathLength += edgeToAdd.getLength();
		}

		// While the length parameter hasn't been exceeded and there are more
		// possible edges, continue
		while(pathLength < maxLength && !possibleEdges.isEmpty()) {
			// Add the longest possible edge that's safe to the path
			ImageEdge longest = possibleEdges.first();
			if(!unsafe.isColliding(longest, unsafeRadius) && longest.getLength() < maxLength) {
				path.add(longest);
				pathLength += longest.getLength();

				// Determine which point to check next for edges
				entry = longest.getOtherEnd(entry);
				possibleEdges = getDistanceSortedEdgeSet(unpainted.getEdgesOf(entry));

				// Remove the edge we just entered on to prevent infinite loops
				// back and forth
				possibleEdges.remove(longest);
				unpainted.removeEdge(longest);
			} else
				possibleEdges.remove(longest);
		}

		// If the path is less than the maximum length and other drawable
		// segments exists within the total distance remaining to travel,
		// connect to the closest drawable segment and draw as much as possible
		if(pathLength < maxLength) {
			double lengthRemaining = maxLength - pathLength;
			List<ImagePoint> closestVertices = unpainted.getPointsInDistanceOrder(entry);

			for(ImagePoint point : closestVertices) {
				// If we can connect to this next point safely, explore that
				// point for paths
				ImageEdge pathToProspectiveStart = new ImageEdge(entry, point);
				if(pathToProspectiveStart.getLength() > lengthRemaining)
					return path;
				if(!unsafe.isColliding(pathToProspectiveStart, unsafeRadius)) {
					lengthRemaining -= pathToProspectiveStart.getLength();
					List<ImageEdge> continuation = getPathFromStart(point, unpainted, unsafe, pathLength, false);
					if(continuation.size() > 0) {
						path.add(pathToProspectiveStart);
						path.addAll(continuation);
					}
					return path;
				}
			}
		}

		return path;
	}

	/**
	 * Create a set of image edges sorted by distance
	 * First = longest, last = shortest
	 * @param set a collection of image edges to be sorted
	 * @return the sorted input set 
	 */
	private static SortedSet<ImageEdge> getDistanceSortedEdgeSet(Collection<ImageEdge> set) {
		SortedSet<ImageEdge> edges = new TreeSet<ImageEdge>(ImageEdge.lengthComparator()).descendingSet();
		edges.addAll(set);
		return edges;
	}

	public static List<ImagePoint> edgesToPoints(List<ImageEdge> path) {
		List<ImagePoint> pathPoints = new ArrayList<ImagePoint>();
		int idx = 0;
		for(ImageEdge edge : path) {
			if(idx == 0) {
				pathPoints.add(edge.getStart());
				pathPoints.add(edge.getEnd());
				idx++;
			} else if(pathPoints.get(idx - 1).equals(edge.getStart()) || pathPoints.get(idx - 2).equals(edge.getStart())) {
				pathPoints.add(edge.getEnd());
			} else if(pathPoints.get(idx - 1).equals(edge.getEnd()) || pathPoints.get(idx - 2).equals(edge.getEnd())) {
				pathPoints.add(edge.getStart());
			} else {
				throw new RuntimeException("Disjoint path: " + pathPoints + " " + edge);
			}
			idx++;
		}
		return pathPoints;
	}

	public static List<ImageEdge> pointsToEdges(List<ImagePoint> points) {
		List<ImageEdge> retval = new ArrayList<ImageEdge>();

		for(int i = 1; i < points.size(); i++)
			retval.add(new ImageEdge(points.get(i - 1), points.get(i)));

		return retval;
	}

	private double getPathLength(List<ImageEdge> path) {
		double retval = 0;
		for(ImageEdge edge : path)
			retval += edge.getLength();
		return retval;
	}

	/**
	 * Inform the algorithm of the position of a robot. The names and locations
	 * of all participating robots should be provided initially using this
	 * method. If a robot is not requesting segments to draw or informing the
	 * algorithm of its progress, the algorithm will have no knowledge of its
	 * location and could possibly plan paths through it.
	 * 
	 * @param name
	 *            the name of the robot
	 * @param position
	 *            the current position of the robots
	 */
	public void setRobotPosition(String name, ImagePoint position) {
		unsafeRobots.put(name, position);
	}

	/**
	 * Generate a graph of only current unsafe robot positions. Won't include
	 * the position of the provided robot.
	 * 
	 * @param currentRobot
	 * @return unsafe regions populated by robots
	 */
	private ImageGraph unsafeRobotGraph(String currentRobot) {
		ImageGraph retval = new ImageGraph();
		for(Entry<String, ImagePoint> robot : unsafeRobots.entrySet()) {
			if(!robot.getKey().equals(currentRobot))
				retval.addPoint(robot.getValue());
		}
		return retval;
	}

	public void draw(Graphics2D g) {
		drawing.draw(g, Color.LIGHT_GRAY, 12);
		unsafe.draw(g, Color.RED, 12, (int) unsafeRadius);
		painted.draw(g, Color.GREEN, 12);

		// Draw each robot position with a red unsafe boundary around it
		unsafeRobotGraph(null).draw(g, Color.PINK, 20);
		g.setColor(Color.RED);
		for(Entry<String, ImagePoint> robot : unsafeRobots.entrySet()) {
			g.drawOval((int) (robot.getValue().getX() - unsafeRadius), (int) (robot.getValue().getY() - unsafeRadius), 2 * (int) unsafeRadius, 2 * (int) unsafeRadius);
		}
	}
}
