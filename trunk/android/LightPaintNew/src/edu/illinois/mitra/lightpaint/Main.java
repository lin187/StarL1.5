package edu.illinois.mitra.lightpaint;

import java.util.Set;

import edu.illinois.mitra.lightpaint.activity.LightPaintActivity;
import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.utility.SvgParser;
import edu.illinois.mitra.lightpaint.utility.WptWriter;
import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {
	private static final int WORLDSIZE = 3000;
	private static final int WORLDCENTER = WORLDSIZE/2;
	
	// TODO: Sometimes a segment is painted over twice, some line segments are absent. See box.svg for example. This *appears* to be a drawing glitch
	// TODO: Line segments are sometimes drawn from the wrong direction?
	
	private static final String WPT_PATH = "/waypoints";
	private static final String inputFilename = "kiva";

	public static void main(String[] args) {
		SvgParser parser = new SvgParser(WORLDSIZE,WORLDSIZE,WORLDCENTER,WORLDCENTER);
		
		Set<ImageEdge> image = parser.parseImage("input_images/" + inputFilename + ".svg");
		WptWriter.writeWpt(WPT_PATH + inputFilename + ".wpt", image);

		SimSettings.Builder builder = new SimSettings.Builder().DRAWER(new LightPaintDrawer(true)).WAYPOINT_FILE(WPT_PATH + inputFilename + ".wpt");
		builder.N_BOTS(4);
		builder.TIC_TIME_RATE(4);
		builder.DRAW_WAYPOINT_NAMES(false);
		builder.DRAW_WAYPOINTS(false);
		builder.MSG_LOSSES_PER_HUNDRED(0);
		builder.GRID_XSIZE(WORLDSIZE).GRID_YSIZE(WORLDSIZE);
		builder.TRACE_OUT_DIR(null);
	
		SimSettings settings = builder.build();
		System.out.println("Starting with " + settings.N_BOTS + " robots.");
		Simulation sim = new Simulation(LightPaintActivity.class, settings);
		sim.start();
		System.out.println(sim.getMessageStatistics());
		System.out.println("Elapsed simulation time: " + sim.getSimulationDuration() + " ms");
	}

}