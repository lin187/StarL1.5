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
		SvgParser parser = new SvgParser(2400,2400,1400,1400);

		// TODO: Sometimes a segment is painted over twice, some line segments are absent. See box.svg for example
		
		Set<ImageEdge> image = parser.parseImage("input_images/" + inputFilename + ".svg");
		WptWriter.writeWpt(WPT_PATH + inputFilename + ".wpt", image);

		SimSettings.Builder builder = new SimSettings.Builder().DRAWER(new LightPaintDrawer()).WAYPOINT_FILE(WPT_PATH + inputFilename + ".wpt").TIC_TIME_RATE(5);
		builder.N_BOTS(1);		
		builder.GRID_XSIZE(3000);
		builder.GRID_YSIZE(3000);
		SimSettings settings = builder.build();
		Simulation sim = new Simulation(LightPaintActivity.class, settings);
		sim.start();
	}

}