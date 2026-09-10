package com.openbravo.pos.ticket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
