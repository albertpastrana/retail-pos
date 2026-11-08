package com.openbravo.pos.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.format.Formats;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.util.XmlParserFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.xml.sax.InputSource;
import org.junit.jupiter.api.Test;

public class LineDiscountTest {

	@Test
	public void appliesTwentyPercentToListPriceAndKeepsSign() {
		TicketLineInfo line = new TicketLineInfo("p1", "Pijama estiu", "001", 1.0, 19.95, null);
		LineDiscount.applyPercent(line, 20.0);
		assertEquals(15.96, line.getPrice(), 0.0001);
		assertTrue(line.getProductName().contains("(-"));
		assertEquals("19.95", line.getProperty("discount.line.baseprice"));
	}

	@Test
	public void putsDiscountBeforeTrailingReference() {
		assertEquals("Pijama (-20%) [3267-E]", LineDiscount.nameWithDiscount("Pijama [3267-E]", "20%"));
		assertEquals("Pijama (-20%)", LineDiscount.nameWithDiscount("Pijama", "20%"));
	}

	@Test
	public void onlyAutoAppliesWhenLinePriceIsTheCataloguePrice() {
		assertTrue(LineDiscount.shouldApplyCatalogSale(19.95, 19.95, 20.0));
		assertTrue(LineDiscount.shouldApplyCatalogSale(19.95, -19.95, 20.0));
		assertFalse(LineDiscount.shouldApplyCatalogSale(19.95, 10.0, 20.0));
		assertFalse(LineDiscount.shouldApplyCatalogSale(19.95, 19.95, 0.0));
	}

	@Test
	public void receiptShowsOriginalTaxIncludedPriceAndTotalSavingForMultipleUnits() {
		TaxInfo tax = new TaxInfo("001", "IVA 21%", "001", null, null, null, 0.21, false, null);
		TicketLineInfo line = new TicketLineInfo("p1", "Top & lace [REF]", "001", 2.0, 16.487603305785125, tax);
		LineDiscount.applyPercent(line, 10.0);

		assertTrue(line.hasLineDiscount());
		assertEquals("Top &amp; lace", line.printTicketBaseName());
		assertEquals("REF", line.printTicketReference());
		assertEquals("10%", line.printLineDiscountPercent());
		assertEquals(amount(19.95), line.printOriginalPriceTaxAmount());
		assertEquals(amount(35.91), line.printValueAmount());
		assertEquals(amount(-3.99), line.printLineDiscountAmount());
	}

	@Test
	public void receiptDiscountOnRefundReversesSavingsSign() {
		TicketLineInfo line = new TicketLineInfo("p1", "Returned top", "001", -1.0, 19.95, null);
		LineDiscount.applyPercent(line, 20.0);
		assertEquals(amount(3.99), line.printLineDiscountAmount());
		TicketLineInfo negativePrice = new TicketLineInfo("p1", "Returned top", "001", 1.0, -19.95, null);
		LineDiscount.applyPercent(negativePrice, 20.0);
		assertEquals(amount(3.99), negativePrice.printLineDiscountAmount());
	}

	@Test
	public void ordinaryAndTotalDiscountLinesHaveNoLineDiscountDetail() {
		TicketLineInfo ordinary = new TicketLineInfo("p1", "Top", "001", 1.0, 19.95, null);
		TicketLineInfo totalDiscount = new TicketLineInfo("Descompte total 10%", "001", 1.0, -1.995, null);
		totalDiscount.setProperty("discount.scope", "total");
		assertFalse(ordinary.hasLineDiscount());
		assertFalse(totalDiscount.hasLineDiscount());
		assertEquals(ordinary.printPriceTaxAmount(), ordinary.printOriginalPriceTaxAmount());
		assertEquals(totalDiscount.printPriceTaxAmount(), totalDiscount.printOriginalPriceTaxAmount());
	}

	@Test
	public void shippedPrintedAndPreviewTemplatesRenderDiscountWithinFortyTwoColumns() throws Exception {
		TaxInfo tax = new TaxInfo("001", "IVA 21%", "001", null, null, null, 0.21, false, null);
		TicketLineInfo discounted = new TicketLineInfo("p1", "TOP FOAM EXTRAIBLE [REF]", "001", 1.0, 16.487603305785125,
				tax);
		LineDiscount.applyPercent(discounted, 10.0);
		TicketInfo ticket = new TicketInfo();
		ticket.addLine(discounted);
		ticket.addLine(new TicketLineInfo("Descompte total 10%", "001", 1.0, -1.0, tax));
		for (String name : new String[]{"Printer.Ticket.xml", "Printer.TicketPreview.xml"}) {
			String template;
			try (InputStream resource = getClass().getResourceAsStream("/com/openbravo/pos/templates/" + name)) {
				template = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
			}
			var script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
			script.put("ticket", ticket);
			String receipt = script.eval(template).toString();
			assertTrue(receipt.contains("TOP FOAM EXTRAIBLE</text>"), name);
			assertTrue(receipt.contains("length=\"7\">" + discounted.printOriginalPriceTaxAmount()), name);
			assertTrue(receipt.contains("length=\"7\">" + discounted.printValueAmount()), name);
			assertTrue(receipt.contains("length=\"35\">Descompte aplicat (-10%)</text>"), name);
			assertTrue(receipt.contains("length=\"7\">" + discounted.printLineDiscountAmount()), name);
			assertEquals(1, receipt.split("Descompte aplicat", -1).length - 1, name);
			XmlParserFactory.newSecureSAXParser().parse(new InputSource(new java.io.StringReader(receipt)),
					new org.xml.sax.helpers.DefaultHandler());
		}
	}

	private static String amount(double value) {
		return Formats.CURRENCY.formatValue(value).replace("€", "").replace("EUR", "").replace('\u00a4', ' ').trim();
	}
}
