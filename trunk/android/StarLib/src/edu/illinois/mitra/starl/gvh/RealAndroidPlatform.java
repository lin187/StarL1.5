package edu.illinois.mitra.starl.gvh;

import android.os.Handler;
import edu.illinois.mitra.starl.bluetooth.BluetoothInterface;
import edu.illinois.mitra.starl.bluetooth.MotionAutomaton;
import edu.illinois.mitra.starl.objects.Common;

public class RealAndroidPlatform extends AndroidPlatform {

	private Handler handler;
	
	public RealAndroidPlatform(Handler handler) {
		this.handler = handler;
	}
	
    public synchronized void setDebugInfo(String debugInfo) {
        sendMainMsg(Common.MESSAGE_DEBUG, debugInfo);
	}
	
	public synchronized void sendMainToast(String debugInfo) {
        sendMainMsg(Common.MESSAGE_TOAST, debugInfo);
	}
	
	public synchronized void sendMainMsg(int type, Object data) {
        handler.obtainMessage(type, -1, -1, data).sendToTarget();
	}
	
	public synchronized void sendMainMsg(int type, int arg1, int arg2) {	
		handler.obtainMessage(type, arg1, arg2, null).sendToTarget();
	}
}
