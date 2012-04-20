package edu.illinois.mitra.starl.bluetooth;

public class MotionParameters {
	public static final int USE_COLAVOID = 0;
	public static final int STOP_ON_COLLISION = 1;
	public static final int BUMPERCARS = 2;
	
	
	public int TURNSPEED_MAX = 110;
	public int TURNSPEED_MIN = 25;
	
	public int LINSPEED_MAX = 250;
	public int LINSPEED_MIN = 175;
	
	public boolean STOP_AT_DESTINATION = true;
	
	public boolean ENABLE_ARCING = true;
	public int ARCSPEED_MAX = 200;
	public int ARCANGLE_MAX = 25;
	
	public int COLAVOID_MODE = USE_COLAVOID;	
}
