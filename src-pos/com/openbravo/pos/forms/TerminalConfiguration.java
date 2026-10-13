package com.openbravo.pos.forms;

import com.openbravo.data.loader.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/** Database-owned terminal identity and operational preferences. */
public final class TerminalConfiguration {
	private static final String[] COLUMNS = {"PRINTER", "PRINTER_2", "PRINTER_3", "PRINTER_NAME", "DISPLAY_DEVICE",
			"SCANNER", "SCREEN_MODE", "TICKETS_BAG", "MENU_STATE", "UNIQUE_INSTANCE", "LANGUAGE", "COUNTRY", "VARIANT",
			"FORMAT_INTEGER", "FORMAT_DOUBLE", "FORMAT_CURRENCY", "FORMAT_PERCENT", "FORMAT_DATE", "FORMAT_TIME",
			"FORMAT_DATETIME", "PAPER_RECEIPT_X", "PAPER_RECEIPT_Y", "PAPER_RECEIPT_WIDTH", "PAPER_RECEIPT_HEIGHT",
			"PAPER_RECEIPT_MEDIA", "PAPER_STANDARD_X", "PAPER_STANDARD_Y", "PAPER_STANDARD_WIDTH",
			"PAPER_STANDARD_HEIGHT", "PAPER_STANDARD_MEDIA", "UPDATE_CHECK", "UPDATE_URL"};
	private static final String[] KEYS = {"machine.printer", "machine.printer.2", "machine.printer.3",
			"machine.printername", "machine.display", "machine.scanner", "machine.screenmode", "machine.ticketsbag",
			"ui.menu.state", "machine.uniqueinstance", "user.language", "user.country", "user.variant",
			"format.integer", "format.double", "format.currency", "format.percent", "format.date", "format.time",
			"format.datetime", "paper.receipt.x", "paper.receipt.y", "paper.receipt.width", "paper.receipt.height",
			"paper.receipt.mediasizename", "paper.standard.x", "paper.standard.y", "paper.standard.width",
			"paper.standard.height", "paper.standard.mediasizename", "update.check", "update.url"};

	private TerminalConfiguration() {
	}

