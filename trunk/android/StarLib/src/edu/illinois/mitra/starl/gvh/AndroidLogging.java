package edu.illinois.mitra.starl.gvh;

import android.util.Log;

public class AndroidLogging extends Logging {

	@Override
	public void e(String tag, String msg) {
		Log.e(tag,msg);
	}

	@Override
	public void i(String tag, String msg) {
		Log.i(tag,msg);
	}

	@Override
	public void d(String tag, String msg) {
		Log.d(tag,msg);
	}

	@Override
	public String getLog() {
		return null;
	}

    @Override
    public boolean saveLogFile() {
        return false;
    }
}
