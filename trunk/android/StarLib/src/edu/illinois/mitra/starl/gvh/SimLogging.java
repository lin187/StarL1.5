package edu.illinois.mitra.starl.gvh;

public class SimLogging extends Logging {

	private String simlog;
	private String name;
	private GlobalVarHolder gvh;
	
	public SimLogging(String name, GlobalVarHolder gvh) {
		this.name = name;
		this.gvh = gvh;
	}
	
	@Override
	public void e(String tag, String msg) {
		simlog += (name + "\t" + gvh.time() + "\te\t" + tag + " : " + msg + "\n");
	}

	@Override
	public void i(String tag, String msg) {
		simlog += (name + "\t" + gvh.time() + "\ti\t" + tag + " : " + msg + "\n");
	}

	@Override
	public void d(String tag, String msg) {
		simlog += (name + "\t" + gvh.time() + "\td\t" + tag + " : " + msg + "\n");
	}

	public String getLog() {
		return simlog;
	}
}
