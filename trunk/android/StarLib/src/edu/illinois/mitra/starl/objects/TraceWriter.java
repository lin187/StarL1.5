package edu.illinois.mitra.starl.objects;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.Traceable;

/**
 * TraceWriter maintains an XML and provides a simple interface for writing system events and variables.
 * @author Adam Zimmerman
 * @version 1.1
 *
 * @see Traceable 
 */
public class TraceWriter {
	private static final String TAG = "RobotMotion";
	private static final String ERR = "Critical Error";
	
	protected File logFile;
	protected BufferedWriter buf;
	private int level = 0;
	
	private GlobalVarHolder gvh;
	
	/**
	 * Create a new TraceWriter and open a file for writing
	 * @param filename the output filename
	 * @param dir the output directory
	 */
	public TraceWriter(String filename, String dir, GlobalVarHolder gvh) {
		this.gvh = gvh;
		SimpleDateFormat df = new SimpleDateFormat("mm:HH dd/MM/yyyy");
		String date = df.format(new Date());
		
		// TODO: refactor / only do for simulation
		logFile = new File(dir);
		if (!logFile.exists()) {
			logFile.mkdirs(); // create directory hierarchy if it doesn't exist
		}
		
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
		
	/**
	 * Write a synchronization tag featuring the sync source and current timestamp. Used to
	 * align trace files of the same execution from multiple agents. 
	 * @param source the thread which triggered the synchronization
	 */
	public synchronized void sync(String source, long timestamp) {
		open("sync");
		writeTag("source", source);
		writeTimeTag(timestamp);
		close("sync");
	}
	
	/**
	 * Write an event tag to the trace file
	 * @param source the thread in which the event occurred
	 * @param type the event type
	 * @param data (optional) any data associated with the event. If data implements Traceable, the object's getXML() 
	 * method will be called to write object specific information to the trace file.
	 * @see edu.illinois.mitra.starl.interfaces.Traceable 
	 */
	public synchronized void event(String source, String type, Object data, long timestamp) {
		open("event");
		writeTag("source", source);
		writeTimeTag(timestamp);
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
	
	/**
	 * Write a variable to the trace file
	 * @param source the thread in which the event occurred
	 * @param varname the name of the variable
	 * @param value the variable value. If this implements Traceable, the object's getXML() 
	 * method will be called to write object specific information to the trace file. If the variable
	 * object has no toString() method defined, this will not print useful information to the trace.
	 * @see edu.illinois.mitra.starl.interfaces.Traceable 
	 */
	public synchronized void variable(String source, String varname, Object value, long timestamp) {
		open("variable");
		writeTag("source", source);
		writeTimeTag(timestamp);
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
	
	/**
	 * Close the trace file. Must be called before exiting the application to ensure that the trace file is complete.
	 */
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
	
	/**
	 * @param text text to write to the trace file
	 */
	protected void write(String text) {
		try {
			for(int i = 0; i < level; i++) {
				buf.append("\t");
			}
			buf.append(text);
			buf.newLine();
			buf.flush();
		} catch(IOException e) {	
		} catch(NullPointerException e) {
		}
	}
	
	/**
	 * Write a complete XML tag to the trace file
	 * @param tag the tag label
	 * @param contents the tag contents
	 */
	protected void writeTag(String tag, String contents) {
		write("<" + tag + ">" + contents + "</" + tag + ">");
	}
	
	/**
	 * Writes an XML tag, time, containing a timestamp 
	 */
	protected void writeTimeTag(long timestamp) {
		writeTag("time", Long.toString(timestamp));
	}
	/**
	 * Writes an XML opening tag
	 * @param field the name of the tag to open
	 */
	protected void open(String field) {
		write("<" + field + ">");
		level ++;
	}
	/**
	 * Writes an XML closing tag
	 * @param field the name of the tag to close
	 */
	protected void close(String field) {
		level --;
		write("</" + field + ">");
	}
}
