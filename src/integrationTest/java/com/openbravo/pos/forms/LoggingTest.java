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
}
