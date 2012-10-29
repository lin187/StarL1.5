package edu.illinois.mitra.starl.gvh;

import java.util.Map;

import android.os.Handler;
import edu.illinois.mitra.starl.comms.SmartUdpComThread;
import edu.illinois.mitra.starl.comms.UdpGpsReceiver;
import edu.illinois.mitra.starl.motion.BluetoothInterface;
import edu.illinois.mitra.starl.motion.MotionAutomaton;
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
	public RealGlobalVarHolder(String name, Map<String,String> participants, Handler handler, String robotMac) {
		super(name, participants);

		super.log = new AndroidLogging();
		super.trace = new Trace(name, "/sdcard/trace/", this);
		super.plat = new RealAndroidPlatform(handler);
		super.comms = new Comms(this, new SmartUdpComThread(this));
		super.gps = new Gps(this, new UdpGpsReceiver(this,"192.168.1.100",4000,new PositionList(),new PositionList()));
		plat.moat = new MotionAutomaton(this, new BluetoothInterface(this, robotMac.trim()));
		plat.moat.start();
	}

	@Override
	public void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public long time() {
		return System.currentTimeMillis();
	}

	@Override
	public void threadCreated(Thread thread) {
		// Nothing happens here
	}

	@Override
	public void threadDestroyed(Thread thread) {
		// Nothing happens here		
	}
}
