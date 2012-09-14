/*
 * Copyright (c) 2012, Willow Garage, Inc.
 * All rights reserved.
 *
 * Willow Garage licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.illinois.mitra.lightpaint.utility;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class XmlReader {

	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private DocumentBuilder builder;
	protected final XPath xpath = XPathFactory.newInstance().newXPath();
	private Document doc = null;

	protected NodeList existResults;

	protected XmlReader(boolean namespaces) {
		factory.setNamespaceAware(namespaces);
		try {
			builder = factory.newDocumentBuilder();
		} catch(ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param contents
	 *            The contents of the XML file in String form
	 */
	protected void buildDocument(InputStream file) {
		try {
			doc = builder.parse(file);
		} catch(SAXException e1) {
			e1.printStackTrace();
		} catch(IOException e1) {
			e1.printStackTrace();
		}
	}

	protected boolean nodeExists(String... xPathExpression) {
		existResults = getExpression(xPathExpression);
		return existResults.getLength() > 0;
	}

	protected Node getSingleNode(String... xPathExpression) {
		NodeList nl = getExpression(xPathExpression);
		if(nl.getLength() > 1)
			throw new IllegalArgumentException("Expression returned multiple results!");
		return nl.item(0);
	}

	protected NodeList getExpression(String... xPathExpression) {
		try {
			//XPathExpression expr = xpath.compile(Compose(xPathExpression));	
			return (NodeList) xpath.evaluate(Compose(xPathExpression), doc, XPathConstants.NODESET);//expr.evaluate(doc, XPathConstants.NODESET);
		} catch(XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected String Compose(String... pieces) {
		if(pieces.length == 1)
			return pieces[0];

		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < pieces.length; i++) {
			sb.append(pieces[i]);
			if(i < pieces.length - 1) {
				sb.append("/");
			}
		}
		return sb.toString();
	}

	protected short[] toShortArray(String str) {
		String[] pieces = str.split(" ");
		short[] retval = new short[pieces.length];
		for(int i = 0; i < pieces.length; i++) {
			retval[i] = Short.parseShort(pieces[i]);
		}
		return retval;
	}

	protected float[] toFloatArray(String str) {
		String[] pieces = str.split(" ");
		float[] retval = new float[pieces.length];
		for(int i = 0; i < pieces.length; i++) {
			retval[i] = Float.parseFloat(pieces[i]);
		}
		return retval;
	}
}