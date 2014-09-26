package edu.illinois.mitra.lightpaint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Map.Entry;

import edu.illinois.mitra.lightpaint.activity.LightPaintActivity;
import edu.illinois.mitra.lightpaint.algorithm.LpAlgorithm;
import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.geometry.ImageGraph;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class LightPaintDrawer extends Drawer {

	private static final int BULB_WIDTH = 25;

	private static boolean drawTubes = true;

	public LightPaintDrawer() {
	}

	public LightPaintDrawer(boolean drawTubes) {
		LightPaintDrawer.drawTubes = drawTubes;
	}

	@Override
	public void draw(LogicThread lt, Graphics2D g) {
		LightPaintActivity instance = (LightPaintActivity) lt;

		drawAlgorithm(g, instance.getAlgorithm());

		if(instance.getScreenColor() != 0) {
			ItemPosition pos = instance.getMyPosition();
			g.setColor(Color.BLACK);
			g.drawOval(pos.getX() - BULB_WIDTH, pos.getY() - BULB_WIDTH, 2 * BULB_WIDTH, 2 * BULB_WIDTH);
			g.setColor(new Color(instance.getScreenColor()));
			g.fillOval(pos.getX() - BULB_WIDTH, pos.getY() - BULB_WIDTH, 2 * BULB_WIDTH, 2 * BULB_WIDTH);
		}
	}

	private void drawAlgorithm(Graphics2D g, LpAlgorithm alg) {
		if(alg == null){
			return;
		}
		int unsafeDrawRadius = (int) alg.unsafeRadius / 2;

		drawImageGraph(alg.drawing, g, Color.LIGHT_GRAY, 12);

		if(drawTubes) {
			for(ImageGraph tube : alg.reachTubes.values())
				drawImageGraph(tube, g, Color.red, 12, unsafeDrawRadius);

			// Draw each robot position with a red unsafe boundary around it

			g.setColor(Color.RED);
			for(Entry<String, ImagePoint> robot : alg.unsafeRobots.entrySet()) {
				g.drawOval((int) (robot.getValue().getX() - unsafeDrawRadius), (int) (robot.getValue().getY() - unsafeDrawRadius), 2 * unsafeDrawRadius, 2 * unsafeDrawRadius);
			}
		}

		drawImageGraph(alg.painted, g, Color.GREEN, 12);
	}

	private static final Stroke LINE_STROKE = new BasicStroke(6);

	private void drawImageGraph(ImageGraph graph, Graphics2D g, Color color, int pointSize) {
		g.setColor(color);
		g.setStroke(LINE_STROKE);

		for(ImagePoint p : graph.getPoints())
			g.fillOval((int) p.getX() - (pointSize / 2), (int) p.getY() - (pointSize / 2), pointSize, pointSize);

		for(ImageEdge edge : graph.getGraph().edgeSet())
			g.drawLine((int) edge.getStart().getX(), (int) edge.getStart().getY(), (int) edge.getEnd().getX(), (int) edge.getEnd().getY());
	}

	private void drawImageGraph(ImageGraph graph, Graphics2D g, Color color, int pointSize, int unsafeRadius) {
		g.setColor(color);
		g.setStroke(LINE_STROKE);

		for(ImagePoint p : graph.getGraph().vertexSet()) {
			g.fillOval((int) p.getX() - (pointSize / 2), (int) p.getY() - (pointSize / 2), pointSize, pointSize);
			g.drawOval((int) (p.getX() - unsafeRadius), (int) (p.getY() - unsafeRadius), 2 * (int) unsafeRadius, 2 * (int) unsafeRadius);
		}

		for(ImageEdge edge : graph.getGraph().edgeSet()) {
			g.drawLine((int) edge.getStart().getX(), (int) edge.getStart().getY(), (int) edge.getEnd().getX(), (int) edge.getEnd().getY());
			// Draw lines offset by unsafeRadius to either side
			// Rotate the line by 90 degrees and scale to offset length
			ImagePoint rotated = new ImagePoint(-edge.getdY(), edge.getdX()).scale(unsafeRadius / edge.getLength());
			int xOffset = (int) rotated.getX();
			int yOffset = (int) rotated.getY();
			g.drawLine((int) edge.getStart().getX() - xOffset, (int) edge.getStart().getY() - yOffset, (int) edge.getEnd().getX() - xOffset, (int) edge.getEnd().getY() - yOffset);
			g.drawLine((int) edge.getStart().getX() + xOffset, (int) edge.getStart().getY() + yOffset, (int) edge.getEnd().getX() + xOffset, (int) edge.getEnd().getY() + yOffset);
		}
	}
}
