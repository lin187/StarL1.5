package edu.illinois.mitra.starl.bluetooth;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.RobotEventListener;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;

/**
 * iRobot Create specific. Experimental location tracker using GPS broadcasts and iRobot Create sensor information
 * to calculate the robot's position more frequently. <b>Currently slightly very wrong.</b> 
 * @author Adam Zimmerman
 * @version 1.0
 */
public class DeadReckoner extends Thread implements RobotEventListener {
	private static final String TAG = "DeadReckoner";
	private static final String ERR = "Critical Error";
	
	private static final int MAXSAMPLES = 50;
	
	private BluetoothInterface bti;
	private GlobalVarHolder gvh;
	private int period;
	private ItemPosition latestpos;
	
	private ArrayList<Integer> xhat;
	private ArrayList<Integer> yhat;
	private ArrayList<Integer> ahat;
	private int writeidx = 0;
	
	public DeadReckoner(GlobalVarHolder gvh, BluetoothInterface bti, int period) {
		super();
		this.gvh = gvh;
		this.period = period;
		this.bti = bti;
		
		xhat = new ArrayList<Integer>();
		xhat.ensureCapacity(MAXSAMPLES);
		yhat = new ArrayList<Integer>();
		yhat.ensureCapacity(MAXSAMPLES);
		ahat = new ArrayList<Integer>();
		ahat.ensureCapacity(MAXSAMPLES);
		
		gvh.addEventListener(this);
		
		latestpos = gvh.gps.getMyPosition();
	}

	public ItemPosition getLatestEstimate() {
		int Xhat = 0, Yhat = 0, Ahat = 0;
		for(int i = 0; i < writeidx; i ++) {
			Xhat += xhat.get(i);
			Yhat += yhat.get(i);
			Ahat += ahat.get(i);
		}
		int angleGuess = Common.angleWrap(Ahat + latestpos.angle);
		ItemPosition retval = new ItemPosition(latestpos.name, Xhat + latestpos.x, Yhat + latestpos.y, angleGuess);
		gvh.plat.setDebugInfo(retval.toString() + "   " + writeidx + "\n" + gvh.gps.getMyPosition().toString());
		return retval;
	}

	@Override
	public void run() {
		super.run();
		ByteBuffer bb = ByteBuffer.allocate(2);
		while(true) {
			sleep(period);
			
			int dist = -1;

			byte[] readDist = new byte[]{0,0};//bti.sendReceive(BluetoothCommands.req_sensor(19), 2);
			byte[] readAngle = bti.sendReceive(BluetoothCommands.req_sensor(20), 2);

			dist = Common.signedShortToInt(readDist);
			
			bb.order(ByteOrder.BIG_ENDIAN);
			bb.put(readAngle);
			short angle = bb.getShort(0);
			bb.clear();

			if(dist != 0 || angle != 0) {
				gvh.log.i(TAG, "Distance: " + readDist[0] + " " + readDist[1]);
				gvh.log.i(TAG, "Angle: " + readAngle[0] + " " + readAngle[1]);
				gvh.log.d(TAG, "Distance: " + dist + " angle: " + angle);
			}
			
			xhat.add(writeidx, (int) (Math.cos(Math.toRadians(latestpos.angle))*dist));
			yhat.add(writeidx, (int) (Math.sin(Math.toRadians(latestpos.angle))*dist));
			ahat.add((int) angle);
			writeidx ++;
			
			if(writeidx == MAXSAMPLES) {
				avgCollapse();
				writeidx = 1;
			}
		}
	}

	@Override
	public synchronized void start() {
		super.start();
		latestpos = gvh.gps.getMyPosition();
		
		// Reset the iRobot's distance traveled
		bti.sendReceive(BluetoothCommands.req_sensor(19), 2);
		bti.sendReceive(BluetoothCommands.req_sensor(20), 2);
	}

	public synchronized void robotEvent(int type, int event) {
		if(type == Common.EVENT_GPS_SELF) {
			latestpos = gvh.gps.getMyPosition();
			writeidx = 0;
			xhat.clear();
			yhat.clear();
			ahat.clear();
		}
	}
	
	// Take the average of all written samples and store it in the first slot
	private void avgCollapse() {
		int xsum = 0, ysum = 0, asum = 0;
		for(int i = 0; i < writeidx; i++) {
			xsum += xhat.get(i);
			ysum += yhat.get(i);
			asum += ahat.get(i);
		}
		xsum /= MAXSAMPLES;
		ysum /= MAXSAMPLES;
		asum /= MAXSAMPLES;
		xhat.clear();
		yhat.clear();
		ahat.clear();
		xhat.add(0,xsum);
		yhat.add(0,ysum);
		ahat.add(0,asum);
	}
	
	private void sleep(int time) {
		try {
			Thread.sleep(period);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
