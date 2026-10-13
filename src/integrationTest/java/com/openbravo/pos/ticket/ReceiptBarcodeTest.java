package com.openbravo.pos.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ReceiptBarcodeTest {

	@Test
	public void encodesSaleAndRefundWithTheirReceiptType() {
		TicketInfo sale = new TicketInfo();
		sale.setTicketId(123);
		assertEquals("T123", ReceiptBarcode.encode(sale));

		TicketInfo refund = new TicketInfo();
		refund.setTicketType(TicketInfo.RECEIPT_REFUND);
		refund.setTicketId(456);
		assertEquals("R456", ReceiptBarcode.encode(refund));
	}

	@Test
	public void decodesOnlyPrefixedNumericReceiptCodes() {
		assertTrue(ReceiptBarcode.isReceiptBarcode("T123"));
		assertEquals(TicketInfo.RECEIPT_NORMAL, ReceiptBarcode.type("T123"));
		assertEquals(123, ReceiptBarcode.ticketId("T123"));
		assertEquals(TicketInfo.RECEIPT_REFUND, ReceiptBarcode.type("R456"));
		assertEquals(456, ReceiptBarcode.ticketId("R456"));
		assertFalse(ReceiptBarcode.isReceiptBarcode("123"));
		assertFalse(ReceiptBarcode.isReceiptBarcode("T12A"));
	}
}
