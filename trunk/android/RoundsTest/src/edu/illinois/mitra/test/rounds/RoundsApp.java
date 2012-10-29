package edu.illinois.mitra.test.rounds;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;

public class RoundsApp extends LogicThread {

	// Round management
	public static enum Round {
		SEND, RECEIVE, DONE
	};

	public static final int TIME_DIVISOR = 1000;
	private long startTime = 0l;
	private static final int MSG_ID = 55;

	public RoundsApp(GlobalVarHolder gvh) {
		super(gvh);
		gvh.comms.addMsgListener(MSG_ID, this);
	}

	private static final Random rand = new Random();
	private Queue<Integer> valuesToSend = new LinkedList<Integer>();
	private SortedSet<Integer> received = new TreeSet<Integer>();
	private int roundNumber = 0;

	@Override
	public List<Object> callStarL() {
		startTime = gvh.time();

		while(true) {
			switch(getCurrentRound()) {
			case SEND:
				roundNumber++;
				if(!valuesToSend.isEmpty())
					broadcast(valuesToSend.remove());
				break;
			case RECEIVE:
				if(!received.isEmpty())
					System.out.println(name + " - " + roundNumber + " Selected value: " + received.first());
				break;
			case DONE:
				received.clear();
				if(rand.nextBoolean())
					valuesToSend.add(rand.nextInt(10));
				break;
			}
			sleep(TIME_DIVISOR);

			if(1 == 0)
				break;
		}
		return null;
	}

	private void broadcast(int val) {
		gvh.comms.addOutgoingMessage(new RobotMessage("ALL", name, MSG_ID, new MessageContents(val)));
		received.add(val);
	}

	private Round getCurrentRound() {
		return Round.values()[(int) (((gvh.time() - startTime) / TIME_DIVISOR) % Round.values().length)];
	}

	@Override
	protected void receive(RobotMessage m) {
		received.add(Integer.parseInt(m.getContents(0)));
	}

}
