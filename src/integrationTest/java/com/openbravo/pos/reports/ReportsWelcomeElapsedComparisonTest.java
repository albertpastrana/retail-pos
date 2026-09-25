package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class ReportsWelcomeElapsedComparisonTest {
	@Test
	public void tuesdayAndTheFifteenthExcludeTheRestOfLastYearWeekAndMonth() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:derby:memory:reportsElapsedIT;create=true");
				Statement sql = connection.createStatement()) {
			sql.executeUpdate("CREATE TABLE RECEIPTS (ID VARCHAR(40), DATENEW TIMESTAMP)");
			sql.executeUpdate("CREATE TABLE TICKETS (ID VARCHAR(40), TICKETTYPE INTEGER)");
			sql.executeUpdate("CREATE TABLE PAYMENTS (RECEIPT VARCHAR(40), TOTAL DOUBLE)");
			sql.executeUpdate("CREATE TABLE TAXLINES (RECEIPT VARCHAR(40), AMOUNT DOUBLE)");
			sql.executeUpdate("CREATE TABLE TICKETLINES (TICKET VARCHAR(40), UNITS DOUBLE, PRICE DOUBLE, "
					+ "LOYALTYREDEMPTION BOOLEAN)");
			sale(sql, "thisMonday", "2026-08-03", 10);
			sale(sql, "thisTuesday", "2026-08-04", 30);
			sale(sql, "lastMonday", "2025-08-04", 10);
			sale(sql, "lastTuesday", "2025-08-05", 20);
			sale(sql, "lastWednesday", "2025-08-06", 900);
			sale(sql, "thisFifteenth", "2026-08-15", 50);
			sale(sql, "lastFifteenth", "2025-08-15", 40);
			sale(sql, "lastSixteenth", "2025-08-16", 1000);

			SalesSummaryRepository repository = new SalesSummaryRepository();
			Date tuesday = date(2026, 8, 4);
			SalesSummaryComparison week = repository.loadComparison(connection,
					ReportsWelcomePeriod.WEEK.current(tuesday), ReportsWelcomePeriod.WEEK.previous(tuesday));
			assertEquals(40, week.getCurrent().getNetSales(), 0.001);
			assertEquals(30, week.getPrevious().getNetSales(), 0.001);
			assertEquals(33.333, week.netDeltaPercent(), 0.01);
			SalesTrend weekChart = new SalesTrendRepository().load(connection, ReportsWelcomePeriod.WEEK, tuesday);
			assertEquals(900, weekChart.previous[2], 0.001);
			assertFalse(weekChart.reached[2]);

			Date fifteenth = date(2026, 8, 15);
			SalesSummaryComparison month = repository.loadComparison(connection,
					ReportsWelcomePeriod.MONTH.current(fifteenth), ReportsWelcomePeriod.MONTH.previous(fifteenth));
			assertEquals(90, month.getCurrent().getNetSales(), 0.001);
			assertEquals(970, month.getPrevious().getNetSales(), 0.001);
			SalesTrend monthChart = new SalesTrendRepository().load(connection, ReportsWelcomePeriod.MONTH, fifteenth);
			assertEquals(1000, monthChart.previous[15], 0.001);
			assertFalse(monthChart.reached[15]);
		}
	}

	private void sale(Statement sql, String id, String day, double amount) throws Exception {
		sql.executeUpdate("INSERT INTO RECEIPTS VALUES ('" + id + "', '" + day + " 12:00:00')");
		sql.executeUpdate("INSERT INTO TICKETS VALUES ('" + id + "', 0)");
		sql.executeUpdate("INSERT INTO PAYMENTS VALUES ('" + id + "', " + amount + ")");
	}

	private Date date(int year, int month, int day) {
		return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
	}
}
