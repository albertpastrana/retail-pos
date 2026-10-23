package com.openbravo.data.loader;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

public class DatabaseLoggingTest {

	@Test
	public void failureEventDoesNotContainSqlOrExceptionMessage() {
		Logger logger = Logger.getLogger("database-logging-test");
		List<LogRecord> records = new ArrayList<>();
		Handler handler = new Handler() {
			@Override
			public void publish(LogRecord record) {
				records.add(record);
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() {
			}
		};
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.ALL);
		logger.addHandler(handler);

		try {
			DatabaseLog.failure(logger, "prepared", 42, System.currentTimeMillis(),
					new SQLException("SELECT secret_customer FROM customers"));

			assertThat(records).hasSize(1);
			assertThat(records.get(0).getLevel()).isEqualTo(Level.WARNING);
			assertThat(records.get(0).getMessage()).contains("event=database_operation_failed", "operation=prepared",
					"sql_length=42", "exception=java.sql.SQLException")
					.doesNotContain("SELECT", "secret_customer", "customers");
			assertThat(records.get(0).getThrown()).isNull();
		} finally {
			logger.removeHandler(handler);
		}
	}
}
