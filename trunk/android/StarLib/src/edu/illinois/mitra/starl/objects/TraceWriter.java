package edu.illinois.mitra.starl.objects;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;
import edu.illinois.mitra.starl.interfaces.Traceable;

public class TraceWriter {
	private static final String TAG = "RobotMotion";
	private static final String ERR = "Critical Error";
	
	private File logFile;
	private BufferedWriter buf;
	private int level = 0;

	public TraceWriter(String filename) {
		SimpleDateFormat df = new SimpleDateFormat("mm:HH dd/MM/yyyy");
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
			write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			open("trace");
			writeTag("date", date);
		} catch(IOException e) {
			Log.e(ERR, "Couldn't create buffered writer!");
			e.printStackTrace();
		}
	}
	
	public synchronized void sync(String source) {
		open("sync");
		writeTag("source", source);
		writeTimeTag();
		close("sync");
	}
	
	public synchronized void event(String source, String type, Object data) {
		open("event");
		writeTag("source", source);
		writeTimeTag();
		writeTag("type", type);
		try {
			if(data instanceof Traceable) {
				writeTag("class", data.getClass().getSimpleName());
				open("data");
				Traceable t = (Traceable) data;
				for(String tag : t.getXML().keySet()) {
					writeTag(tag,t.getXML().get(tag).toString());
				}
				close("data");
			} else {
				writeTag("data",data.toString());
			}
		} catch(NullPointerException e) {
			// Don't write null data for event tags
			//writeTag("data", "NULL");
		}
		close("event");
	}
	
	public synchronized void variable(String source, String varname, Object value) {
		open("variable");
		writeTag("source", source);
		writeTimeTag();
		writeTag("varname", varname);
		
		try {
			writeTag("class", value.getClass().getSimpleName());
			if(value instanceof Traceable) {
				open("value");
				Traceable t = (Traceable) value;
				for(String tag : t.getXML().keySet()) {
					writeTag(tag,t.getXML().get(tag).toString());
				}
				close("value");
			} else {
				writeTag("value",value.toString());
			}
		} catch(NullPointerException e) {
			writeTag("value", "NULL");
		}

		close("variable");
	}
	
	public void close() {
		try {
			close("trace");
			buf.flush();
			buf.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			
		}
	}
	
	private void write(String text) {
		try {
			for(int i = 0; i < level; i++) {
				buf.append("\t");
			}
			buf.append(text);
			buf.newLine();
			//buf.flush();
		} catch(IOException e) {	
		} catch(NullPointerException e) {
		}
	}
	
	private void writeTag(String tag, String contents) {
		write("<" + tag + ">" + contents + "</" + tag + ">");
	}
	private void writeTimeTag() {
		writeTag("time", Long.toString(System.currentTimeMillis()));
	}
	private void open(String field) {
		write("<" + field + ">");
		level ++;
	}
	private void close(String field) {
		level --;
		write("</" + field + ">");
	}
}
