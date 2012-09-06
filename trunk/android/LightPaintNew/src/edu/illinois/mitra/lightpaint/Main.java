package edu.illinois.mitra.lightpaint;

import java.util.Set;

import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.utility.SvgParser;
import edu.illinois.mitra.lightpaint.utility.WptWriter;
import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	private static String inputFilename = "testimg";

	public static void main(String[] args) {
		SvgParser parser = new SvgParser(5500,5500,3000,3000);

		Set<ImageEdge> image = parser.parseImage("input_images/" + inputFilename + ".svg");
		WptWriter.writeWpt("waypoints/" + inputFilename + ".wpt", image);

		// TODO: Algorithm assigns overlapping regions sometimes! It looks like robot positions aren't always added into unsafe

		SimSettings.Builder builder = new SimSettings.Builder().DRAWER(new LightPaintDrawer()).WAYPOINT_FILE("waypoints/" + inputFilename + ".wpt").TIC_TIME_RATE(5);
		builder.N_BOTS(7);		
		builder.GRID_XSIZE(6000);
		builder.GRID_YSIZE(6000);
		SimSettings settings = builder.build();
		Simulation sim = new Simulation(MainActivity.class, settings);
		sim.start();
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

}