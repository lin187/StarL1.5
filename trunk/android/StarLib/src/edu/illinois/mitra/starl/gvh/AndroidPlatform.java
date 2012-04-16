package edu.illinois.mitra.starl.gvh;

import edu.illinois.mitra.starl.bluetooth.RobotMotion;

/**
 * Stub class implementing platform specific methods.
 * 
 * @author Adam Zimmerman
 * @version 1.0
 *
 */
public class AndroidPlatform {
	
	public RobotMotion moat;
		
    public void setDebugInfo(String debugInfo) {
	}
	
	public synchronized void sendMainToast(String debugInfo) {
	}
	
	public synchronized void sendMainMsg(int type, Object data) {
	}
	
	public synchronized void sendMainMsg(int type, int arg1, int arg2) {		
	}
}
