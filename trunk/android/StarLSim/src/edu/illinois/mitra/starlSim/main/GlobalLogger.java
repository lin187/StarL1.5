package edu.illinois.mitra.starlSim.main;

import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.illinois.mitra.starlSim.draw.RobotData;

/**
 * Global logger for ground truth --- basically the same as the DrawFrame
 * viewer, but logs to text file
 * 
 * @author tjohnson
 * 
 */
public class GlobalLogger {
	private FileWriter fileWriter;

	/**
	 * 
	 * @param dir
	 *            directory for the log file
	 * @param filename
	 *            name for the log
	 */
	public GlobalLogger(String dir, String filename) {
		File file = new File(dir + filename);
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch(IOException e) {
			}
		}

		try {
			fileWriter = new FileWriter(file);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void updateData(ArrayList<RobotData> data, long time) {
		if(fileWriter == null)
			return;

		try {
			fileWriter.write(Long.toString(time) + "\n");
			for(RobotData d : data) {
				fileWriter.write(d.name + "," + d.x + "," + d.y + "," + d.degrees + "," + d.time + "\n");
			}
			fileWriter.write("\n\n");
			fileWriter.flush();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(NullPointerException e) {
			// Catch the NPE
		}
	}
}
