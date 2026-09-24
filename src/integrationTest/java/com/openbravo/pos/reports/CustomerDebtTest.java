package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CustomerDebtTest {

	@Test
	public void loadsNullAndZeroDebtAndIgnoresCustomerlessSales() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:derby:memory:customerDebt;create=true")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE TABLE CUSTOMERS (SEARCHKEY VARCHAR(255), NAME VARCHAR(255), "
						+ "CURDEBT DOUBLE, MAXDEBT DOUBLE)");
				statement.execute("CREATE TABLE TICKETS (CUSTOMER VARCHAR(255))");
				statement.execute("INSERT INTO CUSTOMERS VALUES ('A', 'Alice', NULL, 100)");
				statement.execute("INSERT INTO CUSTOMERS VALUES ('B', 'Bob', 0, 0)");
				statement.execute("INSERT INTO TICKETS VALUES (NULL)");
			}

			List<CustomerDebtRow> rows = new CustomerDebtRepository().load(connection);

			assertEquals(2, rows.size());
			assertEquals(100.0, rows.get(0).getRemainingCredit(), 0.000001);
			assertEquals(0.0, rows.get(1).getCurrentDebt(), 0.000001);
			assertEquals(0.0, rows.get(1).getRemainingCredit(), 0.000001);
		}
	}

	@Test
	public void keepsOverLimitDebtAsNegativeRemainingCredit() {
		CustomerDebtRow row = new CustomerDebtRow("A", "Alice", 125.0, 100.0, -25.0);

		assertEquals(-25.0, row.getRemainingCredit(), 0.000001);
	}
}
