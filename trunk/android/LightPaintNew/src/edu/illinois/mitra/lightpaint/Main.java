package edu.illinois.mitra.lightpaint;

import java.util.Set;

import edu.illinois.mitra.lightpaint.activity.LightPaintActivity;
import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.utility.SvgParser;
import edu.illinois.mitra.lightpaint.utility.WptWriter;
import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	private static String inputFilename = "box";

	public static void main(String[] args) {
		SvgParser parser = new SvgParser(5000,5000, 2500, 2500);

		// TODO: Sometimes a segment is painted over twice, some line segments are absent. See box.svg for example
		
		Set<ImageEdge> image = parser.parseImage("input_images/" + inputFilename + ".svg");
		WptWriter.writeWpt("waypoints/" + inputFilename + ".wpt", image);

		SimSettings.Builder builder = new SimSettings.Builder().DRAWER(new LightPaintDrawer()).WAYPOINT_FILE("waypoints/" + inputFilename + ".wpt").TIC_TIME_RATE(5);
		builder.N_BOTS(1);		
		builder.GRID_XSIZE(5000);
		builder.GRID_YSIZE(5000);
		SimSettings settings = builder.build();
		Simulation sim = new Simulation(LightPaintActivity.class, settings);
		sim.start();
	}

}