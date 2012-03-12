package edu.illinois.mitra.starl.objects;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.text.format.DateFormat;

public class TraceWriter {

	private File logFile;
	private BufferedWriter buf;
	
	public TraceWriter(String filename) {
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy-mm:kk");
		String date = df.format(new Date());
		
		// Create the log file
		logFile = new File("/sdcard/" + date + "-" + filename);
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
			write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<trace>\n\t<date>" + date + "</date>");
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sync(String source) {
		write("<sync>\n\t<source>"+source+"</source>\n\t<time>"+System.currentTimeMillis()+"</time>\n</sync>");
	}
	
	public void event(String source, String type, Object data) {
		if(data != null) {
			write("<event>\n\t<source>"+source+"</source>\n\t<time>"+System.currentTimeMillis()+"</time>\n\t<type>"+type+"</type>\n\t<data>"+data.toString()+"</data>\n</event>");
		} else {
			write("<event>\n\t<source>"+source+"</source>\n\t<time>"+System.currentTimeMillis()+"</time>\n\t<type>"+type+"</type>\n\t</event>");
		}
	}
	
	public void variable(String source, String var, Object value) {
		if(value != null) {
			write("<variable>\n\t<source>"+source+"</source>\n\t<time>"+System.currentTimeMillis()+"</time>\n\t<varname>"+ var +"</varname>\n\t<value>"+value.toString()+"</value>\n</variable>");
		} else {
			write("<variable>\n\t<source>"+source+"</source>\n\t<time>"+System.currentTimeMillis()+"</time>\n\t<varname>"+ var +"</varname>\n</variable>");
		}
	}
	
	public void close() {
		try {
			write("</trace>");
			buf.flush();
			buf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write(String text) {
		try {
			buf.append(text);
			buf.newLine();
			//buf.flush();
		} catch(IOException e) {}
	}
}
