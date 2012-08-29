package edu.illinois.mitra.lightpaint;

import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.UIManager;

import edu.illinois.mitra.draw.DrawFrame;
import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;
import edu.illinois.mitra.lightpaint.utility.SvgParser;
import edu.illinois.mitra.lightpaint.utility.WptWriter;

public class Main {

	public static void main(String[] args) {

		try {
			// Set Native Look and Feel
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}

		DrawFrame frame = new DrawFrame();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		SvgParser parser = new SvgParser();
		parser.enableScaling(5000, 5000);
		parser.enableCentering(2500, 2500);
		
		Set<ImageEdge> image = parser.parseImage("linepath2.svg");
		WptWriter.writeWpt("C:/linepath2.wpt", image);

		LpAlgorithm alg = new LpAlgorithm(image, 50, 1000, 165);
		alg.setRobotPosition("Sleepy", new ImagePoint(100,750));
		alg.setRobotPosition("AlsoSleepy", new ImagePoint(1500, 600));
		frame.updateData(alg);
		sleep(500);

		// Assign lines to a robot
		List<ImagePoint> toDraw = alg.assignSegment("A", new ImagePoint(0, 0));
		frame.repaint();
		sleep(500);

		// Assign lines to two other robots
		List<ImagePoint> toDrawB = alg.assignSegment("B", new ImagePoint(1350, 500));
		if(toDrawB == null)
			System.out.println("B has nowhere to go");
		frame.repaint();

/*		// Free the lines
		for(int i = 1; i < toDraw.size(); i++) {
			sleep(500);
			alg.markSafeDrawn("A", toDraw.get(i - 1), toDraw.get(i));
			frame.repaint();
		}

		System.out.println("Done? " + alg.isDone());
		
		if(toDrawB != null) {
			for(int i = 1; i < toDrawB.size(); i++) {
				sleep(500);
				alg.markSafeDrawn("B", toDrawB.get(i - 1), toDrawB.get(i));
				frame.repaint();
			}
		}*/
		
		System.out.println("Done? " + alg.isDone());
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}