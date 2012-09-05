package edu.illinois.mitra.lightpaint.utility;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;

public class SvgParser extends XmlReader {

	public SvgParser() {
		super(false);
	}

	private boolean centering = false;
	private ImagePoint center;

	public void enableCentering(int centerX, int centerY) {
		centering = true;
		center = new ImagePoint(centerX, centerY);
	}

	private boolean scaling = false;
	private ImagePoint scale;

	public void enableScaling(int maxSizeX, int maxSizeY) {
		scaling = true;
		scale = new ImagePoint(maxSizeX, maxSizeY);
	}

	public Set<ImageEdge> parseImage(String filename) {
		try {
			super.buildDocument(new FileInputStream(filename));
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		Set<ImageEdge> retval = new HashSet<ImageEdge>();

		// Isolate all line nodes
		NodeList lines = super.getExpression("//line");
		for(int i = 0; i < lines.getLength(); i++) {
			Node line = lines.item(i);
			NamedNodeMap attrib = line.getAttributes();
			int x1 = (int) Double.parseDouble(attrib.getNamedItem("x1").getNodeValue());
			int x2 = (int) Double.parseDouble(attrib.getNamedItem("x2").getNodeValue());
			int y1 = (int) Double.parseDouble(attrib.getNamedItem("y1").getNodeValue());
			int y2 = (int) Double.parseDouble(attrib.getNamedItem("y2").getNodeValue());

			retval.add(new ImageEdge(new ImagePoint(x1, y1), new ImagePoint(x2, y2)));
		}

		NodeList paths = super.getExpression("//path");
		for(int i = 0; i < paths.getLength(); i++) {
			Node path = paths.item(i);
			String pathString = path.getAttributes().getNamedItem("d").getNodeValue();
			retval.addAll(pathToLines(pathString));
		}

		if(scaling || centering)
			retval = scaleAndCenterEdges(retval);

		return retval;
	}

	private static final String[] INVALID_PATH_COMMANDS = { "c", "C", "s", "S", "q", "Q", "t", "T", "a", "A" };
	private static final Pattern PATH_PATTERN = Pattern.compile("[a-zA-Z]([\\s,]?[-]?[0-9]+[\\s,]?[-]?[0-9]+[\\s,]?){1,}");
	private static final Set<ImageEdge> EMPTY_EDGE_SET = new HashSet<ImageEdge>();

	private Set<ImageEdge> pathToLines(String pathString) {
		// Break the string into commands first
		// Must split along any character, preserving which command the
		// character is placed in
		// For example "m50,100L60,200" -> "m50,100" and "L60,200"

		for(String invalid : INVALID_PATH_COMMANDS) {
			if(pathString.contains(invalid)) {
				System.err.println("Image contains a path with curves! Skipping.");
				return EMPTY_EDGE_SET;
			}
		}

		List<String> commands = new ArrayList<String>(pathString.length() / 4);

		Matcher matcher = PATH_PATTERN.matcher(pathString);
		int idx = 0;
		while(matcher.find(idx)) {
			String cmd = pathString.substring(matcher.start(), matcher.end());
			commands.add(cmd);
			idx = matcher.end();
		}

		Set<ImageEdge> retval = new HashSet<ImageEdge>();

		// Step through the commands
		ImagePoint current = new ImagePoint(0, 0);
		for(String commandString : commands) {
			char cmdType = commandString.charAt(0);
			ImagePoint cmdCoordinates = ImagePoint.fromString(commandString.substring(1));
			switch(cmdType) {
			case 'm':
				current = current.add(cmdCoordinates);
				break;
			case 'M':
				current = cmdCoordinates;
				break;
			case 'l':
				retval.add(new ImageEdge(current, current = current.add(cmdCoordinates)));
				break;
			case 'L':
				retval.add(new ImageEdge(current, current = cmdCoordinates));
				break;
			}
		}

		return retval;
	}

	private Set<ImageEdge> scaleAndCenterEdges(Set<ImageEdge> edges) {
		if(!scaling && !centering)
			return edges;

		// Find X range and Y range
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

		for(ImageEdge edge : edges) {
			for(ImagePoint point : edge.getEnds()) {
				minX = Math.min(minX, point.getX());
				maxX = Math.max(maxX, point.getX());
				minY = Math.min(minY, point.getY());
				maxY = Math.max(maxY, point.getY());
			}
		}

		double width = maxX - minX;
		double height = maxY - minY;

		Set<ImageEdge> retval = new HashSet<ImageEdge>();

		if(centering) {
			double centerX = minX + width / 2;
			double centerY = minY + height / 2;
			ImagePoint offset = center.subtract(new ImagePoint(centerX, centerY));

			// Don't bother centering if we're moving by +/- 10 pixels or less
			if(Math.hypot(offset.getX(), offset.getY()) > 10) {
				System.out.println("Translating by " + offset);

				Set<ImageEdge> centered = new HashSet<ImageEdge>();
				for(ImageEdge edge : retval)
					centered.add(edge.translate(offset));

				retval = centered;

				maxX += centerX;
				maxY += centerY;
			}
		}

		if(scaling) {
			double scalefactor = Math.min(scale.getX() / maxX, scale.getY() / maxY);
			// Skip scaling if we're only scaling by +/- 5% or less
			if(Utility.inRange(scalefactor, 0.95, 1.05))
				retval.addAll(edges);
			else {
				System.out.println("Scaling by " + scalefactor);
				for(ImageEdge edge : edges)
					retval.add(edge.scale(scalefactor));
			}
		} else {
			retval.addAll(edges);
		}

		return retval;
	}
}