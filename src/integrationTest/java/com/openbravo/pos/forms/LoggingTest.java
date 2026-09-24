package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

public class LoggingTest {

	@Test
	public void formatterIncludesCorrelationFieldsAndKeepsRecordOnOneLine() {
		LogFormatter formatter = new LogFormatter();
		LogRecord record = new LogRecord(Level.WARNING, "message\nwith newline");
		record.setLoggerName("test.logger");

		String formatted = formatter.format(record);

		assertTrue(formatted.contains("level=WARNING"));
		assertTrue(formatted.contains("logger=test.logger"));
		assertTrue(formatted.contains("runId="));
		assertTrue(formatted.contains("message=message\\nwith newline"));
		assertFalse(formatted.substring(0, formatted.length() - System.lineSeparator().length())
				.contains(System.lineSeparator()));
	}

	@Test
	public void operationContextIsRestoredAfterScope() {
		assertTrue(LogContext.getOperationId() == null);
		try (LogContext.Scope ignored = LogContext.beginOperation()) {
			assertTrue(LogContext.getOperationId() != null);
		}
		assertTrue(LogContext.getOperationId() == null);
	}

	@Test
	public void jdbcCredentialsAreRedacted() {
		String result = LogSanitizer.jdbcUrl("jdbc:postgresql://db/pos?user=alice&password=secret");

		assertTrue(result.contains("user=<redacted>"));
		assertTrue(result.contains("password=<redacted>"));
		assertFalse(result.contains("secret"));
	}

	@Test
	public void configurationSummaryDoesNotExposeCredentials() {
		AppConfig config = new AppConfig();
		config.setProperty("db.URL", "jdbc:postgresql://db:5432/pos?user=alice&password=secret");
		config.setProperty("db.driver", "org.postgresql.Driver");
		config.setProperty("db.user", "alice");
		config.setProperty("db.password", "secret");
		config.setProperty("machine.hostname", "till-1");
		config.setProperty("user.language", "ca");
		config.setProperty("user.country", "ES");

		String result = LogSanitizer.configuration(config);

		assertTrue(result.contains("databaseType=POSTGRESQL"));
		assertTrue(result.contains("databaseUserConfigured=true"));
		assertFalse(result.contains("alice"));
		assertFalse(result.contains("secret"));
	}

	@Test
	public void logFieldsRemainSingleLineAndParseable() {
		String result = LogSanitizer.field("value with spaces=and\nnewlines");

		assertTrue(result.equals("value_with_spaces_and_newlines"));
	}
}
