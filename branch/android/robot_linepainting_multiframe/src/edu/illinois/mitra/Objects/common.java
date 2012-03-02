package edu.illinois.mitra.Objects;

import android.util.Log;

public final class common {
	private static final String TAG = "Common";
	private static final String ERR = "Critical Error";
	
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
