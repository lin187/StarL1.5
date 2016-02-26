package edu.illinois.mitra.demo.formation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class FormationDrawer extends Drawer {

	private Stroke stroke = new BasicStroke(8);
	private Color selectColor = new Color(0,0,255,100);
	
	@Override
	public void draw(LogicThread lt, Graphics2D g) {
		FormationApp app = (FormationApp) lt;

		g.setColor(Color.RED);
		for(ItemPosition dest : app.destinations.values()) {
			g.fillRect(dest.getX() - 13, dest.getY() - 13, 26, 26);
		}

		g.setColor(selectColor);
		g.setStroke(stroke);
		if(app.currentDestination != null)
		{
			g.drawOval(app.currentDestination.getX() - 10, app.currentDestination.getY() - 10, 20, 20);
			g.drawString("x pos: "+String.valueOf(app.currentDestination.getX()), app.currentDestination.getX(), app.currentDestination.getY());
			g.drawString("y pos: "+String.valueOf(app.currentDestination.getY()), app.currentDestination.getX(), app.currentDestination.getY()+50);
		}
	}

}
