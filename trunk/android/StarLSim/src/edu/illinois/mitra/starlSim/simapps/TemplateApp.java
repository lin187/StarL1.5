package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimApp;

public class TemplateApp extends SimApp {

	private enum STAGE { START, DO_STUFF, DONE }
	private STAGE stage = STAGE.START;
	
	public TemplateApp(String name, HashMap<String, String> participants,
			SimComChannel channel, IdealSimGpsProvider gps,
			ItemPosition initpos) {
		super(name, participants, channel, gps, initpos, "C:\\traces\\");
		
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<String> call() throws Exception {
		while(true) {
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {}
			
			switch(stage) {
			case START:
				stage = STAGE.DO_STUFF;
				break;
			case DO_STUFF:
				// TODO: Do stuff here
				break;
			case DONE:
				return Arrays.asList(results);	
			}
		}
	}
}
