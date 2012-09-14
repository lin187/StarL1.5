package edu.illinois.mitra.lightpaint.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.EdgeFactory;
import org.jgrapht.Graph;
import org.jgrapht.graph.Pseudograph;

public class ImageGraph {
	public static final EdgeFactory<ImagePoint, ImageEdge> IMAGE_EDGEFACTORY = new EdgeFactory<ImagePoint, ImageEdge>() {
		@Override
		public ImageEdge createEdge(ImagePoint arg0, ImagePoint arg1) {
			return new ImageEdge(arg0, arg1);
		}
	};
	private Graph<ImagePoint, ImageEdge> graph = new Pseudograph<ImagePoint, ImageEdge>(IMAGE_EDGEFACTORY);

	public ImageGraph() {
	}

	public ImageGraph(ImageGraph other) {
		for(ImageEdge e : other.graph.edgeSet())
			addEdge(e);
	}

	public Set<ImagePoint> getPoints() {
		return graph.vertexSet();
	}

	public Graph<ImagePoint, ImageEdge> getGraph() {
		return graph;
	}

	public List<ImagePoint> getPointsInDistanceOrder(final ImagePoint start) {
		List<ImagePoint> retval = new ArrayList<ImagePoint>(graph.vertexSet());

		Collections.sort(retval, new Comparator<ImagePoint>() {
			@Override
			public int compare(ImagePoint o1, ImagePoint o2) {
				double o1d = o1.distanceTo(start);
				double o2d = o2.distanceTo(start);
				if(o1d > o2d)
					return 1;
				else if(o1d < o2d)
					return -1;
				return 0;
			}
		});
		return retval;
	}

	public ImagePoint getNearestPoint(ImagePoint start) {
		ImagePoint retval = null;
		double dist = Double.MAX_VALUE;
		for(ImagePoint ip : graph.vertexSet()) {
			if(ip.distanceTo(start) < dist) {
				dist = ip.distanceTo(start);
				retval = ip;
			}
		}
		return retval;
	}

	public <T extends Collection<ImageEdge>> T removeCollidingEdges(T collection, double radius) {
		Set<ImageEdge> toRemove = new HashSet<ImageEdge>();
		for(ImageEdge ie : collection)
			if(isColliding(ie, radius))
				toRemove.add(ie);

		collection.removeAll(toRemove);
		return collection;
	}

	public <T extends Collection<ImagePoint>> T removeCollidingPoints(T collection, double radius) {
		Set<ImagePoint> toRemove = new HashSet<ImagePoint>();
		for(ImagePoint ip : collection)
			if(isColliding(ip, radius))
				toRemove.add(ip);

		collection.removeAll(toRemove);
		return collection;
	}

	public void addAll(ImageGraph other) {
		for(ImageEdge edge : other.graph.edgeSet())
			addEdge(edge);

		for(ImagePoint point : other.graph.vertexSet())
			addPoint(point);
	}

	public void addEdge(ImagePoint start, ImagePoint end) {
		graph.addVertex(start);
		graph.addVertex(end);
		graph.addEdge(start, end);
	}

	public void addEdge(ImageEdge edge) {
		if(!graph.containsEdge(edge))
			addEdge(edge.getStart(), edge.getEnd());
	}

	public void removeEdge(ImagePoint start, ImagePoint end) {
		graph.removeEdge(start, end);

		// If the vertices no longer have any edges, remove vertices too
		if(graph.containsVertex(start))
			if(graph.edgesOf(start).isEmpty())
				graph.removeVertex(start);
		if(graph.containsVertex(end))
			if(graph.edgesOf(end).isEmpty())
				graph.removeVertex(end);
	}

	public boolean hasEdge(ImageEdge edge) {
		return graph.containsEdge(edge);
	}

	public boolean hasEdge(ImagePoint start, ImagePoint end) {
		return graph.containsEdge(start, end);
	}

	public void removeEdge(ImageEdge edge) {
		removeEdge(edge.getStart(), edge.getEnd());
	}

	public void addPoint(ImagePoint point) {
		graph.addVertex(point);
	}

	public void removePoint(ImagePoint point) {
		if(graph.edgesOf(point).isEmpty())
			graph.removeVertex(point);
		else
			System.err.println("Tried to remove a point with edges attached!");
	}

	public Set<ImageEdge> getEdgesOf(ImagePoint point) {
		if(graph.containsVertex(point))
			return graph.edgesOf(point);
		return null;
	}

