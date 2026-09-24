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

		assertThat(result).contains("databaseType=POSTGRESQL", "databaseUserConfigured=true").doesNotContain("alice",
				"secret");
	}

	@Test
	public void logFieldsRemainSingleLineAndParseable() {
		String result = LogSanitizer.field("value with spaces=and\nnewlines");

		assertThat(result).isEqualTo("value_with_spaces_and_newlines");
	}
}
