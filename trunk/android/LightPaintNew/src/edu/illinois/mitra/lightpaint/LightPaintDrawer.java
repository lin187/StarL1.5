package edu.illinois.mitra.lightpaint;

import java.awt.Graphics2D;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class LightPaintDrawer extends Drawer {

	@Override
	public void draw(LogicThread lt, Graphics2D g) {
		LightPaintActivity instance = (LightPaintActivity)lt;
		if(instance.iAmLeader)
			instance.alg.draw(g);
	}
	
}
