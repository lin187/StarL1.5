package edu.illinois.mitra.starl.gvh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

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

	@Override
	public String getLog() {
		return simlog;
	}
	
	@Override
	public boolean saveLogFile(){
		File tmp = new File("logs/" + name);
		tmp.getParentFile().mkdirs();
		
		try {
			tmp.createNewFile();
			PrintWriter out = new PrintWriter("logs/" + name);
			out.println(getLog());
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
