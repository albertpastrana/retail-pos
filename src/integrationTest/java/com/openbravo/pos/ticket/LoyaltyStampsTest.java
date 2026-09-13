package com.openbravo.pos.ticket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LoyaltyStampsTest {

	@Test
	public void floorsEligibleSpendAtTenEurosPerStamp() {
		TicketInfo ticket = ticket(line("Pijama", 27.0));
		assertEquals(27.0, LoyaltyStamps.eligibleEuros(ticket), 0.0001);
		assertEquals(2, LoyaltyStamps.stampsEarned(ticket));
	}

	@Test
	public void ignoresSaleAndLineDiscountAndVoucherAndRedemption() {
		TicketLineInfo sale = line("Rebaixat", 20.0);
		LineDiscount.applyPercent(sale, 20.0);

		TicketLineInfo voucher = line("Val", 50.0);
		voucher.setProperty("product.voucher", "true");

		TicketLineInfo totalOff = line("Descompte total 10%", -5.0);
		totalOff.setProperty("discount.scope", "total");

		TicketLineInfo redeem = line("Descompte fidelització", -5.0);
		LoyaltyStamps.markRedemption(redeem);

		TicketInfo ticket = ticket(line("Normal", 10.0), sale, voucher, totalOff, redeem);
		assertEquals(1, LoyaltyStamps.stampsEarned(ticket));
		assertEquals(5.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
	}

	@Test
	public void genericFiveEuroNegativeLineIsNotLoyaltySavings() {
		TicketInfo ticket = ticket(line("Normal", 12.0), line("", -5.0));
		assertEquals(1, LoyaltyStamps.stampsEarned(ticket));
		assertEquals(0.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
		assertFalse(ticket.hasLoyaltySavings());
	}

	@Test
	public void totalDiscountIsNotLoyaltySavings() {
		TicketLineInfo iva = line("Descompte total 6%", -3.10);
		iva.setProperty("discount.scope", "total");
		TicketLineInfo reduced = line("Descompte total 6%", -1.90);
		reduced.setProperty("discount.scope", "total");
		TicketInfo ticket = ticket(line("Normal", 40.0), iva, reduced);
		assertEquals(4, LoyaltyStamps.stampsEarned(ticket));
		assertEquals(0.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
		assertFalse(ticket.hasLoyaltySavings());
	}

	@Test
	public void markedRedemptionUsesConfiguredStampName() {
		TicketLineInfo redemption = line("Descompte Segells", -5.0);
		LoyaltyStamps.markRedemption(redemption);
		TicketInfo ticket = ticket(line("Normal", 12.0), redemption);
		LoyaltyStamps.applyToTicket(ticket, "true", "Segells");
		assertEquals(1, LoyaltyStamps.stampsEarned(ticket));
		assertEquals(5.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
		assertTrue(LoyaltyStamps.isRedemption(redemption));
	}

	@Test
	public void missingConfigKeepsLoyaltyOnWithDefaultName() {
		assertTrue(LoyaltyStamps.isEnabled(null));
		assertEquals("victorines", LoyaltyStamps.name(""));
		assertFalse(LoyaltyStamps.isEnabled("false"));
	}

	@Test
	public void reprintingKeepsTheLoyaltyNameUsedWhenTheTicketWasCreated() {
		TicketInfo ticket = ticket(line("Normal", 20.0));
		LoyaltyStamps.applyToTicket(ticket, "true", "Nom original");
		LoyaltyStamps.applyToTicket(ticket, "true", "Segells");
		assertEquals("Nom original", ticket.getProperty(LoyaltyStamps.NAME_KEY));
	}

	@Test
	public void disabledLoyaltyPrintsNeitherStampsNorSavings() {
		TicketInfo ticket = ticket(line("Normal", 40.0), line("", -5.0));
		LoyaltyStamps.applyToTicket(ticket, "false", "segells");
		assertFalse(ticket.isLoyaltyEnabled());
		assertEquals(0, ticket.getLoyaltyStampsEarned());
		assertFalse(ticket.hasLoyaltySavings());
	}

	@Test
	public void addsUpSeveralRedemptionsOnTheSameReceipt() {
		TicketLineInfo first = line("Descompte fidelització", -5.0);
		LoyaltyStamps.markRedemption(first);
		TicketLineInfo second = line("Descompte fidelització", -5.0);
		LoyaltyStamps.markRedemption(second);
		TicketInfo ticket = ticket(line("Normal", 40.0), first, second);
		assertEquals(4, LoyaltyStamps.stampsEarned(ticket));
		assertEquals(10.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
		assertEquals("10€", ticket.printLoyaltySavings());
	}

	@Test
	public void printsSavingsAsWholeEurosWithSymbolAfter() {
		TicketLineInfo redemption = line("Descompte fidelització", -5.0);
		LoyaltyStamps.markRedemption(redemption);
		TicketInfo ticket = ticket(line("Normal", 12.0), redemption);
		LoyaltyStamps.applyToTicket(ticket, "true", "victorines");
		assertEquals(5, ticket.getLoyaltySavingsEuros());
		assertEquals("5€", ticket.printLoyaltySavings());
	}

	@Test
	public void doesNotCountARandomTotalDiscountAsLoyaltySavings() {
		TicketLineInfo off = line("Descompte total 10%", -8.0);
		off.setProperty("discount.scope", "total");
		TicketInfo ticket = ticket(line("Normal", 80.0), off);
		assertEquals(0.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
		assertFalse(ticket.hasLoyaltySavings());
	}

	private static TicketInfo ticket(TicketLineInfo... lines) {
		TicketInfo ticket = new TicketInfo();
		for (TicketLineInfo line : lines) {
			ticket.addLine(line);
		}
		return ticket;
	}

	private static TicketLineInfo line(String name, double value) {
		return new TicketLineInfo(name, "001", 1.0, value, null);
	}
}
