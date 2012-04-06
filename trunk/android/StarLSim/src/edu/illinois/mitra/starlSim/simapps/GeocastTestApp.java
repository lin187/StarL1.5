package edu.illinois.mitra.starlSim.simapps;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.Geocaster;
import edu.illinois.mitra.starl.harness.IdealSimGpsProvider;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.SimComChannel;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starlSim.main.SimApp;
import edu.illinois.mitra.starlSim.main.SimSettings;

public class GeocastTestApp extends SimApp implements MessageListener {
	
	private enum STAGE { START, SEND, WAIT, DONE }
	private STAGE stage = STAGE.START;
	private Geocaster geo;
	
	public GeocastTestApp(String name, HashMap<String, String> participants, SimComChannel channel, IdealSimGpsProvider gps, ItemPosition initpos) {
		super(name, participants, channel, gps, initpos, "C:\\");
		gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
		
		geo = new Geocaster(gvh);
		
		// Register this as a listener for messages with ID=99
		gvh.comms.addMsgListener(99, this);
	}

	@Override
	public List<String> call() throws Exception {
		while(true) {
			switch(stage) {
			case START:
				Thread.sleep((long) (Math.random()*SimSettings.START_DELAY_MAX));
				gvh.trace.traceSync("Launch");
				stage = STAGE.SEND;
				break;
			case SEND:
				stage = STAGE.WAIT;
				// Don't send a geocast if you're inside the target region
				if(gvh.gps.getMyPosition().getX() < 500) {
					MessageContents contents = new MessageContents("hello from", name);
					// Rectangular target area:
					geo.sendGeocast(contents, 99, 500, 500, 500, 500);
					// Circular target area:
					//geo.sendGeocast(contents, 99, 500, 500, 500);
					System.out.println(gvh.id.getName() + " sent a geocast.");
					stage = STAGE.DONE;
				}
				break;
			case WAIT:
				// Waiting to receive a geocast
				break;
			case DONE:
				gvh.trace.traceEnd();
				return Arrays.asList(results);
			}
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {}
		}
	}

	@Override
	public void messageReceied(RobotMessage m) {
		System.out.println(name + " received a geocast message: " + m.getContentsList().toString());
		synchronized(stage) {
			stage = STAGE.DONE;
		}
	}
}
