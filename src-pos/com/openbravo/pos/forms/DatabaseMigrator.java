package com.openbravo.pos.forms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.flywaydb.core.Flyway;

final class DatabaseMigrator {
	private static final Logger logger = Logger.getLogger(DatabaseMigrator.class.getName());

	private static final String DERBY_URL_PREFIX = "jdbc:derby:";
	private static final String DATABASE_NOT_FOUND = "XJ004";

	private static final String BINARY_TYPE = "binary_type";
	private static final String BOOLEAN_TYPE = "boolean_type";
	private static final String TRUE_VALUE = "true_value";
	private static final String FALSE_VALUE = "false_value";
	private static final String TICKET_NUMBER_OBJECTS = "ticket_number_objects";

	private DatabaseMigrator() {
	}

	static void migrate(String url, String user, String password) {
		logger.info("event=database_migration_start url=" + LogSanitizer.jdbcUrl(url));
		Map<String, String> placeholders = placeholdersFor(url);
		placeholders.put("app_id", AppLocal.APP_ID);
		placeholders.put("app_name", AppLocal.APP_NAME);
		placeholders.put("app_version", AppLocal.APP_VERSION);

		if (url.startsWith(DERBY_URL_PREFIX)) {
			upgradeDerbyStorage(url, user, password);
		}

		// A database that predates Flyway already holds everything V1 and V2 create,
		// so adopt it at version 2 rather than replaying the baseline over live data.
		try {
			Flyway.configure().dataSource(url, user, password).locations("classpath:db/migration")
					.placeholders(placeholders).baselineOnMigrate(true).baselineVersion("2").validateOnMigrate(true)
					.load().migrate();
			logger.info("event=database_migration_success url=" + LogSanitizer.jdbcUrl(url));
		} catch (RuntimeException e) {
			logger.log(Level.SEVERE, "event=database_migration_failed url=" + LogSanitizer.jdbcUrl(url), e);
			throw e;
		}
	}

	/**
	 * Flyway's schema history table declares a BOOLEAN column, which Derby refuses
	 * to create while the files are still in their pre-10.7 on-disk format. The
	 * hard upgrade is one way, and Derby only rewrites what is actually out of
	 * date.
	 */
	private static void upgradeDerbyStorage(String url, String user, String password) {
		try {
			Connection connection = open(url + ";upgrade=true", user, password);
			connection.close();
		} catch (SQLException e) {
			if (!DATABASE_NOT_FOUND.equals(e.getSQLState())) {
				throw new IllegalStateException("Could not upgrade the Derby database at " + url, e);
			}
			// Nothing on disk yet, so there is no format to upgrade and Flyway will create
			// it.
		}
	}

	private static Connection open(String url, String user, String password) throws SQLException {
		if (user == null || user.isEmpty()) {
			return DriverManager.getConnection(url);
		}
		return DriverManager.getConnection(url, user, password);
	}

	private static Map<String, String> placeholdersFor(String url) {
		Map<String, String> placeholders = new HashMap<String, String>();

		if (url.startsWith(DERBY_URL_PREFIX)) {
			placeholders.put(BINARY_TYPE, "BLOB");
			placeholders.put(BOOLEAN_TYPE, "SMALLINT");
			placeholders.put(TRUE_VALUE, "1");
			placeholders.put(FALSE_VALUE, "0");
			placeholders.put(TICKET_NUMBER_OBJECTS,
					"CREATE TABLE TICKETSNUM (ID INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 1));"
							+ "INSERT INTO TICKETSNUM VALUES (DEFAULT);"
							+ "CREATE TABLE TICKETSNUM_REFUND (ID INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 1));"
							+ "INSERT INTO TICKETSNUM_REFUND VALUES (DEFAULT);"
							+ "CREATE TABLE TICKETSNUM_PAYMENT (ID INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 1));"
							+ "INSERT INTO TICKETSNUM_PAYMENT VALUES (DEFAULT)");
		} else if (url.startsWith("jdbc:mysql:")) {
			placeholders.put(BINARY_TYPE, "MEDIUMBLOB");
			placeholders.put(BOOLEAN_TYPE, "BIT");
			placeholders.put(TRUE_VALUE, "TRUE");
			placeholders.put(FALSE_VALUE, "FALSE");
			placeholders.put(TICKET_NUMBER_OBJECTS,
					"CREATE TABLE TICKETSNUM (ID INTEGER NOT NULL);" + "INSERT INTO TICKETSNUM VALUES (1);"
							+ "CREATE TABLE TICKETSNUM_REFUND (ID INTEGER NOT NULL);"
							+ "INSERT INTO TICKETSNUM_REFUND VALUES (1);"
							+ "CREATE TABLE TICKETSNUM_PAYMENT (ID INTEGER NOT NULL);"
							+ "INSERT INTO TICKETSNUM_PAYMENT VALUES (1)");
		} else if (url.startsWith("jdbc:postgresql:")) {
			placeholders.put(BINARY_TYPE, "BYTEA");
			placeholders.put(BOOLEAN_TYPE, "BOOLEAN");
			placeholders.put(TRUE_VALUE, "TRUE");
			placeholders.put(FALSE_VALUE, "FALSE");
			placeholders.put(TICKET_NUMBER_OBJECTS,
					"CREATE SEQUENCE TICKETSNUM START WITH 1;" + "CREATE SEQUENCE TICKETSNUM_REFUND START WITH 1;"
							+ "CREATE SEQUENCE TICKETSNUM_PAYMENT START WITH 1");
		} else {
			throw new IllegalArgumentException("Flyway migrations support Derby, MySQL and PostgreSQL only: " + url);
		}

		return placeholders;
	}
}
