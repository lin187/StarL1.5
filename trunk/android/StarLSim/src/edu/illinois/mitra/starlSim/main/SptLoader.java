package edu.illinois.mitra.starlSim.main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

//comment
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public final class SptLoader {
	
	private SptLoader() {
	}
	
	public static PositionList loadSensepoints(String file) {
		PositionList sensepoints = new PositionList();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			System.err.println("File " + file + " not found! No sensepoints loaded.");
			return new PositionList();
		}
		
		String line;
		try {
			while((line = in.readLine()) != null) {
				String[] parts = line.replace(" ", "").split(",");
				if(parts[0].equals("SENSE") && parts.length == 5) {
					ItemPosition sencePt = new ItemPosition(parts[4], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
					sensepoints.update(sencePt, 0);
				}
			}
			in.close();
		} catch (IOException e) {
			System.out.println("Error reading Sensepoints file!");
		}
		return sensepoints;
	}
}