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
import org.junit.jupiter.api.Test;

public class SalesTrendRepositoryTest {
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
			assertEquals(25.0, trend.current[4], 0.001);
			assertEquals(15.0, trend.previous[4], 0.001);
			assertTrue(trend.reached[4]);
			assertFalse(trend.reached[5]);
		}
	}
}
