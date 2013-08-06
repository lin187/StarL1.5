package edu.illinois.mitra.starlSim.main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
//comment
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public final class WptLoader {
	
	private WptLoader() {
	}
	
	public static PositionList loadWaypoints(String file) {
		PositionList waypoints = new PositionList();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			System.err.println("File " + file + " not found! No waypoints loaded.");
			return new PositionList();
		}
		
		String line;
		try {
			while((line = in.readLine()) != null) {
				String[] parts = line.replace(" ", "").split(",");
				if(parts[0].equals("WAY") && parts.length == 6) {
					ItemPosition wpt = new ItemPosition(parts[4], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[5]));
					waypoints.update(wpt, 0);
				}
			}
			in.close();
		} catch (IOException e) {
			System.out.println("Error reading waypoints file!");
		}
		return waypoints;
	}
}
