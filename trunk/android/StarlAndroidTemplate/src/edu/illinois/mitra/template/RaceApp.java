package edu.illinois.mitra.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class RaceApp extends LogicThread {
	private static final boolean RANDOM_DESTINATION = false;
	public static final int ARRIVED_MSG = 22;

	final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
	ItemPosition currentDestination;

	private enum Stage {
		INIT, PICK, GO, DONE, HOVER
	};

	private Stage stage = Stage.INIT;

	public RaceApp(GlobalVarHolder gvh) {
		super(gvh);
		MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
		settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
		MotionParameters param = settings.build();
		gvh.plat.moat.setParameters(param);
        for(ItemPosition i : gvh.gps.getWaypointPositions())
            destinations.put(i.getName(), i);
		gvh.comms.addMsgListener(this, ARRIVED_MSG);
	}

	@Override
	public List<Object> callStarL() {
		while(true) {
			switch(stage) {
            case INIT:
                for(ItemPosition i : gvh.gps.getWaypointPositions())
                    destinations.put(i.getName(), i);
                stage = Stage.PICK;
			case PICK:
				if(destinations.isEmpty()) {
					stage = Stage.HOVER;
				} else {
					currentDestination = getRandomElement(destinations);
					gvh.plat.moat.goTo(currentDestination);
					stage = Stage.GO;
				}
				break;
			case GO:
				if(!gvh.plat.moat.inMotion) {
					if(currentDestination != null)
						destinations.remove(currentDestination.getName());
					RobotMessage inform = new RobotMessage("ALL", name, ARRIVED_MSG, currentDestination.getName());
					gvh.comms.addOutgoingMessage(inform);
					stage = Stage.PICK;
				}
				break;
            case HOVER:
                break;
			case DONE:
				return null;
			}
			sleep(100);
		}
	}

	@Override
	protected void receive(RobotMessage m) {
        if(m.getMID() == ARRIVED_MSG && !m.getFrom().equals(name)) {
            gvh.log.d("Race", "Adding to message count from " + m.getFrom());
        }
		String posName = m.getContents(0);
		if(destinations.containsKey(posName))
			destinations.remove(posName);

		if(currentDestination.getName().equals(posName)) {
			gvh.plat.moat.motion_stop();
			stage = Stage.PICK;
		}
	}

	private static final Random rand = new Random();

	@SuppressWarnings("unchecked")
	private <X, T> T getRandomElement(Map<X, T> map) {
		if(RANDOM_DESTINATION)
			return (T) map.values().toArray()[rand.nextInt(map.size())];
		else
			return (T) map.values().toArray()[0];
	}
}
