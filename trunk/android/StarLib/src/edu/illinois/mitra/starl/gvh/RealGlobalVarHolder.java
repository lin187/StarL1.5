package edu.illinois.mitra.starl.gvh;

import java.util.HashMap;

import android.os.Handler;
import edu.illinois.mitra.starl.bluetooth.BluetoothInterface;
import edu.illinois.mitra.starl.bluetooth.BluetoothRobotMotion;
import edu.illinois.mitra.starl.bluetooth.MotionAutomaton;
import edu.illinois.mitra.starl.comms.UdpComThread;
import edu.illinois.mitra.starl.comms.UdpGpsReceiver;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.PositionList;

/**
 * Extension of the GlobalVarHolder class for use in physical implementations of StarL applications
 * @author Adam Zimmerman
 * @version 1.0
 */
public class RealGlobalVarHolder extends GlobalVarHolder {

	/**
	 * @param name the name of this agent
	 * @param participants contains (name,IP) pairs for each participating agent
	 * @param handler the main application handler capable of receiving GUI update messages
	 * @param robotMac the MAC address of this agent's iRobot Create chassis
	 */
	public RealGlobalVarHolder(String name, HashMap<String,String> participants, Handler handler, String robotMac) {
		super(name, participants);

		super.log = new AndroidLogging();
		super.trace = new Trace(name, "/sdcard/trace/");
		super.plat = new RealAndroidPlatform(handler);
		super.comms = new Comms(this, new UdpComThread(this));
		super.gps = new Gps(this, new UdpGpsReceiver(this,"192.168.1.100",4000,new PositionList(),new PositionList()));
		plat.moat = new MotionAutomaton(this, new BluetoothInterface(this, robotMac));
	}
}