	public static void load(AppConfig config, Session session) throws SQLException {
		Connection connection = session.getConnection();
		String id = config.getHost();
		if (id == null) {
			throw new SQLException("Terminal identity is missing");
		}
		boolean originalAutoCommit = connection.getAutoCommit();
		try {
			connection.setAutoCommit(false);
			if (!exists(connection, "SELECT ID FROM TERMINALS WHERE ID = ?", id)) {
				register(connection, config);
			}
			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(originalAutoCommit);
		}
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT * FROM TERMINAL_SETTINGS WHERE TERMINAL_ID = ?")) {
			statement.setString(1, id);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					throw new SQLException("Missing settings for terminal " + id);
				}
				for (int i = 0; i < KEYS.length; i++) {
					config.setProperty(KEYS[i],
							(i == 9 || i == 30)
									? Boolean.toString(result.getInt(COLUMNS[i]) != 0)
									: result.getString(COLUMNS[i]));
				}
			}
		}
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT ENABLED, LAST_DATE FROM BACKUP_POLICY WHERE ID = '0'");
				ResultSet result = statement.executeQuery()) {
			if (!result.next()) {
				throw new SQLException("Backup policy is missing");
			}
			config.setProperty("backup.daily", Boolean.toString(result.getInt(1) != 0));
			config.setProperty("backup.lastdate", result.getString(2));
		}
	}

	private static void register(Connection connection, AppConfig config) throws SQLException {
		String id = config.getHost();
		String host = config.getProperty("machine.hostname");
		String resource = null;
		Properties legacy = new Properties();
		if (host != null && !host.isEmpty()) {
			String candidate = host + "/properties";
			if (!exists(connection, "SELECT ID FROM TERMINALS WHERE LEGACY_RESOURCE = ?", candidate)) {
				try (PreparedStatement statement = connection
						.prepareStatement("SELECT CONTENT FROM RESOURCES WHERE NAME = ?")) {
					statement.setString(1, candidate);
					try (ResultSet result = statement.executeQuery()) {
						if (result.next() && result.getBytes(1) != null) {
							try {
								legacy.loadFromXML(new ByteArrayInputStream(result.getBytes(1)));
								resource = candidate;
							} catch (IOException e) {
								throw new SQLException("Cannot read legacy terminal state for " + host, e);
							}
						}
					}
				}
			}
		}
		String cash = legacy.getProperty("activecash");
		if (cash != null) {
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT HOST FROM CLOSEDCASH WHERE MONEY = ? AND DATEEND IS NULL")) {
				statement.setString(1, cash);
				try (ResultSet result = statement.executeQuery()) {
					if (!result.next() || !result.getString(1).equals(host)) {
						cash = null;
					}
				}
			}
		}
		String location = legacy.getProperty("location", "0");
		if (!exists(connection, "SELECT ID FROM LOCATIONS WHERE ID = ?", location)) {
			location = "0";
		}
		try (PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO TERMINALS (ID, LEGACY_HOSTNAME, LEGACY_RESOURCE, ACTIVE_CASH, LOCATION_ID) VALUES (?, ?, ?, ?, ?)")) {
			statement.setString(1, id);
			statement.setString(2, host);
			statement.setString(3, resource);
			statement.setString(4, cash);
			statement.setString(5, location);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection
				.prepareStatement("INSERT INTO TERMINAL_SETTINGS (TERMINAL_ID) VALUES (?)")) {
			statement.setString(1, id);
			statement.executeUpdate();
		}
		// Import existing local settings before replacing the local file with bootstrap
		// values.
		saveSettings(connection, config);
		if (cash != null) {
			try (PreparedStatement statement = connection.prepareStatement(
					"UPDATE CLOSEDCASH SET HOST = ? WHERE MONEY = ? AND HOST = ? AND DATEEND IS NULL")) {
				statement.setString(1, id);
				statement.setString(2, cash);
				statement.setString(3, host);
				if (statement.executeUpdate() != 1) {
					throw new SQLException("Legacy active cash changed during terminal registration");
				}
			}
		}
		if ("true".equalsIgnoreCase(config.getProperty("backup.daily"))) {
			try (PreparedStatement statement = connection.prepareStatement(
					"UPDATE BACKUP_POLICY SET ENABLED = 1, TERMINAL_ID = ? WHERE ID = '0' AND TERMINAL_ID IS NULL")) {
				statement.setString(1, id);
				statement.executeUpdate();
			}
		}
	}

	private static boolean exists(Connection connection, String sql, String value) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, value);
			try (ResultSet result = statement.executeQuery()) {
				return result.next();
			}
		}
	}

	public static void save(AppConfig config, Session session) throws SQLException {
		saveSettings(session.getConnection(), config);
	}

	public static void restoreDefaults(AppConfig config, Session session) throws SQLException {
		AppConfig defaults = new AppConfig();
		defaults.load(null);
		for (String key : KEYS) {
			config.setProperty(key, defaults.getProperty(key));
		}
		save(config, session);
	}

	private static void saveSettings(Connection connection, AppConfig config) throws SQLException {
		StringBuilder sql = new StringBuilder("UPDATE TERMINAL_SETTINGS SET ");
		for (int i = 0; i < COLUMNS.length; i++) {
			if (i > 0) {
				sql.append(", ");
			}
			sql.append(COLUMNS[i]).append(" = ?");
		}
		sql.append(" WHERE TERMINAL_ID = ?");
		try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
			for (int i = 0; i < KEYS.length; i++) {
				String value = config.getProperty(KEYS[i]);
				if (i == 9 || i == 30) {
					statement.setInt(i + 1, "true".equalsIgnoreCase(value) ? 1 : 0);
				} else {
					statement.setString(i + 1, value == null ? "" : value);
				}
			}
			statement.setString(KEYS.length + 1, config.getHost());
			if (statement.executeUpdate() != 1) {
				throw new SQLException("Terminal settings were not saved");
			}
		}
	}

	public static String activeCash(Session session, String id) throws SQLException {
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("SELECT ACTIVE_CASH FROM TERMINALS WHERE ID = ?")) {
			statement.setString(1, id);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					throw new SQLException("Terminal is not registered");
				}
				return result.getString(1);
			}
		}
	}

	public static String location(Session session, String id) throws SQLException {
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("SELECT LOCATION_ID FROM TERMINALS WHERE ID = ?")) {
			statement.setString(1, id);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					throw new SQLException("Terminal is not registered");
				}
				return result.getString(1);
			}
		}
	}

	public static void setActiveCash(Session session, String id, String cash) throws SQLException {
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("UPDATE TERMINALS SET ACTIVE_CASH = ? WHERE ID = ?")) {
			statement.setString(1, cash);
			statement.setString(2, id);
			if (statement.executeUpdate() != 1) {
				throw new SQLException("Terminal is not registered");
			}
		}
	}

	public static void setLocation(Session session, String id, String location) throws SQLException {
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("UPDATE TERMINALS SET LOCATION_ID = ? WHERE ID = ?")) {
			statement.setString(1, location);
			statement.setString(2, id);
			if (statement.executeUpdate() != 1) {
				throw new SQLException("Terminal is not registered");
			}
		}
	}
}
