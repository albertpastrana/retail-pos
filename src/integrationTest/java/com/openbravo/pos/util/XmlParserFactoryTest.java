package com.openbravo.pos.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import javax.xml.parsers.SAXParser;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlParserFactoryTest {

	@Test
	public void rejectsDocumentsWithExternalEntities() throws Exception {
		SAXParser parser = XmlParserFactory.newSecureSAXParser();
		String xml = "<!DOCTYPE root [<!ENTITY secret SYSTEM 'file:///etc/passwd'>]><root>&secret;</root>";

		assertThrows(SAXException.class,
				() -> parser.parse(new InputSource(new StringReader(xml)), new DefaultHandler()));
	}

	@Test
	public void parsesNormalXml() throws Exception {
		SAXParser parser = XmlParserFactory.newSecureSAXParser();

		assertDoesNotThrow(() -> parser.parse(new InputSource(new StringReader("<root><item>value</item></root>")),
				new DefaultHandler()));
	}
}
