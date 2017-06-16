package edu.illinois.mitra.starl.objects;

public class HandlerMessage {
	// GUI Message handler
	public static final int MESSAGE_TOAST = 0;
	public static final int MESSAGE_LOCATION = 1;
	public static final int MESSAGE_BLUETOOTH = 2;
	public static final int MESSAGE_LAUNCH = 3;
	public static final int MESSAGE_ABORT = 4;
	public static final int MESSAGE_DEBUG = 5;
	public static final int MESSAGE_BATTERY = 6;
	public static final int MESSAGE_REGISTERED_DJI = 7; //status of MAVIC SDK registration
	public static final int MESSAGE_REGISTERED_3DR = 8; //status of 3DR sdk registration
	public static final int AVATAR_MODE = 9;//GHOST avatar
	public static final int MANUAL_MODE = 10; //GHOST manual
	public static final int  HEARTBEAT_MODE = 11; //GHOST HEARTBEAT
	public static final int COPTER_CONNECTED =  12; //GHOST connected
	public static final int ARM_TOGGLE = 13; //GHOST arm/disarm
	
	public static final int BLUETOOTH_CONNECTING = 2;
	public static final int BLUETOOTH_CONNECTED = 1;
	public static final int BLUETOOTH_DISCONNECTED = 0;
	public static final int GPS_RECEIVING = 1;
	public static final int GPS_OFFLINE = 0;
}
