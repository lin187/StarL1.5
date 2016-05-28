package src.edu.illinois.mitra.demo.diffuse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.starl.objects.DSMVariable;
import edu.illinois.mitra.starl.objects.ItemPosition;
import java.util.Set;
import edu.illinois.mitra.starl.functions.DSMMultipleAttr;
import edu.illinois.mitra.starl.functions.DSMPubSub;

public class DiffuseApp extends LogicThread {
	private static final boolean RANDOM_DESTINATION = false;
	public static final int ARRIVED_MSG = 22;


	final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>(); // 4 corners,change source final
	ItemPosition currentDestination;
	ItemPosition myPos;
	Set<String> botset;
	int botsetsize;
	String agent_name ;
	int tracerefresh;
	int totalmsgreceived ;
	//DSMMultipleAttr Mydsm;
	DSMPubSub Mydsm;
	private enum Stage {
		PICK, GO, DONE
	};

	private Stage stage = Stage.PICK;

	public DiffuseApp(GlobalVarHolder gvh) {
		super(gvh);
		totalmsgreceived = 0;
		MotionParameters.Builder settings = new MotionParameters.Builder();
		settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
		MotionParameters param = settings.build();
		this.myPos = gvh.gps.getMyPosition();
		this.botset = gvh.id.getParticipants();
		botsetsize = botset.size();
		this.agent_name = gvh.id.getName();
		this.tracerefresh = 0;
		gvh.plat.moat.setParameters(param);
		for(ItemPosition i : gvh.gps.getWaypointPositions())
			destinations.put(i.getName(), i);
		gvh.comms.addMsgListener(this, ARRIVED_MSG);
		//this.Mydsm = new DSMMultipleAttr(gvh);
		this.Mydsm = new DSMPubSub(gvh);
	}

	@Override
	public List<Object> callStarL() {
		while(true) {
			tracerefresh +=1;
			myPos = gvh.gps.getMyPosition();
			boolean divergemotion = false;

			this.Mydsm.put(this.agent_name, this.agent_name, new String[]{"x", Integer.toString(myPos.getX()), "y", Integer.toString(myPos.getY())});

		//for flooding method
		/*	for	(DSMVariable v : Mydsm.getAll(this.agent_name, this.agent_name)) {
				if (!v.name.equals(this.agent_name)) {
					ItemPosition other = new ItemPosition(v.name,Integer.parseInt(v.values.get("x").s_value),Integer.parseInt(v.values.get("y").s_value),0);
					if (myPos.distanceTo(other) < 1000) {
						divergemotion = true;
						break;
					}
				}
			}*/

			this.Mydsm.checkmap();
			for	(DSMVariable v : Mydsm.getAll(this.agent_name, this.agent_name)) {
				if (!v.name.equals(this.agent_name)) {
					ItemPosition other = new ItemPosition(v.name,Integer.parseInt(v.values.get("x").s_value),Integer.parseInt(v.values.get("y").s_value),0);
					if (myPos.distanceTo(other) < 1000) {
						divergemotion = true;
						break;
					}
				}
			}



			if(!divergemotion) {
				gvh.plat.moat.inMotion = false;
			}

			switch(stage) {
				case PICK:
					if(destinations.isEmpty()) {
						stage = Stage.DONE;
					} else {
						currentDestination = getRandomElement(destinations);
						currentDestination = gvh.gps.getWaypointPositions().getList().get(Character.getNumericValue(this.agent_name.charAt(3)));
						destinations.remove(currentDestination.getName());
						gvh.plat.moat.goTo(currentDestination);
						stage = Stage.GO;
					}
					break;
				case GO:
					if(!gvh.plat.moat.inMotion) {
						if(currentDestination != null)
							destinations.remove(currentDestination.getName());
						stage = Stage.PICK;
					}
					break;
				case DONE:
					return null;
			}
			sleep(100);
		}
	}

	@Override
	protected void receive(RobotMessage m) {
		String posName = m.getContents(0);
		if(destinations.containsKey(posName))
			destinations.remove(posName);

		if(currentDestination.getName().equals(posName)) {
			gvh.plat.moat.cancel();
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
