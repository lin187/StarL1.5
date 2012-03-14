package edu.illinois.mitra.starl.objects;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;

public class TraceWriter {
	private static final String TAG = "RobotMotion";
	private static final String ERR = "Critical Error";
	
	private File logFile;
	private BufferedWriter buf;

	public TraceWriter(String filename) {
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy-mm-kk");
		String date = df.format(new Date());
		
		// Create the log file
		logFile = new File("sdcard/trace/" + filename + ".xml");
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
			Log.e(ERR, "Couldn't create buffered writer!");
			e.printStackTrace();
		}
	}
	
	public void sync(String source) {
		write("\t<sync>\n\t\t<source>"+source+"</source>\n\t\t<time>"+System.currentTimeMillis()+"</time>\n\t</sync>");
	}
	
	public void event(String source, String type, Object data) {
		if(data != null) {
			write("\t<event>\n\t\t<source>"+source+"</source>\n\t\t<time>"+System.currentTimeMillis()+"</time>\n\t\t<type>"+type+"</type>\n\t\t<data>"+data.toString()+"</data>\n\t</event>");
		} else {
			write("\t<event>\n\t\t<source>"+source+"</source>\n\t\t<time>"+System.currentTimeMillis()+"</time>\n\t\t<type>"+type+"</type>\n\t\t</event>");
		}
	}
	
	public void variable(String source, String var, Object value) {
		try {
			write("\t<variable>\n\t\t<source>"+source+"</source>\n\t\t<time>"+System.currentTimeMillis()+"</time>\n\t\t<varname>"+ var +"</varname>\n\t\t<class>"+value.getClass().getName()+"</class>\n\t\t<value>"+value.toString()+"</value>\n\t</variable>");
		} catch(NullPointerException e) {
			Log.e(ERR, "Tried to write a null value to the trace!");
			write("\t<variable>\n\t\t<source>"+source+"</source>\n\t\t<time>"+System.currentTimeMillis()+"</time>\n\t\t<varname>"+ var +"</varname>\n\t\t<class>?</class>\n\t\t<value>null</value>\n\t</variable>");
		}
	}
	
	public void close() {
		try {
			write("</trace>");
			buf.flush();
			buf.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			
		}
	}
	
	private void write(String text) {
		try {
			buf.append(text);
			buf.newLine();
			//buf.flush();
		} catch(IOException e) {	
		} catch(NullPointerException e) {
		}
	}
}
