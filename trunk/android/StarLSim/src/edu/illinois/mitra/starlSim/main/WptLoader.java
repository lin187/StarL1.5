package edu.illinois.mitra.starlSim.main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public final class WptLoader {
	
	private WptLoader() {
	}
	
	public static PositionList loadWaypoints(String dir) {
		PositionList waypoints = new PositionList();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(dir));
		} catch (FileNotFoundException e) {
			System.out.println("File " + dir + " not found! No waypoints loaded.");
			return new PositionList();
		}
		
		String line;
		try {
			while((line = in.readLine()) != null) {
				String[] parts = line.split(",");
				if(parts[0].equals("WAY") && parts.length == 5) {
					ItemPosition wpt = new ItemPosition(parts[4], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
					waypoints.update(wpt);
				}
			}
			in.close();
		} catch (IOException e) {
			System.out.println("Error reading waypoints file!");
		}
		return waypoints;
	}
}
