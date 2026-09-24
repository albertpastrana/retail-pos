package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CashClosingTest {

	@Test
	public void calculatesSignedPaymentTotals() {
		CashClosingRow row = new CashClosingRow("money", "register", 4, new Date(1000L), null, 3, 125.0, -20.0);

		assertEquals(105.0, row.getNetPayments(), 0.000001);
		assertEquals(3, row.getReceiptCount());
		assertNull(row.getDateEnd());
		assertEquals(true, row.isOpen());
	}

	@Test
	public void protectsSessionDatesFromMutation() {
		Date start = new Date(1000L);
		CashClosingRow row = new CashClosingRow("money", "register", 1, start, new Date(2000L), 0, 0.0, 0.0);
		start.setTime(3000L);

		assertEquals(1000L, row.getDateStart().getTime());
	}

	@Test
	public void loadsOpenAndClosedSessionsWithSignedPayments() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:derby:memory:cash-closing-report;create=true")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE TABLE CLOSEDCASH (MONEY VARCHAR(255) PRIMARY KEY, HOST VARCHAR(255), "
						+ "HOSTSEQUENCE INTEGER, DATESTART TIMESTAMP, DATEEND TIMESTAMP)");
				statement.execute(
						"CREATE TABLE RECEIPTS (ID VARCHAR(255) PRIMARY KEY, MONEY VARCHAR(255), DATENEW TIMESTAMP)");
				statement.execute("CREATE TABLE TICKETS (ID VARCHAR(255) PRIMARY KEY, TICKETTYPE INTEGER)");
				statement.execute("CREATE TABLE PAYMENTS (ID VARCHAR(255) PRIMARY KEY, RECEIPT VARCHAR(255), "
						+ "PAYMENT VARCHAR(255), TOTAL DOUBLE)");
				statement.execute("INSERT INTO CLOSEDCASH VALUES ('closed', 'register', 1, "
						+ "TIMESTAMP('2026-09-01 08:00:00'), TIMESTAMP('2026-09-01 16:00:00'))");
				statement.execute("INSERT INTO CLOSEDCASH VALUES ('open', 'register', 2, "
						+ "TIMESTAMP('2026-09-02 08:00:00'), NULL)");
				statement.execute(
						"INSERT INTO RECEIPTS VALUES ('sale-receipt', 'closed', TIMESTAMP('2026-09-01 09:00:00'))");
				statement.execute(
						"INSERT INTO RECEIPTS VALUES ('refund-receipt', 'closed', TIMESTAMP('2026-09-01 10:00:00'))");
				statement.execute(
						"INSERT INTO RECEIPTS VALUES ('open-receipt', 'open', TIMESTAMP('2026-09-02 09:00:00'))");
				statement.execute("INSERT INTO TICKETS VALUES ('sale-receipt', 0)");
				statement.execute("INSERT INTO TICKETS VALUES ('refund-receipt', 1)");
				statement.execute("INSERT INTO TICKETS VALUES ('open-receipt', 0)");
				statement.execute("INSERT INTO PAYMENTS VALUES ('sale-payment', 'sale-receipt', 'cash', 100.0)");
				statement.execute(
						"INSERT INTO PAYMENTS VALUES ('refund-payment', 'refund-receipt', 'cashrefund', -15.0)");
				statement.execute("INSERT INTO PAYMENTS VALUES ('drawer', 'sale-receipt', 'paperin', 50.0)");
				statement.execute("INSERT INTO PAYMENTS VALUES ('open-payment', 'open-receipt', 'magcard', 20.0)");
			}

			List<CashClosingRow> rows = new CashClosingRepository().load(connection,
					new SalesSummaryParameters(new Date(0L), new Date(2000000000000L)));

			assertEquals(2, rows.size());
			assertEquals(100.0, rows.get(0).getGrossPayments(), 0.000001);
			assertEquals(-15.0, rows.get(0).getRefunds(), 0.000001);
			assertEquals(85.0, rows.get(0).getNetPayments(), 0.000001);
			assertEquals(1, rows.get(1).getReceiptCount());
			assertEquals(true, rows.get(1).isOpen());
		}
	}
}