	public boolean isColliding(ImageEdge edge, double radius) {
		for(ImageEdge e : graph.edgeSet())
			if(e.distanceTo(edge) <= radius)
				return true;

		for(ImagePoint p : graph.vertexSet())
			if(edge.distanceTo(p) <= radius)
				return true;
		return false;
	}

	public boolean isColliding(ImagePoint point, double radius) {
		for(ImageEdge e : graph.edgeSet())
			if(e.distanceTo(point) <= radius)
				return true;

		for(ImagePoint p : graph.vertexSet())
			if(p.distanceTo(point) <= radius)
				return true;
		return false;
	}

	public boolean isColliding(ImageGraph other, double radius) {
		for(ImagePoint p : other.getPoints())
			if(isColliding(p, radius))
				return true;

		for(ImageEdge e : other.getGraph().edgeSet())
			if(isColliding(e, radius))
				return true;
		return false;
	}

	public double minDistanceTo(ImagePoint point) {
		double distance = Double.MAX_VALUE;
		for(ImagePoint p : graph.vertexSet())
			distance = Math.min(distance, p.distanceTo(point));
		for(ImageEdge e : graph.edgeSet())
			distance = Math.min(distance, e.distanceTo(point));
		return distance;
	}

	public boolean isEmpty() {
		return graph.edgeSet().isEmpty();
	}

//	private static final Stroke LINE_STROKE = new BasicStroke(6);
//
//	public void draw(Graphics2D g, Color color, int pointSize, int unsafeRadius) {
//		g.setColor(color);
//		g.setStroke(LINE_STROKE);
//
//		for(ImagePoint p : graph.vertexSet()) {
//			g.fillOval((int) p.getX() - (pointSize / 2), (int) p.getY() - (pointSize / 2), pointSize, pointSize);
//			g.drawOval((int) (p.getX() - unsafeRadius), (int) (p.getY() - unsafeRadius), 2 * (int) unsafeRadius, 2 * (int) unsafeRadius);
//		}
//
//		for(ImageEdge edge : graph.edgeSet()) {
//			g.drawLine((int) edge.getStart().getX(), (int) edge.getStart().getY(), (int) edge.getEnd().getX(), (int) edge.getEnd().getY());
//			// Draw lines offset by unsafeRadius to either side
//			// Rotate the line by 90 degrees and scale to offset length
//			ImagePoint rotated = new ImagePoint(-edge.getdY(), edge.getdX()).scale(unsafeRadius / edge.getLength());
//			int xOffset = (int) rotated.getX();
//			int yOffset = (int) rotated.getY();
//			g.drawLine((int) edge.getStart().getX() - xOffset, (int) edge.getStart().getY() - yOffset, (int) edge.getEnd().getX() - xOffset, (int) edge.getEnd().getY() - yOffset);
//			g.drawLine((int) edge.getStart().getX() + xOffset, (int) edge.getStart().getY() + yOffset, (int) edge.getEnd().getX() + xOffset, (int) edge.getEnd().getY() + yOffset);
//		}
//	}
//
//	private Color drawColor = Color.BLACK;
//
//	public void setDrawColor(Color color) {
//		drawColor = color;
//	}
//
//	public void draw(Graphics2D g) {
//		draw(g, drawColor, 25);
//	}
//
//	public void draw(Graphics2D g, Color color, int pointSize) {
//		g.setColor(color);
//		g.setStroke(LINE_STROKE);
//
//		for(ImagePoint p : graph.vertexSet())
//			g.fillOval((int) p.getX() - (pointSize / 2), (int) p.getY() - (pointSize / 2), pointSize, pointSize);
//
//		for(ImageEdge edge : graph.edgeSet())
//			g.drawLine((int) edge.getStart().getX(), (int) edge.getStart().getY(), (int) edge.getEnd().getX(), (int) edge.getEnd().getY());
//	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((graph == null) ? 0 : graph.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		ImageGraph other = (ImageGraph) obj;
		if(graph == null) {
			if(other.graph != null)
				return false;
		} else if(!graphEquals(graph, other.graph) || !graphEquals(graph, other.graph))
			return false;
		return true;
	}

	private static boolean graphEquals(Graph<ImagePoint, ImageEdge> a, Graph<ImagePoint, ImageEdge> b) {
		// Make sure Graph b contains all of the vertices and edges of graph a
		if(a.edgeSet().size() != b.edgeSet().size())
			return false;

		for(ImageEdge edge : a.edgeSet())
			if(!b.containsEdge(edge) && !b.containsEdge(edge.reversed()))
				return false;

		for(ImagePoint point : a.vertexSet())
			if(!b.containsVertex(point))
				return false;

		return true;
	}
}
