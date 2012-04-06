package edu.illinois.mitra.starl.objects;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.illinois.mitra.starl.interfaces.Traceable;

public class TraceWriter {
	private static final String TAG = "RobotMotion";
	private static final String ERR = "Critical Error";
	
	protected File logFile;
	protected BufferedWriter buf;
	private int level = 0;

	private boolean idealClocks = true;
	private int drift = 0;
	private double skew = 1.0;
	
	public TraceWriter(String filename, String dir) {
		SimpleDateFormat df = new SimpleDateFormat("mm:HH dd/MM/yyyy");
		String date = df.format(new Date());
		
		// Create the log file
		logFile = new File(dir + filename + ".xml");
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
			e.printStackTrace();
		}
	}
	
	public TraceWriter(String filename, String dir, int driftMax, float skewBound) {
		this(filename, dir);
		drift = (int)(Math.random()*2*driftMax) - driftMax;
		skew += (Math.random()*2*skewBound) - skewBound; 
		idealClocks = false;
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
		if(data != null) {
			try {
				if(data instanceof Traceable) {
					open("data");
					writeTag("class", data.getClass().getName());
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
				close("data");
			}
		}
		close("event");
	}
	
	public synchronized void variable(String source, String varname, Object value) {
		open("variable");
		writeTag("source", source);
		writeTimeTag();
		writeTag("varname", varname);
		
		try {
			if(value instanceof Traceable) {
				open("data");
				writeTag("class", value.getClass().getName());
				Traceable t = (Traceable) value;
				for(String tag : t.getXML().keySet()) {
					writeTag(tag,t.getXML().get(tag).toString());
				}
				close("data");
			} else {
				writeTag("data",value.toString());
			}
		} catch(NullPointerException e) {
			writeTag("data", "NULL");
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
	
	protected void write(String text) {
		try {
			for(int i = 0; i < level; i++) {
				buf.append("\t");
			}
			buf.append(text);
			buf.newLine();
		} catch(IOException e) {	
		} catch(NullPointerException e) {
		}
	}
	
	protected void writeTag(String tag, String contents) {
		write("<" + tag + ">" + contents + "</" + tag + ">");
	}
	protected void writeTimeTag() {
		if(idealClocks) {
			writeTag("time", Long.toString(System.currentTimeMillis()));
		} else {
			writeTag("time", Long.toString((long)(drift + skew*System.currentTimeMillis())));
		}
	}
	protected void open(String field) {
		write("<" + field + ">");
		level ++;
	}
	protected void close(String field) {
		level --;
		write("</" + field + ">");
	}
}
