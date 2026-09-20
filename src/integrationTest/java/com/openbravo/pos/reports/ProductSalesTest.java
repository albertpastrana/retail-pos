package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ProductSalesTest {

	@Test
	public void keepsRefundsAsNegativeProductSales() {
		ProductSalesRow row = new ProductSalesRow("SKU-1", "Coffee", "Drinks", -1.0, -2.5);

		assertEquals("SKU-1", row.getReference());
		assertEquals(-1.0, row.getUnits(), 0.000001);
		assertEquals(-2.5, row.getAmount(), 0.000001);
	}
}
