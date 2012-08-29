package edu.illinois.mitra.lightpaint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Random;

import org.jgrapht.alg.DijkstraShortestPath;

import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.geometry.ImageGraph;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;

public class PrmPathFinder {
	private static final int POINTS_TO_GENERATE = 100;
	private int width;
	private int height;
	private double unsafeRadius;

	public PrmPathFinder(int width, int height, double unsafeRadius) {
		this.width = width;
		this.height = height;
		this.unsafeRadius = unsafeRadius;
	}

	private static final Random rand = new Random(System.currentTimeMillis());
	private static final int MAX_CONNECTED_NEIGHBORS = 10;

	private ImageGraph points;

	public List<ImageEdge> findPath(ImageGraph unsafe, ImagePoint start, ImagePoint end) {
		synchronized(this) {
			// Step 1: Generate random points in the world
			points = new ImageGraph();
			for(int i = 0; i < POINTS_TO_GENERATE; i++) {
				ImagePoint toAdd = new ImagePoint(rand.nextInt(width), rand.nextInt(height));
				if(!unsafe.isColliding(toAdd, unsafeRadius))
					points.addPoint(toAdd);
			}

			// Return if no valid points were generated
			if(points.getPoints().isEmpty())
				return null;

			// Step 2: Connect points with their nearest neighbors
			// that are within CONNECT_RADIUS
			for(ImagePoint point : points.getPoints())
				points = connectNeighbors(points, point, unsafe);

			// It nothing was connected, give up again
			if(points.isEmpty())
				return null;

			// Connect the start and end to their nearest neighbors
			points = connectNeighbors(points, start, unsafe);
			points = connectNeighbors(points, end, unsafe);

			// Step 3: Determine if a path exists from start to end
			List<ImageEdge> path;

			try {
				path = DijkstraShortestPath.findPathBetween(points.getGraph(), start, end);
			} catch (IllegalArgumentException e) {
				// Failed to find path
				System.err.println("PRM failed to find a path!");
				return null;
			}

			if(path != null && path.size() > 0)
				return straightenPath(path, unsafe, unsafeRadius);
			else
				return null;
		}
	}

	public void draw(Graphics2D g) {
		synchronized(this) {
			if(points != null)
				points.draw(g, Color.BLUE, 12);
		}
	}

	private ImageGraph connectNeighbors(ImageGraph points, ImagePoint point, ImageGraph unsafe) {
		List<ImagePoint> neighbors = points.getPointsInDistanceOrder(point);

		for(int i = 0; i < Math.min(neighbors.size(), MAX_CONNECTED_NEIGHBORS); i++) {
			ImageEdge toAdd = new ImageEdge(point, neighbors.get(i));
			if(unsafe.isColliding(toAdd, unsafeRadius))
				continue;
			else {
				points.addEdge(toAdd);
			}
		}
		return points;
	}

	private List<ImageEdge> straightenPath(List<ImageEdge> pathEdges, ImageGraph unsafe, double unsafeRadius) {
		List<ImagePoint> path = LpAlgorithm.edgesToPoints(pathEdges);
		int i = 2;
		while(i < path.size()) {
			if(!unsafe.isColliding(new ImageEdge(path.get(i - 2), path.get(i)), unsafeRadius)) {
				path.remove(i - 1);
			} else {
				i++;
			}
		}

		return LpAlgorithm.pointsToEdges(path);
	}
}
