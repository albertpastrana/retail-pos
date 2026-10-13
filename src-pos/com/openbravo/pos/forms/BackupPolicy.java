package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Single shared schedule and atomic, expiring claim for scheduled backups. */
public final class BackupPolicy {
	private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private static final int LEASE_HOURS = 24;
	private final boolean enabled;
	private final int hour;
	private final int retentionDays;
	private final String terminalId;

	private BackupPolicy(boolean enabled, int hour, int retentionDays, String terminalId) {
		this.enabled = enabled;
		this.hour = hour;
		this.retentionDays = retentionDays;
		this.terminalId = terminalId;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getHour() {
		return hour;
	}

	public int getRetentionDays() {
		return retentionDays;
	}

	public String getTerminalId() {
		return terminalId;
	}

	public static BackupPolicy load(Session session) throws SQLException {
		try (PreparedStatement statement = session.getConnection().prepareStatement(
				"SELECT ENABLED, HOUR_OF_DAY, RETENTION_DAYS, TERMINAL_ID FROM BACKUP_POLICY WHERE ID = '0'");
				ResultSet result = statement.executeQuery()) {
			if (!result.next()) {
				throw new SQLException("Backup policy is missing");
			}
			return new BackupPolicy(result.getInt(1) != 0, result.getInt(2), result.getInt(3), result.getString(4));
		}
	}

	public static void save(Session session, boolean enabled, int hour, int retentionDays, String terminalId)
			throws SQLException {
		if (hour < 0 || hour > 23 || retentionDays < 1) {
			throw new SQLException("Invalid backup schedule or retention");
		}
		try (PreparedStatement statement = session.getConnection().prepareStatement(
				"UPDATE BACKUP_POLICY SET ENABLED = ?, HOUR_OF_DAY = ?, RETENTION_DAYS = ?, TERMINAL_ID = ? WHERE ID = '0'")) {
			statement.setInt(1, enabled ? 1 : 0);
			statement.setInt(2, hour);
			statement.setInt(3, retentionDays);
			statement.setString(4, terminalId);
			if (statement.executeUpdate() != 1) {
				throw new SQLException("Backup policy was not saved");
			}
		}
	}

	/** Returns a token only if this terminal won today's lease. */
	public static String claim(Session session, String terminalId, LocalDateTime now) throws SQLException {
		String token = UUID.randomUUID().toString();
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("UPDATE BACKUP_POLICY SET LEASE_OWNER = ?, LEASE_UNTIL = ? "
						+ "WHERE ID = '0' AND ENABLED = 1 AND TERMINAL_ID = ? AND HOUR_OF_DAY <= ? "
						+ "AND (LAST_DATE IS NULL OR LAST_DATE <> ?) "
						+ "AND (LEASE_UNTIL IS NULL OR LEASE_UNTIL < ?)")) {
			statement.setString(1, token);
			statement.setTimestamp(2, Timestamp.valueOf(now.plusHours(LEASE_HOURS)));
			statement.setString(3, terminalId);
			statement.setInt(4, now.getHour());
			statement.setString(5, now.toLocalDate().format(DATE));
			statement.setTimestamp(6, Timestamp.valueOf(now));
			return statement.executeUpdate() == 1 ? token : null;
		}
	}

	public static void release(Session session, String token, LocalDate date, boolean success) throws SQLException {
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("UPDATE BACKUP_POLICY SET LEASE_OWNER = NULL, LEASE_UNTIL = NULL, "
						+ "LAST_DATE = CASE WHEN ? = 1 THEN ? ELSE LAST_DATE END WHERE ID = '0' AND LEASE_OWNER = ?")) {
			statement.setInt(1, success ? 1 : 0);
			statement.setString(2, date.format(DATE));
			statement.setString(3, token);
			statement.executeUpdate();
		}
	}

	public static Session connect(AppProperties props) throws BasicException {
		return AppViewConnection.createSession(props);
	}
}
