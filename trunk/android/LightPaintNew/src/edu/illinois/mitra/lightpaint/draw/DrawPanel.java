package edu.illinois.mitra.lightpaint.draw;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import edu.illinois.mitra.lightpaint.algorithm.LpAlgorithm;
import edu.illinois.mitra.starl.interfaces.Drawer;

@SuppressWarnings("serial")
public class DrawPanel extends ZoomablePanel {
	private Drawer alg;
	NumberFormat format = new DecimalFormat("0.00");
	int scaleFactor = 0;

	public DrawPanel() {
	}

	@Override
	protected void draw(Graphics2D g) {
		Point a = new Point(0, 0);
		Point b = new Point(0, 100);

		synchronized(this) {
			// Determine scale
			if(alg != null)
				alg.draw(g);
			scaleFactor = (int) toRealCoords(a).distance(toRealCoords(b));
		}
	}

	@Override
	protected void postDraw(Graphics2D g) {
		g.setColor(Color.black);
		g.setFont(new Font("Tahoma", Font.PLAIN, 15));

		g.drawString("SCALE: " + scaleFactor, getSize().width - 95, getSize().height - 15);
		g.drawLine(getSize().width - 110, getSize().height - 10, getSize().width - 10, getSize().height - 10);
	}

	public void updateData(Drawer alg) {
		synchronized(this) {
			this.alg = alg;
		}

		repaint();
	}
}
