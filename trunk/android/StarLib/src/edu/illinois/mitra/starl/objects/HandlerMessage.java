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
	
	public static final int BLUETOOTH_CONNECTING = 2;
	public static final int BLUETOOTH_CONNECTED = 1;
	public static final int BLUETOOTH_DISCONNECTED = 0;
	public static final int GPS_RECEIVING = 1;
	public static final int GPS_OFFLINE = 0;
}
