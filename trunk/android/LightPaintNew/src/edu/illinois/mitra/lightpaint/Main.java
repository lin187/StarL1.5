package edu.illinois.mitra.lightpaint;

import java.util.Set;

import edu.illinois.mitra.lightpaint.activity.LightPaintActivity;
import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.utility.SvgParser;
import edu.illinois.mitra.lightpaint.utility.WptWriter;
import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {
	private static final String WPT_PATH = "C:/Users/StarL/Documents/Workspace/starl/trunk/matlab/matlab_optitrack/waypoints/";
	
	private static final String inputFilename = "knot";

	public static void main(String[] args) {
		SvgParser parser = new SvgParser(2200,2200,1200,1300);

		// TODO: Sometimes a segment is painted over twice, some line segments are absent. See box.svg for example
		// TODO: Line segments are sometimes drawn from the wrong direction!
		
		Set<ImageEdge> image = parser.parseImage("input_images/" + inputFilename + ".svg");
		WptWriter.writeWpt(WPT_PATH + inputFilename + ".wpt", image);

		SimSettings.Builder builder = new SimSettings.Builder().DRAWER(new LightPaintDrawer()).WAYPOINT_FILE(WPT_PATH + inputFilename + ".wpt");
		builder.N_BOTS(1);
		builder.TIC_TIME_RATE(2.5);
		builder.DRAW_WAYPOINT_NAMES(false);
		builder.DRAW_WAYPOINTS(false);
		builder.GRID_XSIZE(3000);
		builder.GRID_YSIZE(3000);
		SimSettings settings = builder.build();
		Simulation sim = new Simulation(LightPaintActivity.class, settings);
		sim.start();
	}

}