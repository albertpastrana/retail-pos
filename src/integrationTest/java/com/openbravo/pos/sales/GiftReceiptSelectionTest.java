package com.openbravo.pos.sales;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.LoyaltyStamps;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;

public class GiftReceiptSelectionTest {

	private static final TaxInfo TAX = new TaxInfo("001", "IVA", "001", null, null, null, 0.21, false, null);

	@Test
	public void selectedLinesAreCopiedWithoutChangingOriginalTicket() {
		TicketInfo original = new TicketInfo();
		original.addLine(new TicketLineInfo("shirt", "Shirt", "001", 1.0, 20.0, TAX));
		original.addLine(new TicketLineInfo("shoes", "Shoes", "001", 1.0, 40.0, TAX));

		TicketInfo giftTicket = JTicketsBagTicket.giftReceiptTicket(original, Set.of(1));

		assertEquals(2, original.getLinesCount());
		assertEquals(1, giftTicket.getLinesCount());
		assertEquals("Shoes", giftTicket.getLine(0).getProductName());
	}

	@Test
	public void victorinesRedemptionIsNeverIncluded() {
		TicketInfo original = new TicketInfo();
		original.addLine(new TicketLineInfo("shirt", "Shirt", "001", 1.0, 20.0, TAX));
		TicketLineInfo redemption = new TicketLineInfo("redemption", "Victorines", "001", 1.0, -5.0, TAX);
		LoyaltyStamps.markRedemption(redemption);
		original.addLine(redemption);
		original.addLine(new TicketLineInfo("shoes", "Shoes", "001", 1.0, 40.0, TAX));

		TicketInfo giftTicket = JTicketsBagTicket.giftReceiptTicket(original, Set.of(0, 1, 2));

		assertEquals(3, original.getLinesCount());
		assertEquals(2, giftTicket.getLinesCount());
		assertEquals("Shirt", giftTicket.getLine(0).getProductName());
		assertEquals("Shoes", giftTicket.getLine(1).getProductName());
	}

	@Test
	public void longGiftReceiptLabelsAreTruncated() {
		String label = "A".repeat(100);

		String truncated = JTicketsBagTicket.truncateGiftReceiptLabel(label);

		assertEquals(72, truncated.length());
		assertEquals("...", truncated.substring(69));
	}
}
