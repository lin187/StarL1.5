package edu.illinois.mitra.demo.numadd;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class AddNumDrawer extends Drawer {

	private Stroke stroke = new BasicStroke(8);
	private Color selectColor = new Color(0,0,255,100);
	
	@Override
	public void draw(LogicThread lt, Graphics2D g) {
		AddNum app = (AddNum) lt;

		g.setColor(Color.RED);
		g.setColor(selectColor);
		g.setStroke(stroke);
		if(app.position != null){
		g.drawString("current total "+String.valueOf(app.currentTotal), app.position.x, app.position.y);
		g.drawString("numadded: "+String.valueOf(app.numAdded), app.position.x, app.position.y+50);
		g.drawString("final Sum: " + String.valueOf(app.finalSum), app.position.x, app.position.y+100);
		}
	}

}
