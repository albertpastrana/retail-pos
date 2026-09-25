package com.openbravo.pos.forms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.openbravo.data.loader.Session;
import com.openbravo.pos.ticket.ProductInfoExt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseMigratorIT {

	private static final long WAIT_MS = 60000L;

	@Test
	public void migratesEmptyDerby() throws Exception {
		String url = "jdbc:derby:memory:flywayIT;create=true";
		assertFreshInstall(url, null, null);
		assertIdempotent(url, null, null);
	}

	@Test
	public void readsFallbackCatalogPriceOverlay() throws Exception {
		String url = "jdbc:derby:memory:fallbackCatalogIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			try (Statement statement = session.getConnection().createStatement()) {
				statement.executeUpdate("INSERT INTO CATALOG_FALLBACK_PRODUCTS "
						+ "(ID, BARCODE, REFERENCE, NAME, CATEGORY_ID, CATEGORY_NAME, PRICE_BUY, PRICE_SELL, BRAND) "
						+ "VALUES ('fallback-1', '9990000000101', 'P761237-R64-3XL', 'Fallback shirt', "
						+ "'cat-1', 'Shirts', 10.0, 20.0, 'Massana')");
				statement.executeUpdate("INSERT INTO CATALOG_FALLBACK_PRICES "
						+ "(LOOKUP_CODE, REFERENCE, PRICE_BUY, PRICE_SELL, BRAND, SOURCE) VALUES "
						+ "('9990000000101', 'P761237-R64-3XL', 12.5, 25.0, 'Massana', 'supplier')");
			}
			DataLogicSales sales = new DataLogicSales();
			sales.init(session);
			ProductInfoExt product = sales.getCatalogProductByCode("9990000000101", null, null);
			assertEquals("Fallback shirt", product.getName());
			assertEquals(12.5, product.getPriceBuy(), 0.0001);
			assertEquals("Shirts", product.getProperty("catalog.category.name"));
		} finally {
			session.close();
		}
	}

	@Test
	public void readsFallbackCatalogFamilyByFamilyKey() throws Exception {
		String url = "jdbc:derby:memory:fallbackCatalogFamilyIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			try (Statement statement = session.getConnection().createStatement()) {
				statement.executeUpdate("INSERT INTO CATALOG_FALLBACK_PRODUCTS "
						+ "(ID, BARCODE, REFERENCE, NAME, CATEGORY_ID, CATEGORY_NAME, PRICE_BUY, PRICE_SELL, BRAND, FAMILY) "
						+ "VALUES ('fallback-family-1', '9990000000201', 'P761237-R64-3XL', 'Variant one', "
						+ "'cat-1', 'Shirts', 10.0, 20.0, 'Massana', 'Massana|P761237')");
				statement.executeUpdate("INSERT INTO CATALOG_FALLBACK_PRODUCTS "
						+ "(ID, BARCODE, REFERENCE, NAME, CATEGORY_ID, CATEGORY_NAME, PRICE_BUY, PRICE_SELL, BRAND, FAMILY) "
						+ "VALUES ('fallback-family-2', '9990000000202', 'P761237-R65-3XL', 'Variant two', "
						+ "'cat-1', 'Shirts', 11.0, 21.0, 'Massana', 'Massana|P761237')");
			}
			DataLogicSales sales = new DataLogicSales();
			sales.init(session);
			assertEquals(2, sales.getCatalogProductFamily("9990000000201", null, null).size());
			assertEquals("Massana|P761237",
					sales.getCatalogProductFamily("9990000000202", null, null).get(0).getFamily());
		} finally {
			session.close();
		}
	}

	@Test
	public void adoptsDerbySchemaCreatedBeforeFlyway() throws Exception {
		String url = "jdbc:derby:memory:flywayLegacyIT;create=true";
		DatabaseMigrator.migrate(url, null, null);

		Connection connection = open(url, null, null);
		try {
			assertEquals(0, queryInt(connection, "SELECT COUNT(*) FROM RESOURCES WHERE NAME = 'Ticket.Buttons'"));
			assertEquals(4, count(connection, "PRODUCTS"));
			assertEquals(4, queryInt(connection,
					"SELECT COUNT(*) FROM PRODUCTS WHERE CREATED_AT IS NOT NULL AND UPDATED_AT IS NOT NULL"));
			assertEquals(4, queryInt(connection, "SELECT COUNT(*) FROM PRODUCTS WHERE ISVOUCHER = TRUE"));
			assertEquals(1, count(connection, "PRICE_RULES"));
			assertEquals(0, count(connection, "CATALOG_FALLBACK_PRODUCTS"));
			assertEquals(0, count(connection, "CATALOG_FALLBACK_PRICES"));
		} finally {
			connection.close();
		}
	}

	@Test
	public void ignoresObjectsLeftInOtherSchemas() throws Exception {
		String url = "jdbc:derby:memory:flywayOtherSchemaIT;create=true";
		Connection connection = open(url, null, null);
		try {
			Statement statement = connection.createStatement();
			try {
				statement.execute("CREATE SCHEMA LEFTOVER");
				statement.execute("CREATE TABLE LEFTOVER.PRICE_RULES (ID VARCHAR(255) NOT NULL PRIMARY KEY)");
				statement.execute("CREATE TABLE LEFTOVER.PRODUCTS (ID VARCHAR(255), PRICEBUY_WHOLESALE DOUBLE)");
			} finally {
				statement.close();
			}
		} finally {
			connection.close();
		}

		DatabaseMigrator.migrate(url, null, null);

		connection = open(url, null, null);
		try {
			assertEquals(1, count(connection, "PRICE_RULES"));
			assertEquals(0, queryInt(connection, "SELECT COUNT(PRICEBUY_WHOLESALE) FROM PRODUCTS"));
		} finally {
			connection.close();
		}
	}

	@Test
	public void migratesEmptyMysql() throws Exception {
		String url = System.getProperty("pos.mysql.url", "jdbc:mysql://127.0.0.1:13306/pos");
		String user = System.getProperty("pos.mysql.user", "root");
		String password = System.getProperty("pos.mysql.password", "pos");
		waitFor(url, user, password).close();
		assertFreshInstall(url, user, password);
		assertIdempotent(url, user, password);
	}

	@Test
	public void migratesEmptyPostgres() throws Exception {
		String url = System.getProperty("pos.postgres.url", "jdbc:postgresql://127.0.0.1:15432/pos");
		String user = System.getProperty("pos.postgres.user", "pos");
		String password = System.getProperty("pos.postgres.password", "pos");
		waitFor(url, user, password).close();
		assertFreshInstall(url, user, password);
		assertIdempotent(url, user, password);
	}

	private static void assertFreshInstall(String url, String user, String password) throws SQLException {
		DatabaseMigrator.migrate(url, user, password);
		Connection connection = open(url, user, password);
		try {
			assertEquals(0, queryInt(connection, "SELECT COUNT(*) FROM RESOURCES WHERE NAME = 'Ticket.Buttons'"));
			assertEquals(4, countWhereNotNull(connection, "ROLES", "PERMISSIONS"));
			assertEquals(4, count(connection, "ROLES"));
			assertTrue(indexExists(connection, "PAYMENTS", "PAYMENTS_RECEIPT_INX"));
			assertEquals(0, queryInt(connection, "SELECT COUNT(*) FROM TICKETLINES WHERE LOYALTYREDEMPTION = TRUE"));
			assertEquals(1,
					queryInt(connection, "SELECT COUNT(*) FROM ROLES WHERE ID = '0' AND NAME = 'Administrator'"));
			assertEquals(1, queryInt(connection, "SELECT COUNT(*) FROM ROLES WHERE ID = '1' AND NAME = 'Manager'"));
			assertEquals(0, queryInt(connection, "SELECT COUNT(*) FROM RESOURCES WHERE NAME = 'Menu.Root'"));
			assertEquals(0,
					queryInt(connection,
							"SELECT COUNT(*) FROM RESOURCES WHERE NAME IN "
									+ "('payment.cash', 'banknote.50euro', 'banknote.20euro', 'banknote.10euro', "
									+ "'banknote.5euro', 'coin.2euro', 'coin.1euro', 'coin.50cent', 'coin.20cent', "
									+ "'coin.10cent', 'coin.5cent', 'coin.2cent', 'coin.1cent')"));
			assertTrue(roleContains(connection, "0", "com.openbravo.pos.forms.JPanelWelcome"));
			assertTrue(roleContains(connection, "1", "com.openbravo.pos.forms.JPanelWelcome"));
			assertTrue(resourceContains(connection, "Printer.Ticket", "Entregat:"));
			assertTrue(resourceContains(connection, "Printer.Ticket", "printChange()"));
			assertTrue(resourceContains(connection, "Printer.TicketPreview", "Entregat:"));
			assertTrue(resourceContains(connection, "Printer.TicketPreview", "printChange()"));
			assertTrue(roleContains(connection, "0", "JPanelSalesSummary"));
			assertTrue(roleContains(connection, "0", "JPanelProductSales"));
			assertTrue(roleContains(connection, "0", "JPanelPaymentSales"));
			assertTrue(roleContains(connection, "0", "JPanelLowStock"));
			assertTrue(roleContains(connection, "0", "JPanelTaxSummary"));
			assertTrue(roleContains(connection, "0", "JPanelCashClosing"));
			assertTrue(roleContains(connection, "0", "JPanelCustomerDebt"));
			assertTrue(roleContains(connection, "1", "JPanelSalesSummary"));
			assertTrue(roleContains(connection, "1", "JPanelProductSales"));
			assertTrue(roleContains(connection, "1", "JPanelPaymentSales"));
			assertTrue(roleContains(connection, "1", "JPanelLowStock"));
			assertTrue(roleContains(connection, "1", "JPanelTaxSummary"));
			assertTrue(roleContains(connection, "1", "JPanelCashClosing"));
			assertTrue(roleContains(connection, "1", "JPanelCustomerDebt"));
			assertTrue(roleContains(connection, "0", "button.print"));
			assertTrue(roleContains(connection, "1", "button.print"));
			assertTrue(roleContains(connection, "2", "button.print"));
			assertEquals(1, queryInt(connection, "SELECT COUNT(*) FROM ROLES WHERE ID = '2' AND NAME = 'Employee'"));
			assertEquals(4, count(connection, "PRODUCTS"));
			assertEquals(4, queryInt(connection,
					"SELECT COUNT(*) FROM PRODUCTS WHERE CREATED_AT IS NOT NULL AND UPDATED_AT IS NOT NULL"));
			assertEquals(4, queryInt(connection, "SELECT COUNT(*) FROM PRODUCTS WHERE ISVOUCHER = TRUE"));
			assertEquals(4, count(connection, "PEOPLE"));
			assertEquals(1, count(connection, "PRICE_RULES"));
			assertEquals(0, count(connection, "CATALOG_FALLBACK_PRODUCTS"));
			assertEquals(0, count(connection, "CATALOG_FALLBACK_PRICES"));
			assertEquals(1, count(connection, "LOYALTY_SETTINGS"));
			assertEquals(1,
					queryInt(connection, "SELECT COUNT(*) FROM LOYALTY_SETTINGS WHERE ID = '0' AND ENABLED = FALSE "
							+ "AND NAME = '' AND ELIGIBLE_SPEND_PER_STAMP = 10.0 AND REDEMPTION_VALUE = 5.0"));
			assertEquals(0, tableCount(connection, "APPLICATIONS"));
		} finally {
			connection.close();
		}
	}

	private static void assertIdempotent(String url, String user, String password) throws SQLException {
		DatabaseMigrator.migrate(url, user, password);
		Connection connection = open(url, user, password);
		try {
			assertTrue(indexExists(connection, "PAYMENTS", "PAYMENTS_RECEIPT_INX"));
			assertEquals(4, queryInt(connection,
					"SELECT COUNT(*) FROM PRODUCTS WHERE CREATED_AT IS NOT NULL AND UPDATED_AT IS NOT NULL"));
			assertEquals(0, tableCount(connection, "APPLICATIONS"));
		} finally {
			connection.close();
		}
	}

	private static boolean indexExists(Connection connection, String table, String index) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		boolean derby = metadata.getDatabaseProductName().toLowerCase().contains("derby");
		String actualTable = null;
		String actualCatalog = null;
		String actualSchema = null;
		try (ResultSet tables = metadata.getTables(null, null, null, new String[]{"TABLE"})) {
			while (tables.next()) {
				if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
					actualTable = tables.getString("TABLE_NAME");
					actualCatalog = tables.getString("TABLE_CAT");
					actualSchema = tables.getString("TABLE_SCHEM");
					break;
				}
			}
		}
		if (actualTable == null) {
			return false;
		}
		try (ResultSet indexes = metadata.getIndexInfo(actualCatalog, actualSchema, actualTable, false, false)) {
			while (indexes.next()) {
				if (index.equalsIgnoreCase(indexes.getString("INDEX_NAME"))
						|| (derby && "RECEIPT".equalsIgnoreCase(indexes.getString("COLUMN_NAME")))) {
					return true;
				}
			}
			return false;
		}
	}

	private static Connection waitFor(String url, String user, String password) {
		long deadline = System.currentTimeMillis() + WAIT_MS;
		SQLException last = null;
		while (System.currentTimeMillis() < deadline) {
			try {
				return open(url, user, password);
			} catch (SQLException e) {
				last = e;
				try {
					Thread.sleep(500L);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					throw new AssertionError("Interrupted waiting for " + url, interrupted);
				}
			}
		}
		throw new AssertionError("Database not reachable: " + url, last);
	}

	private static Connection open(String url, String user, String password) throws SQLException {
		if (user == null || user.isEmpty()) {
			return DriverManager.getConnection(url);
		}
		return DriverManager.getConnection(url, user, password);
	}

	private static int count(Connection connection, String table) throws SQLException {
		return queryInt(connection, "SELECT COUNT(*) FROM " + quotedTable(connection, table));
	}

	private static int tableCount(Connection connection, String table) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		try (ResultSet tables = metadata.getTables(connection.getCatalog(), connection.getSchema(), null,
				new String[]{"TABLE"})) {
			int count = 0;
			while (tables.next()) {
				if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
					count++;
				}
			}
			return count;
		}
	}

	private static int countWhereNotNull(Connection connection, String table, String column) throws SQLException {
		return queryInt(connection,
				"SELECT COUNT(*) FROM " + quotedTable(connection, table) + " WHERE " + column + " IS NOT NULL");
	}

	private static int queryInt(Connection connection, String sql) throws SQLException {
		Statement statement = connection.createStatement();
		try {
			ResultSet results = statement.executeQuery(sql);
			try {
				results.next();
				return results.getInt(1);
			} finally {
				results.close();
			}
		} finally {
			statement.close();
		}
	}

	private static boolean resourceContains(Connection connection, String name, String text) throws SQLException {
		try (java.sql.PreparedStatement statement = connection
				.prepareStatement("SELECT CONTENT FROM RESOURCES WHERE NAME = ?")) {
			statement.setString(1, name);
			try (ResultSet results = statement.executeQuery()) {
				results.next();
				return new String(results.getBytes(1), StandardCharsets.UTF_8).contains(text);
			}
		}
	}

	private static boolean roleContains(Connection connection, String id, String text) throws SQLException {
		try (java.sql.PreparedStatement statement = connection
				.prepareStatement("SELECT PERMISSIONS FROM ROLES WHERE ID = ?")) {
			statement.setString(1, id);
			try (ResultSet results = statement.executeQuery()) {
				results.next();
				return new String(results.getBytes(1), StandardCharsets.UTF_8).contains(text);
			}
		}
	}

	private static String quotedTable(Connection connection, String name) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		String quote = metadata.getIdentifierQuoteString();
		ResultSet tables = metadata.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"});
		try {
			while (tables.next()) {
				String actual = tables.getString("TABLE_NAME");
				if (name.equalsIgnoreCase(actual)) {
					return quote + actual + quote;
				}
			}
		} finally {
			tables.close();
		}
		throw new SQLException("Missing table " + name);
	}
}
