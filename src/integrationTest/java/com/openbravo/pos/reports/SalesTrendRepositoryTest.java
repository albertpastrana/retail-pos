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
import java.util.Locale;
import com.openbravo.pos.forms.AppLocal;
import org.junit.jupiter.api.Test;

public class SalesTrendRepositoryTest {
	@Test
	public void splitsCurrentMonthLastYearIntoElapsedAndFullTotalsForMonthlyCharts() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:derby:memory:reportsMonthlySplitIT;create=true");
				Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE RECEIPTS (ID VARCHAR(40), DATENEW TIMESTAMP)");
			statement.executeUpdate("CREATE TABLE TICKETS (ID VARCHAR(40), TICKETTYPE INTEGER)");
			statement.executeUpdate("CREATE TABLE PAYMENTS (RECEIPT VARCHAR(40), TOTAL DOUBLE)");
			statement.executeUpdate("INSERT INTO RECEIPTS VALUES ('current', '2026-10-24 12:00:00')");
			statement.executeUpdate("INSERT INTO RECEIPTS VALUES ('elapsed', '2025-10-15 12:00:00')");
			statement.executeUpdate("INSERT INTO RECEIPTS VALUES ('rest', '2025-10-27 12:00:00')");
			statement.executeUpdate("INSERT INTO TICKETS VALUES ('current', 0), ('elapsed', 0), ('rest', 0)");
			statement.executeUpdate(
					"INSERT INTO PAYMENTS VALUES ('current', 80.00), ('elapsed', 60.00), ('rest', 40.00)");
			Date cutoff = Date.from(LocalDate.of(2026, 10, 24).atStartOfDay(ZoneId.systemDefault()).toInstant());
			for (ReportsWelcomePeriod period : new ReportsWelcomePeriod[]{ReportsWelcomePeriod.YEAR,
					ReportsWelcomePeriod.ROLLING_YEAR}) {
				SalesTrend trend = new SalesTrendRepository().load(connection, period, cutoff);
				int month = period == ReportsWelcomePeriod.YEAR ? 9 : 11;
				assertEquals(month, trend.partialMonthIndex);
				assertEquals(80, trend.current[month], 0.001);
				assertEquals(60, trend.previousElapsed[month], 0.001);
				assertEquals(100, trend.previous[month], 0.001);
				assertTrue(trend.labels[month].contains("(1–24)"), trend.labels[month]);
			}
			assertEquals(-1,
					new SalesTrendRepository().load(connection, ReportsWelcomePeriod.MONTH, cutoff).partialMonthIndex);
		}
	}

	@Test
	public void pairsTheSameWeekdaysAndLeavesFutureDaysEmpty() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:derby:memory:reportsTrendIT;create=true");
				Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE RECEIPTS (ID VARCHAR(40), DATENEW TIMESTAMP)");
			statement.executeUpdate("CREATE TABLE TICKETS (ID VARCHAR(40), TICKETTYPE INTEGER)");
			statement.executeUpdate("CREATE TABLE PAYMENTS (RECEIPT VARCHAR(40), TOTAL DOUBLE)");
			statement.executeUpdate("INSERT INTO RECEIPTS VALUES ('now', '2026-09-25 12:00:00')");
			statement.executeUpdate("INSERT INTO RECEIPTS VALUES ('previous', '2025-09-26 12:00:00')");
			statement.executeUpdate("INSERT INTO TICKETS VALUES ('now', 0), ('previous', 0)");
			statement.executeUpdate("INSERT INTO PAYMENTS VALUES ('now', 25.00), ('previous', 15.00)");
			Date today = Date.from(LocalDate.of(2026, 9, 25).atStartOfDay(ZoneId.systemDefault()).toInstant());
			SalesTrend trend = new SalesTrendRepository().load(connection, ReportsWelcomePeriod.WEEK, today);
			assertEquals(7, trend.labels.length);
			assertTrue(trend.labels[0].endsWith(" 21"), trend.labels[0]);
			assertTrue(trend.labels[6].endsWith(" 27"), trend.labels[6]);
			assertEquals(25.0, trend.current[4], 0.001);
			assertEquals(15.0, trend.previous[4], 0.001);
			assertTrue(trend.reached[4]);
			assertFalse(trend.reached[5]);
			Locale original = Locale.getDefault();
			try {
				AppLocal.setLocale(new Locale("ca", "ES"));
				SalesTrend catalan = new SalesTrendRepository().load(connection, ReportsWelcomePeriod.WEEK, today);
				assertTrue(catalan.labels[0].startsWith("dl."), catalan.labels[0]);
				assertTrue(catalan.labels[6].startsWith("dg."), catalan.labels[6]);
			} finally {
				AppLocal.setLocale(original);
			}
		}
	}
}
