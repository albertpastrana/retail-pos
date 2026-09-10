package com.openbravo.pos.forms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatabaseMigratorIT {

	private static final long WAIT_MS = 60000L;

	@Test
	public void migratesEmptyDerby() throws Exception {
		String url = "jdbc:derby:memory:flywayIT;create=true";
		assertFreshInstall(url, null, null);
		assertIdempotent(url, null, null);
	}

	@Test
	public void adoptsDerbySchemaCreatedBeforeFlyway() throws Exception {
		String url = "jdbc:derby:memory:flywayLegacyIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		dropSchemaHistory(url, null, null);

		DatabaseMigrator.migrate(url, null, null);

		Connection connection = open(url, null, null);
		try {
			assertEquals(8, count(connection, "flyway_schema_history"));
			assertEquals(34, count(connection, "RESOURCES"));
			assertEquals(4, count(connection, "PRODUCTS"));
			assertEquals(1, count(connection, "PRICE_RULES"));
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
			assertEquals(9, count(connection, "flyway_schema_history"));
			assertEquals(34, count(connection, "RESOURCES"));
			assertEquals(4, countWhereNotNull(connection, "ROLES", "PERMISSIONS"));
			assertEquals(4, count(connection, "PRODUCTS"));
			assertEquals(4, count(connection, "PEOPLE"));
			assertEquals(1, count(connection, "PRICE_RULES"));
		} finally {
			connection.close();
		}
	}

	private static void assertIdempotent(String url, String user, String password) throws SQLException {
		DatabaseMigrator.migrate(url, user, password);
		Connection connection = open(url, user, password);
		try {
			assertEquals(9, count(connection, "flyway_schema_history"));
			assertEquals(34, count(connection, "RESOURCES"));
		} finally {
			connection.close();
		}
	}

	private static void dropSchemaHistory(String url, String user, String password) throws SQLException {
		Connection connection = open(url, user, password);
		try {
			Statement statement = connection.createStatement();
			try {
				statement.execute("DROP TABLE " + quotedTable(connection, "flyway_schema_history"));
			} finally {
				statement.close();
			}
		} finally {
			connection.close();
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

	private static String quotedTable(Connection connection, String name) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		String quote = metadata.getIdentifierQuoteString();
		ResultSet tables = metadata.getTables(connection.getCatalog(), null, "%", new String[] { "TABLE" });
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
