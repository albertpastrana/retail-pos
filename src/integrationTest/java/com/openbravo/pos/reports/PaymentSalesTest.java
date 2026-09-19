package com.openbravo.pos.reports;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PaymentSalesTest {

	@Test
	public void calculatesNetPaymentAmount() {
		PaymentSalesRow row = new PaymentSalesRow("cash", 100.0, -15.0);

		assertEquals(85.0, row.getNet(), 0.000001);
	}
}
