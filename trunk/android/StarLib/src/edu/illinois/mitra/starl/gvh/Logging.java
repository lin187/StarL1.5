package edu.illinois.mitra.starl.gvh;

public abstract class Logging {
	
	public abstract void e(String tag, String msg);
	
	public abstract void i(String tag, String msg);
	
	public abstract void d(String tag, String msg);

	public abstract String getLog();
	
	public abstract boolean saveLogFile();
}
