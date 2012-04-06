package edu.illinois.mitra.starl.functions;

import java.util.List;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class Geocaster implements MessageListener {

	private GlobalVarHolder gvh;
	
	public Geocaster(GlobalVarHolder gvh) {
		this.gvh = gvh;
		gvh.comms.addMsgListener(Common.MSG_GEOCAST, this);
	}
	
	// Send a message with ID = MID and contents = msgcontents to all robots contained within the rectangle defined by x, y, width, and height
	public void sendGeocast(MessageContents msgcontents, int MID, int x, int y, int width, int height) {
		MessageContents geocastContents = new MessageContents("RECT", Integer.toString(x),Integer.toString(y),Integer.toString(width),Integer.toString(height),Integer.toString(MID));
		geocastContents.append(msgcontents);
		RobotMessage toSend = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_GEOCAST, geocastContents);
		gvh.comms.addOutgoingMessage(toSend);
	}
	
	public void sendGeocast(MessageContents msgcontents, int MID, int x, int y, int radius) {
		MessageContents geocastContents = new MessageContents("CIRCLE", Integer.toString(x),Integer.toString(y),Integer.toString(radius),Integer.toString(MID));
		geocastContents.append(msgcontents);
		RobotMessage toSend = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_GEOCAST, geocastContents);
		gvh.comms.addOutgoingMessage(toSend);		
	}

	@Override
	public void messageReceied(RobotMessage m) {
		List<String> contents = m.getContentsList();
		
		String type = contents.get(0);
		
		if(type.equals("RECT")) {
			int minX = Integer.parseInt(contents.get(1));
			int minY = Integer.parseInt(contents.get(2));
			int maxX = minX + Integer.parseInt(contents.get(3));
			int maxY = minX + Integer.parseInt(contents.get(4));
			
			ItemPosition mypos = gvh.gps.getMyPosition();
			
			if(Common.inRange(mypos.getX(), minX, maxX) && Common.inRange(mypos.getY(), minY, maxY)) {
				int MID = Integer.parseInt(contents.get(5));
				MessageContents receiveContents = new MessageContents();
				receiveContents.append(contents.subList(6, contents.size()));
				gvh.comms.addIncomingMessage(new RobotMessage("ALL", m.getFrom(), MID, receiveContents));
			}
		} else if(type.equals("CIRCLE")) {
			int x = Integer.parseInt(contents.get(1));
			int y = Integer.parseInt(contents.get(2));
			int radius = Integer.parseInt(contents.get(3));
			
			ItemPosition mypos = gvh.gps.getMyPosition();
			
			if(mypos.distanceTo(new ItemPosition("t",x,y,0)) <= radius) {
				int MID = Integer.parseInt(contents.get(4));
				MessageContents receiveContents = new MessageContents();
				receiveContents.append(contents.subList(5, contents.size()));
				gvh.comms.addIncomingMessage(new RobotMessage("ALL", m.getFrom(), MID, receiveContents));
			}
		}
	}
}
