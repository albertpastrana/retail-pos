package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.*;

import com.openbravo.data.loader.Session;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalConfigurationIT {
	@Test
	void failedCashCreationRollsBackAndClaimCanBeRetried() throws Exception {
		String url = "jdbc:derby:memory:terminalAtomicIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		String id = UUID.randomUUID().toString();
		AppConfig config = new AppConfig();
		config.load(null);
		config.setProperty("terminal.id", id);
		String cash = UUID.randomUUID().toString();
		try (Connection connection = open(url, null, null)) {
			Session session = new Session(url, null, null);
			try {
				DataLogicSystem logic = new DataLogicSystem();
				logic.init(session);
				assertThrows(java.sql.SQLException.class,
						() -> TerminalConfiguration.createCash(session, logic, id, cash, 1, new Date()));
				assertEquals(0, count(connection, "SELECT COUNT(*) FROM CLOSEDCASH WHERE MONEY = '" + cash + "'"));
				TerminalConfiguration.load(config, session);
				TerminalConfiguration.createCash(session, logic, id, cash, 1, new Date());
				assertEquals(cash, TerminalConfiguration.activeCash(session, id));
				BackupPolicy.save(session, true, 0, 7, id);
				LocalDateTime now = LocalDateTime.of(2026, 10, 14, 12, 0);
				session.begin();
				assertNotNull(BackupPolicy.claim(session, id, now));
				session.rollback();
				session.begin();
				String claim = BackupPolicy.claim(session, id, now);
				assertNotNull(claim);
				BackupPolicy.release(session, claim, now.toLocalDate(), true);
				session.commit();
				assertNull(BackupPolicy.claim(session, id, now));
			} finally {
				session.close();
			}
		}
	}

	private int count(Connection connection, String sql) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet result = statement.executeQuery()) {
			result.next();
			return result.getInt(1);
		}
	}

	@Test
	void derby() throws Exception {
		verify("jdbc:derby:memory:terminalConfigIT;create=true", null, null);
	}

	@Test
	void mysql() throws Exception {
		verify(System.getProperty("pos.mysql.url", "jdbc:mysql://127.0.0.1:13306/pos"),
				System.getProperty("pos.mysql.user", "root"), System.getProperty("pos.mysql.password", "pos"));
	}

	@Test
	void postgres() throws Exception {
		verify(System.getProperty("pos.postgres.url", "jdbc:postgresql://127.0.0.1:15432/pos"),
				System.getProperty("pos.postgres.user", "pos"), System.getProperty("pos.postgres.password", "pos"));
	}

	private void verify(String url, String user, String password) throws Exception {
		DatabaseMigrator.migrate(url, user, password);
		String id = UUID.randomUUID().toString();
		String legacyHost = "legacy-" + id;
		String location = "location-" + id;
		String cash = UUID.randomUUID().toString();
		Properties legacy = new Properties();
		legacy.setProperty("activecash", cash);
		legacy.setProperty("location", location);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		legacy.storeToXML(bytes, "Legacy terminal");
		try (Connection connection = open(url, user, password)) {
			try (PreparedStatement statement = connection
					.prepareStatement("INSERT INTO LOCATIONS(ID, NAME) VALUES (?, ?)")) {
				statement.setString(1, location);
				statement.setString(2, "Legacy stockroom " + id);
				statement.executeUpdate();
			}
			try (PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO CLOSEDCASH(MONEY, HOST, HOSTSEQUENCE, DATESTART) VALUES (?, ?, 1, CURRENT_TIMESTAMP)")) {
				statement.setString(1, cash);
				statement.setString(2, legacyHost);
				statement.executeUpdate();
			}
			try (PreparedStatement statement = connection
					.prepareStatement("INSERT INTO RESOURCES(ID, NAME, RESTYPE, CONTENT) VALUES (?, ?, 0, ?)")) {
				statement.setString(1, UUID.randomUUID().toString());
				statement.setString(2, legacyHost + "/properties");
				statement.setBytes(3, bytes.toByteArray());
				statement.executeUpdate();
			}
		}
		AppConfig config = new AppConfig();
		config.load(null);
		config.setProperty("terminal.id", id);
		config.setProperty("machine.hostname", legacyHost);
		config.setProperty("machine.printer", "printer:Receipt");
		config.setProperty("user.language", "es");
		Session session = new Session(url, user, password);
		try {
			TerminalConfiguration.load(config, session);
			assertEquals(cash, TerminalConfiguration.activeCash(session, id));
			assertEquals(location, TerminalConfiguration.location(session, id));
			assertEquals("printer:Receipt", config.getProperty("machine.printer"));
			assertEquals("es", config.getProperty("user.language"));
			AppConfig other = new AppConfig();
			other.load(null);
			other.setProperty("terminal.id", UUID.randomUUID().toString());
			other.setProperty("machine.hostname", legacyHost);
			TerminalConfiguration.load(other, session);
			assertNull(TerminalConfiguration.activeCash(session, other.getHost()));
			assertEquals(cash, TerminalConfiguration.activeCash(session, id));
			config.setProperty("machine.printer", "screen");
			TerminalConfiguration.save(config, session);
			TerminalConfiguration.load(config, session);
			assertEquals("screen", config.getProperty("machine.printer"));
			BackupPolicy.save(session, true, 0, 7, id);
			LocalDateTime now = LocalDateTime.of(2026, 10, 14, 12, 0);
			String lease = BackupPolicy.claim(session, id, now);
			assertNotNull(lease);
			assertNull(BackupPolicy.claim(session, id, now));
			Session competing = new Session(url, user, password);
			try {
				assertNull(BackupPolicy.claim(competing, id, now));
			} finally {
				competing.close();
			}
			BackupPolicy.release(session, lease, now.toLocalDate(), true);
			assertNull(BackupPolicy.claim(session, id, now));
			assertNull(BackupPolicy.claim(session, UUID.randomUUID().toString(), now.plusDays(1)));
		} finally {
			session.close();
		}
		DatabaseMigrator.migrate(url, user, password);
		try (Connection connection = open(url, user, password);
				PreparedStatement statement = connection
						.prepareStatement("SELECT HOST FROM CLOSEDCASH WHERE MONEY = ?")) {
			statement.setString(1, cash);
			try (ResultSet result = statement.executeQuery()) {
				assertTrue(result.next());
				assertEquals(id, result.getString(1));
			}
		}
	}

	private Connection open(String url, String user, String password) throws Exception {
		return user == null ? DriverManager.getConnection(url) : DriverManager.getConnection(url, user, password);
	}
}
