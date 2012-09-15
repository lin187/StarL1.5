package edu.illinois.mitra.lightpaint;

import java.util.Set;

import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.utility.SvgParser;
import edu.illinois.mitra.lightpaint.utility.WptWriter;
import edu.illinois.mitra.lightpaintlib.activity.LightPaintActivity;
import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

	private static String inputFilename = "pong";

	public static void main(String[] args) {
		SvgParser parser = new SvgParser(2500,2500,1300,1800);

		Set<ImageEdge> image = parser.parseImage("input_images/" + inputFilename + ".svg");
		WptWriter.writeWpt("waypoints/" + inputFilename + ".wpt", image);

		SimSettings.Builder builder = new SimSettings.Builder().DRAWER(new LightPaintDrawer()).WAYPOINT_FILE("waypoints/" + inputFilename + ".wpt").TIC_TIME_RATE(5);
		builder.N_BOTS(4);		
		builder.GRID_XSIZE(3000);
		builder.GRID_YSIZE(3000);
		SimSettings settings = builder.build();
		Simulation sim = new Simulation(LightPaintActivity.class, settings);
		sim.start();
	}

}