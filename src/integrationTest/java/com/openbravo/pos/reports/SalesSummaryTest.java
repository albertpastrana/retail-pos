package com.openbravo.pos.reports;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import org.junit.Test;

public class SalesSummaryTest {

	@Test
	public void calculatesNetAndAverageFromNamedValues() {
		SalesSummary summary = new SalesSummary(4, 100.0, -20.0, 16.0);

		assertEquals(80.0, summary.getNetSales(), 0.000001);
		assertEquals(20.0, summary.getAverageReceipt(), 0.000001);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsAnEmptyPeriod() {
		Date date = new Date();
		new SalesSummaryParameters(date, date);
	}
}
