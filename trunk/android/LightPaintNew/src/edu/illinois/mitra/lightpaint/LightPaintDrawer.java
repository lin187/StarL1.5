package edu.illinois.mitra.lightpaint;

import java.awt.Graphics2D;
import java.util.Map.Entry;

import edu.illinois.mitra.lightpaint.algorithm.LpAlgorithm;
import edu.illinois.mitra.lightpaint.geometry.ImageGraph;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;
import edu.illinois.mitra.lightpaintlib.activity.LightPaintActivity;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class LightPaintDrawer extends Drawer {

	@Override
	public void draw(LogicThread lt, Graphics2D g) {
//		LightPaintActivity instance = (LightPaintActivity)lt;
//		if(instance.iAmLeader)
//			drawAlgorithm(g, instance.alg);
	}
	
//	public void drawAlgorithm(Graphics2D g, LpAlgorithm alg) {
//		alg.drawing.draw(g, Color.LIGHT_GRAY, 12);
//		for(ImageGraph tube : reachTubes.values())
//			tube.draw(g, Color.red, 12, unsafeDrawRadius);
//
//		// Draw each robot position with a red unsafe boundary around it
//		g.setColor(Color.RED);
//		for(Entry<String, ImagePoint> robot : unsafeRobots.entrySet()) {
//			g.drawOval((int) (robot.getValue().getX() - unsafeDrawRadius), (int) (robot.getValue().getY() - unsafeDrawRadius), 2 * unsafeDrawRadius, 2 * unsafeDrawRadius);
//		}
//		painted.draw(g, Color.GREEN, 12);
//	}
	
}
