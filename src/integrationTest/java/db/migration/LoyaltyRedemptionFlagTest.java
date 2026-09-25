package db.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

public class LoyaltyRedemptionFlagTest {
	@Test
	public void backfillsOnlyMarkedHistoricalNegativeLines() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:derby:memory:loyaltyBackfillIT;create=true")) {
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("CREATE TABLE TICKETLINES (TICKET VARCHAR(40), LINE INTEGER, "
						+ "PRODUCT VARCHAR(40), PRICE DOUBLE, ATTRIBUTES BLOB, " + "PRIMARY KEY (TICKET, LINE))");
			}
			insert(connection, "marked", null, -5, true);
			insert(connection, "unmarked", null, -5, false);
			insert(connection, "product", "p1", -5, true);
			insert(connection, "positive", null, 5, true);
			Context context = (Context) Proxy.newProxyInstance(Context.class.getClassLoader(),
					new Class<?>[]{Context.class}, (proxy, method, args) -> {
						if ("getConnection".equals(method.getName())) {
							return connection;
						}
						throw new UnsupportedOperationException(method.getName());
					});
			new V49__loyalty_redemption_flag().migrate(context);
			try (Statement statement = connection.createStatement();
					ResultSet rows = statement
							.executeQuery("SELECT TICKET, LOYALTYREDEMPTION FROM TICKETLINES ORDER BY TICKET")) {
				assertEquals(true, flag(rows, "marked"));
				assertEquals(false, flag(rows, "positive"));
				assertEquals(false, flag(rows, "product"));
				assertEquals(false, flag(rows, "unmarked"));
			}
		}
	}

	private boolean flag(ResultSet rows, String ticket) throws Exception {
		rows.next();
		assertEquals(ticket, rows.getString(1));
		return rows.getBoolean(2);
	}

	private void insert(Connection connection, String ticket, String product, double price, boolean redemption)
			throws Exception {
		Properties attributes = new Properties();
		attributes.setProperty("loyalty.redemption", Boolean.toString(redemption));
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		attributes.storeToXML(xml, "Retail POS", "UTF-8");
		try (PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO TICKETLINES (TICKET, LINE, PRODUCT, PRICE, ATTRIBUTES) VALUES (?, 0, ?, ?, ?)")) {
			statement.setString(1, ticket);
			statement.setString(2, product);
			statement.setDouble(3, price);
			statement.setBytes(4, xml.toByteArray());
			statement.executeUpdate();
		}
	}
}
