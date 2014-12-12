package com.digero.maestro.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractList;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlUtil
{
	private XmlUtil()
	{
	}

	//
	// Document
	//

	public static Document createDocument()
	{
		try
		{
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException e)
		{
			// How can a vanilla instance throw a configuration exception?
			e.printStackTrace();
			assert false : e.getMessage();
			throw new RuntimeException(e);
		}
	}

	public static Document openDocument(String file) throws SAXException, IOException
	{
		return openDocument(new File(file));
	}

	public static Document openDocument(File file) throws SAXException, IOException
	{
		try (FileInputStream stream = new FileInputStream(file))
		{
			Document doc = openDocument(stream);
			doc.setUserData(DOCUMENT_FILE_USERDATA, file, null);
			return doc;
		}
	}

	public static Document openDocument(InputStream stream) throws SAXException, IOException
	{
		try
		{
			LineNumberHandler handler = new LineNumberHandler();
			SAXParserFactory.newInstance().newSAXParser().parse(stream, handler);
			return handler.getDocument();
		}
		catch (ParserConfigurationException e)
		{
			// How can a vanilla instance throw a configuration exception?
			e.printStackTrace();
			assert false : e.getMessage();
			throw new RuntimeException(e);
		}
	}

	public static void saveDocument(Document document, String file) throws TransformerException, IOException
	{
		saveDocument(document, new File(file));
	}

	public static void saveDocument(Document document, File file) throws TransformerException, IOException
	{
		try (FileOutputStream stream = new FileOutputStream(file))
		{
			saveDocument(document, stream);
		}
	}

	public static void saveDocument(Document document, OutputStream stream) throws TransformerException, IOException
	{
		try
		{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(new DOMSource(document), new StreamResult(stream));
		}
		catch (TransformerConfigurationException e)
		{
			// How can a vanilla instance throw a configuration exception?
			e.printStackTrace();
			assert false : e.getMessage();
			throw new RuntimeException(e);
		}
		catch (TransformerFactoryConfigurationError e)
		{
			// How can a vanilla instance throw a configuration exception?
			e.printStackTrace();
			assert false : e.getMessage();
			throw new RuntimeException(e);
		}
	}

	public static String DOCUMENT_FILE_USERDATA = XmlUtil.class.getName() + ".DOCUMENT_FILE";
	public static String LINE_NUMBER_USERDATA = XmlUtil.class.getName() + ".LINE_NUMBER";

	private static class LineNumberHandler extends DefaultHandler
	{
		private Document doc = null;
		private Stack<Node> stack = new Stack<Node>();
		private StringBuilder text = new StringBuilder();
		private Locator locator = null;

		public Document getDocument()
		{
			return doc;
		}

		private void appendText()
		{
			if (text.length() > 0)
				stack.peek().appendChild(doc.createTextNode(text.toString()));
			text.setLength(0);
		}

		@Override public void startDocument() throws SAXException
		{
			doc = createDocument();
			stack.push(doc);
		}

		@Override public void endDocument() throws SAXException
		{
			stack.pop();
		}

		@Override public void setDocumentLocator(Locator locator)
		{
			this.locator = locator;
		}

		@Override public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException
		{
			appendText();
			Element ele = doc.createElement(qName);
			stack.push(ele);
			if (locator != null)
				ele.setUserData(LINE_NUMBER_USERDATA, locator.getLineNumber(), null);
			for (int i = 0; i < attributes.getLength(); i++)
				ele.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}

		@Override public void endElement(String uri, String localName, String qName) throws SAXException
		{
			appendText();
			Node node = stack.pop();
			stack.peek().appendChild(node);
		}

		@Override public void characters(char[] ch, int start, int length) throws SAXException
		{
			text.append(ch, start, length);
		}
	}

	public static int getLineNumber(Node node)
	{
		if (node instanceof Attr)
			node = ((Attr) node).getOwnerElement();

		Object data = node.getUserData(LINE_NUMBER_USERDATA);
		if (data instanceof Integer)
			return (Integer) data;

		return -1;
	}

	public static File getDocumentFile(Document doc)
	{
		Object data = doc.getUserData(DOCUMENT_FILE_USERDATA);
		if (data instanceof File)
			return (File) data;

		return null;
	}

	//
	// NodeList
	//

	public static class NodeListWrapper<TNode extends Node> extends AbstractList<TNode>
	{
		private final NodeList nodeList;

		public NodeListWrapper(NodeList nodeList)
		{
			this.nodeList = nodeList;
		}

		public NodeList getNodeList()
		{
			return nodeList;
		}

		@Override public int size()
		{
			return nodeList.getLength();
		}

		@SuppressWarnings("unchecked")//
		@Override public TNode get(int index)
		{
			return (TNode) nodeList.item(index);
		}
	}

	//
	// XPath
	//

	private static XPath xpath = XPathFactory.newInstance().newXPath();

	public static Node selectSingleNode(Node fromNode, String xpathString) throws XPathExpressionException
	{
		return (Node) xpath.evaluate(xpathString, fromNode, XPathConstants.NODE);
	}

	public static Element selectSingleElement(Node fromNode, String xpathString) throws XPathExpressionException
	{
		return (Element) xpath.evaluate(xpathString, fromNode, XPathConstants.NODE);
	}

	public static NodeListWrapper<Node> selectNodes(Node fromNode, String xpathString) throws XPathExpressionException
	{
		return new NodeListWrapper<Node>((NodeList) xpath.evaluate(xpathString, fromNode, XPathConstants.NODESET));
	}

	public static NodeListWrapper<Element> selectElements(Node fromNode, String xpathString)
			throws XPathExpressionException
	{
		return new NodeListWrapper<Element>((NodeList) xpath.evaluate(xpathString, fromNode, XPathConstants.NODESET));
	}

	//
	// Exceptions
	//

	public static String formatException(SAXException e)
	{
		String msg = e.getMessage();
		if (e instanceof SAXParseException)
		{
			SAXParseException e2 = (SAXParseException) e;
			if (e2.getLineNumber() >= 0)
			{
				msg += " (line " + e2.getLineNumber();
				if (e2.getColumnNumber() >= 0)
					msg += ", column " + e2.getColumnNumber();
				msg += ")";
			}
		}
		return msg;
	}

	public static String formatException(TransformerException e)
	{
		return e.getMessageAndLocation();
	}
}
