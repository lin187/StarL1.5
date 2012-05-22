package edu.illinois.mitra.starlSim.main;

import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.illinois.mitra.starlSim.draw.RobotData;

/**
 * Global logger for ground truth --- basically the same as the DrawFrame viewer, but logs to text file
 * @author tjohnson
 *
 */
public class GlobalLogger {
	private File _f;
	private FileWriter _fw;
	
	/**
	 * 
	 * @param dir - directory for the log file
	 * @param filename - name for the log
	 */
	public GlobalLogger(String dir, String filename) {
		_f = new File(dir + filename);
		try {
			_f.createNewFile();
			_fw = new FileWriter(_f);
		}
		catch (IOException e) {

		}
		
	}
	
	public void updateData(ArrayList <RobotData> data, long time)
	{
		try {
			_fw.write(Long.toString(time) + "\n");
			for (RobotData d : data) {
				_fw.write(d.name + "," + d.x + "," + d.y + "," + d.degrees + "," + d.time + "\n");
			}
			_fw.write("\n\n");
			_fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			// Catch the NPE
		}
		
	}
}
