package com.openbravo.pos.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Properties;

public class LoyaltyStampsTest {

	@Test
	public void floorsEligibleSpendAtTenEurosPerStamp() {
		TicketInfo ticket = ticket(line("Pijama", 27.0));
		assertEquals(27.0, LoyaltyStamps.eligibleEuros(ticket), 0.0001);
		assertEquals(2, LoyaltyStamps.stampsEarned(ticket));
	}

	@Test
	public void ignoresSaleAndLineDiscountAndVoucherButSubtractsRedemption() {
		TicketLineInfo sale = line("Rebaixat", 20.0);
		LineDiscount.applyPercent(sale, 20.0);

		TicketLineInfo voucher = line("Val", 50.0);
		voucher.setProperty("product.voucher", "true");

		TicketLineInfo redeem = line("Descompte fidelització", -5.0);
		LoyaltyStamps.markRedemption(redeem);

		TicketInfo ticket = ticket(line("Normal", 20.0), sale, voucher, redeem);
		assertEquals(15.0, LoyaltyStamps.eligibleEuros(ticket), 0.0001);
		assertEquals(1, LoyaltyStamps.stampsEarned(ticket));
		assertEquals(5.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
	}

	@Test
	public void redemptionComesOffTheEligibleSpend() {
		TicketLineInfo redemption = line("Descompte victorines", -5.0);
		LoyaltyStamps.markRedemption(redemption);
		TicketInfo ticket = ticket(line("Pijama", 82.0), redemption);
		assertEquals(77.0, LoyaltyStamps.eligibleEuros(ticket), 0.0001);
		assertEquals(7, LoyaltyStamps.stampsEarned(ticket));
	}

	@Test
	public void genericNegativeLineComesOffTheEligibleSpendWithoutBeingLoyaltySavings() {
		TicketInfo ticket = ticket(line("Normal", 12.0), line("", -5.0));
		assertEquals(7.0, LoyaltyStamps.eligibleEuros(ticket), 0.0001);
		assertEquals(0, LoyaltyStamps.stampsEarned(ticket));
		assertEquals(0.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
		assertFalse(ticket.hasLoyaltySavings());
	}

	@Test
	public void negativeLinesNeverPushTheEligibleSpendBelowZero() {
		TicketLineInfo discounted = line("Rebaixat", 30.0);
		LineDiscount.applyPercent(discounted, 20.0);
		TicketLineInfo redemption = line("Descompte victorines", -5.0);
		LoyaltyStamps.markRedemption(redemption);
		TicketInfo ticket = ticket(discounted, redemption);
		assertEquals(0.0, LoyaltyStamps.eligibleEuros(ticket), 0.0001);
		assertEquals(0, LoyaltyStamps.stampsEarned(ticket));
	}

	@Test
	public void totalDiscountMakesTheWholeReceiptIneligible() {
		TicketLineInfo iva = line("Descompte total 6%", -3.10);
		iva.setProperty("discount.scope", "total");
		TicketLineInfo reduced = line("Descompte total 6%", -1.90);
		reduced.setProperty("discount.scope", "total");
		TicketInfo ticket = ticket(line("Normal", 40.0), iva, reduced);
		assertTrue(LoyaltyStamps.hasTotalDiscount(ticket));
		assertEquals(0, LoyaltyStamps.stampsEarned(ticket));
		assertEquals(0.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
		assertFalse(ticket.hasLoyaltySavings());
	}

	@Test
	public void payingWithAGiftVoucherStillEarnsStampsOnTheGoods() {
		TicketLineInfo voucher = line("Val", -50.0);
		voucher.setProperty("product.voucher", "true");
		TicketInfo ticket = ticket(line("Pijama", 82.0), voucher);
		assertEquals(82.0, LoyaltyStamps.eligibleEuros(ticket), 0.0001);
		assertEquals(8, LoyaltyStamps.stampsEarned(ticket));
	}

	@Test
	public void markedRedemptionUsesConfiguredStampName() {
		TicketLineInfo redemption = line("Descompte Segells", -5.0);
		LoyaltyStamps.markRedemption(redemption);
		TicketInfo ticket = ticket(line("Normal", 22.0), redemption);
		LoyaltyStamps.applyToTicket(ticket, "true", "Segells");
		assertEquals(1, LoyaltyStamps.stampsEarned(ticket));
		assertEquals(5.0, LoyaltyStamps.savingsEuros(ticket), 0.0001);
		assertTrue(LoyaltyStamps.isRedemption(redemption));
	}

	@Test
	public void missingConfigKeepsLoyaltyOnWithDefaultName() {
		assertFalse(LoyaltyStamps.isEnabled(null));
		assertEquals("", LoyaltyStamps.name(""));
		assertFalse(LoyaltyStamps.isEnabled("false"));
	}

	@Test
	public void databaseConfigOverridesLocalConfig() {
		Properties runtime = new Properties();
		runtime.setProperty(LoyaltyStamps.ENABLED_KEY, "true");
		runtime.setProperty(LoyaltyStamps.NAME_KEY, "Local");
		Properties database = new Properties();
		database.setProperty(LoyaltyStamps.ENABLED_KEY, "true");
		database.setProperty(LoyaltyStamps.NAME_KEY, "Segells");

		LoyaltyConfiguration.apply(runtime, database);
		assertEquals("true", runtime.getProperty(LoyaltyStamps.ENABLED_KEY));
		assertEquals("Segells", runtime.getProperty(LoyaltyStamps.NAME_KEY));
	}

	@Test
	public void missingDatabaseConfigDisablesLoyalty() {
		Properties runtime = new Properties();
		runtime.setProperty(LoyaltyStamps.ENABLED_KEY, "true");
		runtime.setProperty(LoyaltyStamps.NAME_KEY, "Segells");
		Properties database = new Properties();

		LoyaltyConfiguration.apply(runtime, database);
		assertFalse(LoyaltyStamps.isEnabled(runtime.getProperty(LoyaltyStamps.ENABLED_KEY)));
		assertEquals("", runtime.getProperty(LoyaltyStamps.NAME_KEY));
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
		assertEquals(3, LoyaltyStamps.stampsEarned(ticket));
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
