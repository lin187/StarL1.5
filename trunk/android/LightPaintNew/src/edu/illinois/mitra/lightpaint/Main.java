package edu.illinois.mitra.lightpaint;

import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.UIManager;

import edu.illinois.mitra.lightpaint.algorithm.LpAlgorithm;
import edu.illinois.mitra.lightpaint.draw.DrawFrame;
import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;
import edu.illinois.mitra.lightpaint.utility.SvgParser;
import edu.illinois.mitra.lightpaint.utility.WptWriter;
import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	private static boolean simulate = false;
	
	public static void main(String[] args) {

		if(simulate) {
			Simulation sim = new Simulation(MainActivity.class, new SimSettings());
			sim.start();
		} else {
			try {
				// Set Native Look and Feel
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
			}
			DrawFrame frame = new DrawFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
	
			SvgParser parser = new SvgParser();
			parser.enableScaling(6000, 6000);
			parser.enableCentering(3000, 3000);
			
			Set<ImageEdge> image = parser.parseImage("input_images/linepath2.svg");
			WptWriter.writeWpt("waypoints/linepath2.wpt", image);
			
			LpAlgorithm alg = new LpAlgorithm(image, 50, 3300, 2*165);
			alg.setRobotPosition("A", new ImagePoint(0,0));
			alg.setRobotPosition("B", new ImagePoint(3350,1500));
			alg.setRobotPosition("Sleepy", new ImagePoint(3000,150));
			alg.setRobotPosition("AlsoSleepy", new ImagePoint(200, 2200));
			frame.updateData(alg);
			sleep(500);
	
			// Assign lines to a robot
			List<ImagePoint> toDraw = alg.assignSegment("A", new ImagePoint(0, 0));
			frame.repaint();
			sleep(500);
	
			// Assign lines to two other robots
			List<ImagePoint> toDrawB = alg.assignSegment("B", new ImagePoint(3350, 1500));
			if(toDrawB == null)
				System.out.println("B has nowhere to go");
			frame.repaint();
	
			// Free the lines
			if(toDraw != null) {
				for(int i = 1; i < toDraw.size(); i++) {
					sleep(800);
					alg.markSafeDrawn("A", toDraw.get(i - 1), toDraw.get(i));
					frame.repaint();
				}
			}
	
			System.out.println("Done? " + alg.isDone());
			
			if(toDrawB != null) {
				for(int i = 1; i < toDrawB.size(); i++) {
					sleep(800);
					alg.markSafeDrawn("B", toDrawB.get(i - 1), toDrawB.get(i));
					frame.repaint();
				}
			}
			
			System.out.println("Done? " + alg.isDone());
		}
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}