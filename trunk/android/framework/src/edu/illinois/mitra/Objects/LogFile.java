package edu.illinois.mitra.Objects;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.illinois.mitra.comms.UDPMessage;

public class LogFile {
	File logFile;
	BufferedWriter buf;
	
	public LogFile(String fname) {
		// Create the log file
		logFile = new File("/sdcard/" + fname);
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
			buf.append("Sequence #, Direction, Type, From, To, # Retries, MID");
			buf.newLine();
			buf.flush();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(String text) {
		try {
			buf.append(text);
			buf.newLine();
			buf.flush();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(UDPMessage msg, Boolean sent) {
		String type = msg.isACK() ? "ACK" : "DATA";
		
		if(sent) {
			write(msg.getSeqNum() + ", SENT, " + type + ", " + msg.getContents().getFrom() + ", " + msg.getContents().getTo() + ", " + msg.getRetries() + ", " + msg.getContents().getMID());
		} else {
			write(msg.getSeqNum() + ", RECEIVED, " + type + ", " + msg.getContents().getFrom() + ", " + msg.getContents().getTo() + ", " + msg.getRetries() + ", " + msg.getContents().getMID());
		}		
	}
	
	public void close() {
		try {
			buf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
