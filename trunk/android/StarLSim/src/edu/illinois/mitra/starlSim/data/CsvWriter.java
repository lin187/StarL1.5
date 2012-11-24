package edu.illinois.mitra.starlSim.data;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes data to a CSV file, one row at a time. Especially useful for saving results of simulations
 * @author Adam Zimmerman
 */
public class CsvWriter {
	private String[] columns;
	private FileWriter writer;

	public CsvWriter(String filename, String... columns) throws IOException {
		writer = new FileWriter(filename);
		this.columns = columns;

		for(int i = 0; i < columns.length - 1; i++)
			writer.append(columns[i] + ", ");
		writer.append(columns[columns.length - 1] + "\n");
	}

	public void commit(Object... row) {
		if(row.length != columns.length)
			throw new IllegalArgumentException("Must commit one entry for each column!");

		try {
			for(int i = 0; i < row.length - 1; i++)
				writer.append(row[i].toString() + ", ");
			writer.append(row[row.length - 1].toString() + "\n");
			writer.flush();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			writer.flush();
			writer.close();
		} catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
