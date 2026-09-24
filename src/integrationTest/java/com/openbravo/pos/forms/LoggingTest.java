package com.openbravo.pos.forms;

import static org.assertj.core.api.Assertions.assertThat;

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

		assertThat(formatted).contains("level=WARNING", "logger=test.logger", "runId=",
				"message=message\\nwith newline");
		assertThat(formatted.substring(0, formatted.length() - System.lineSeparator().length()))
				.doesNotContain(System.lineSeparator());
	}

	@Test
	public void operationContextIsRestoredAfterScope() {
		assertThat(LogContext.getOperationId()).isNull();
		try (LogContext.Scope ignored = LogContext.beginOperation()) {
			assertThat(LogContext.getOperationId()).isNotNull();
		}
		assertThat(LogContext.getOperationId()).isNull();
	}

	@Test
	public void jdbcCredentialsAreRedacted() {
		String result = LogSanitizer.jdbcUrl("jdbc:postgresql://db/pos?user=alice&password=secret");

		assertThat(result).contains("user=<redacted>", "password=<redacted>").doesNotContain("secret");
	}
}
