package edu.illinois.mitra.starl.gvh;

public class SimLogging extends Logging {

	private String simlog;
	private String name;
	
	public SimLogging(String name) {
		this.name = name;
	}
	
	@Override
	public void e(String tag, String msg) {
		simlog += (name + "\t" + System.currentTimeMillis() + "\te\t" + tag + " : " + msg + "\n");
	}

	@Override
	public void i(String tag, String msg) {
		simlog += (name + "\t" + System.currentTimeMillis() + "\ti\t" + tag + " : " + msg + "\n");
	}

	@Override
	public void d(String tag, String msg) {
		simlog += (name + "\t" + System.currentTimeMillis() + "\td\t" + tag + " : " + msg + "\n");
	}

	public String getLog() {
		return simlog;
	}
}
