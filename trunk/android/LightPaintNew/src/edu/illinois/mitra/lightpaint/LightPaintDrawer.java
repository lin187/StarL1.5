package edu.illinois.mitra.lightpaint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map.Entry;

import edu.illinois.mitra.lightpaint.geometry.ImageGraph;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class LightPaintDrawer extends Drawer {

	@Override
	public void draw(LogicThread lt, Graphics2D g) {
		MainActivity instance = (MainActivity)lt;
		if(instance.iAmLeader)
			instance.alg.draw(g);
	}
	

}
