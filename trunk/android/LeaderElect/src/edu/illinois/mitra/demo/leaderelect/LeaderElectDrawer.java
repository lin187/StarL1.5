package edu.illinois.mitra.demo.leaderelect;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class LeaderElectDrawer extends Drawer {

	private Stroke stroke = new BasicStroke(8);
	private Color selectColor = new Color(0,0,255,100);
	
	@Override
	public void draw(LogicThread lt, Graphics2D g) {
		LeaderElectApp app = (LeaderElectApp) lt;

		g.setColor(Color.RED);/*
		for(ItemPosition dest : app.destinations.values()) {
			g.fillRect(dest.getX() - 13, dest.getY() - 13, 26, 26);
		}
*/
		g.setColor(selectColor);
		g.setStroke(stroke);
		if(app.position != null){
			int x = app.position.getX() ;
			int y= app.position.getY();
			//g.drawOval(app.position.getX() - 20, app.position.getY() - 20, 40, 40);
			g.drawString("Candidate: "+app.candidate, x, y);
			g.drawString("numVotes: "+app.numVotes , x, y+50);
			g.drawString("LeaderID: "+app.LeaderId , x, y+100);
		}
	}

}
