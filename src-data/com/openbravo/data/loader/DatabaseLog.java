package com.openbravo.data.loader;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Safe diagnostics shared by the low-level database sentence implementations.
 */
final class DatabaseLog {

	private DatabaseLog() {
	}

	static void failure(Logger logger, String operation, int sqlLength, long started, SQLException exception) {
		logger.log(Level.WARNING,
				"event=database_operation_failed operation=" + operation + " sql_length=" + sqlLength + " duration_ms="
						+ (System.currentTimeMillis() - started) + " exception=" + exception.getClass().getName());
	}
}
