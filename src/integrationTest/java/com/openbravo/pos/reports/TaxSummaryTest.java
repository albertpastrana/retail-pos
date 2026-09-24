package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TaxSummaryTest {

	@Test
	public void keepsRefundsNegativeAndCalculatesTotal() {
		TaxSummaryRow row = new TaxSummaryRow("Standard", 0.21, -100.0, -21.0);

		assertEquals("Standard", row.getTaxName());
		assertEquals(0.21, row.getTaxRate(), 0.000001);
		assertEquals(-100.0, row.getTaxableBase(), 0.000001);
		assertEquals(-21.0, row.getTaxAmount(), 0.000001);
		assertEquals(-121.0, row.getTotal(), 0.000001);
	}
}
