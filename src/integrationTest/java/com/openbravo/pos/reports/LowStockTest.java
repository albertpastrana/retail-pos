package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LowStockTest {

	@Test
	public void calculatesUnitsToOrderUpToMaximum() {
		LowStockRow row = new LowStockRow("SKU-1", "Coffee", "Drinks", "General", 2.0, 5.0, 10.0);

		assertEquals(8.0, row.getUnitsToOrder(), 0.000001);
	}
}
