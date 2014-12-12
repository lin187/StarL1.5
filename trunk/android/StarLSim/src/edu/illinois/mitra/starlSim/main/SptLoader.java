package edu.illinois.mitra.starlSim.main;

import java.io.*;

//comment
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public final class SptLoader {
	
	private SptLoader() {
	}
	
	public static PositionList loadSensepoints(String file) {
		PositionList sensepoints = new PositionList();
		BufferedReader in = null;
        InputStream inputStream =
                WptLoader.class.getClassLoader().getResourceAsStream(file);
        if(inputStream != null){
            in = new BufferedReader(new InputStreamReader(inputStream));
        }else{

            try {
                in = new BufferedReader(new FileReader("waypoints/" + file));
            } catch (FileNotFoundException e) {
                System.err.println("File " + file + " not found! No sensepoints loaded.");
                return new PositionList();
            }
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