package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class WelcomeSalesRepositoryTest {

	@Test
	public void categoryWithSalesOnlyLastYearIsNotComparable() throws Exception {
		try (Connection connection = connection()) {
			createSchema(connection);
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate(
						"INSERT INTO CATEGORIES VALUES ('current', 'Current', NULL), ('old', 'Old', NULL)");
				statement.executeUpdate(
						"INSERT INTO PRODUCTS VALUES ('p-current', 'C-1', 'Current product', 'current', 'Brand'),"
								+ " ('p-old', 'O-1', 'Old product', 'old', 'Brand')");
				statement.executeUpdate("INSERT INTO TICKETS VALUES ('now', 0), ('last', 0)");
				statement.executeUpdate(
						"INSERT INTO RECEIPTS VALUES ('now', '2026-10-10 12:00:00'), ('last', '2025-10-10 12:00:00')");
				statement.executeUpdate(
						"INSERT INTO TICKETLINES VALUES ('now', 'p-current', 1, 100), ('last', 'p-current', 1, 80),"
								+ " ('last', 'p-old', 1, 50)");
				statement.executeUpdate("INSERT INTO PAYMENTS VALUES ('now', 'cash', 100), ('last', 'cash', 80)");
			}

			WelcomeSalesRepository.WelcomeSalesData data = load(connection);
			WelcomeSalesRepository.CategoryInsight old = data.getCategories().stream()
					.filter(category -> "Old".equals(category.getName())).findFirst().orElseThrow();
			assertEquals(0, old.getAmount(), 0.000001);
			assertEquals(50, old.getPrevious(), 0.000001);
			assertFalse(old.hasComparablePeriods());
		}
	}

	@Test
	public void refundKeepsItsTenderClassification() throws Exception {
		assertEquals(PaymentSalesRepository.CASH, PaymentSalesRepository.classify("cashrefund"));
		assertEquals(PaymentSalesRepository.CARD, PaymentSalesRepository.classify("magcardrefund"));

		try (Connection connection = connection()) {
			createSchema(connection);
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("INSERT INTO TICKETS VALUES ('sale', 0), ('refund', 1)");
				statement.executeUpdate(
						"INSERT INTO RECEIPTS VALUES ('sale', '2026-10-10 12:00:00'), ('refund', '2025-10-10 12:00:00')");
				statement.executeUpdate(
						"INSERT INTO PAYMENTS VALUES ('sale', 'cash', 100), ('refund', 'cashrefund', -20)");
			}
			WelcomeSalesRepository.WelcomeSalesData data = load(connection);
			WelcomeSalesRepository.PaymentInsight cash = data.getPayments().stream()
					.filter(payment -> PaymentSalesRepository.CASH.equals(payment.getCategory())).findFirst()
					.orElseThrow();
			WelcomeSalesRepository.PaymentInsight other = data.getPayments().stream()
					.filter(payment -> PaymentSalesRepository.OTHER.equals(payment.getCategory())).findFirst()
					.orElseThrow();
			assertEquals(100, cash.getAmount(), 0.000001);
			assertEquals(-20, cash.getPrevious(), 0.000001);
			assertEquals(0, other.getAmount(), 0.000001);
			assertEquals(0, other.getPrevious(), 0.000001);
			assertTrue(data.getPayments().size() >= 2);
		}
	}

	private static WelcomeSalesRepository.WelcomeSalesData load(Connection connection) throws Exception {
		Date currentStart = date(2026, 10, 1);
		Date currentEnd = date(2026, 11, 1);
		Date previousStart = date(2025, 10, 1);
		Date previousEnd = date(2025, 11, 1);
		return new WelcomeSalesRepository().load(connection, new SalesSummaryParameters(currentStart, currentEnd),
				new SalesSummaryParameters(previousStart, previousEnd));
	}

	private static Connection connection() throws Exception {
		return DriverManager.getConnection("jdbc:derby:memory:welcomeSales" + UUID.randomUUID() + ";create=true");
	}

	private static Date date(int year, int month, int day) {
		return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private static void createSchema(Connection connection) throws Exception {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(
					"CREATE TABLE CATEGORIES (ID VARCHAR(255), NAME VARCHAR(255), PARENTID VARCHAR(255))");
			statement.executeUpdate(
					"CREATE TABLE PRODUCTS (ID VARCHAR(255), REFERENCE VARCHAR(255), NAME VARCHAR(255), CATEGORY VARCHAR(255), BRAND VARCHAR(255))");
			statement.executeUpdate(
					"CREATE TABLE TICKETLINES (TICKET VARCHAR(255), PRODUCT VARCHAR(255), UNITS DOUBLE, PRICE DOUBLE)");
			statement.executeUpdate("CREATE TABLE TICKETS (ID VARCHAR(255), TICKETTYPE INTEGER)");
			statement.executeUpdate("CREATE TABLE RECEIPTS (ID VARCHAR(255), DATENEW TIMESTAMP)");
			statement.executeUpdate("CREATE TABLE PAYMENTS (RECEIPT VARCHAR(255), PAYMENT VARCHAR(255), TOTAL DOUBLE)");
		}
	}
}
