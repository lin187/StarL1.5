package edu.illinois.mitra.starl.objects;

import android.util.Log;

public final class common {
	private static final String TAG = "Common";
	private static final String ERR = "Critical Error";
	
	// Message IDs
	public static final int MSG_BARRIERSYNC 			= 1;
	public static final int MSG_MUTEX_TOKEN_OWNER_BCAST = 2;
	public static final int MSG_MUTEX_TOKEN 			= 3;
	public static final int MSG_MUTEX_TOKEN_REQUEST 	= 4;
	public static final int MSG_LEADERELECT 			= 5;
	public static final int MSG_LEADERELECT_ANNOUNCE	= 6;
	
	// GUI Message handler
	public static final int MESSAGE_TOAST = 0;
	public static final int MESSAGE_LOCATION = 1;
	public static final int MESSAGE_BLUETOOTH = 2;
	public static final int MESSAGE_LAUNCH = 3;
	public static final int MESSAGE_DEBUG = 4;
	public static final int MESSAGE_BATTERY = 7;
	public static final int MESSAGE_MOTION = 8;
	// Motion types
	public static final int MOT_TURNING		= 0;
	public static final int MOT_ARCING		= 1;
	public static final int MOT_STRAIGHT	= 2;
	public static final int MOT_STOPPED		= 3;

	
	private common() {
	}
	
	public static int[] partsToInts(String[] parts) {
		int[] retval = new int[parts.length];
		for(int i = 0; i < parts.length; i++) {
			try {
				retval[i] = Integer.parseInt(parts[i]);
			} catch(NumberFormatException e) {
				Log.e(TAG, "Can't parse " + parts[i] + " as an integer!");
				return null;
			}
		}
		return retval;
	}
	
	public static int[] partsToInts(String str, String delimiter) {
		String[] parts = str.split(delimiter);
		return partsToInts(parts);
	}
	
	// Common value manipulation functions	
	public static <T extends Comparable<T>> T cap(T val, T max) {
		if(val.compareTo(max) < 0) {
			return val;
		} else {
			return max;
		}
	}
	
	public static <T extends Comparable<T>> T  cap(T val, T min, T max) {
		if(val.compareTo(max) > 0) {
			return max;
		} else if(val.compareTo(min) < 0) {
			return min;
		} else {
			return val;
		}
	}
}
