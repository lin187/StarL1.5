package edu.illinois.mitra.starl.objects;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TraceWriter {

	File logFile;
	BufferedWriter buf;
	Long startTime = 0l;
	
	public TraceWriter(String filename) {
		// Create the log file
		logFile = new File("/sdcard/" + filename);
		if(logFile.exists()) {
			logFile.delete();
		}
		try {
			logFile.createNewFile();
		} catch(IOException e) {
			e.printStackTrace();
		}
		// Create a buffered writer for the log file
		try {
			buf = new BufferedWriter(new FileWriter(logFile, true));
			write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
	}
	
	public void event(String source, Object data) {
		write("<event source=\""+source+"\" time=\"" + (System.currentTimeMillis() - startTime) + "\" data=\""+data.toString()+"\"/>");
	}
	
	public void variable(String source, String var, Object value) {
		write("<variable source=\""+source+"\" time=\"" + (System.currentTimeMillis() - startTime) + "\" varname=\""+ var +"\" value=\""+value.toString()+"\"/>");		
	}
	
	public void close() {
		try {
			buf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write(String text) {
		try {
		buf.append(text);
		buf.newLine();
		buf.flush();
		} catch(IOException e) {}
	}
}
