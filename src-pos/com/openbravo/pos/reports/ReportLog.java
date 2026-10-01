package com.openbravo.pos.reports;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Callable;
import com.openbravo.pos.forms.LogContext;

/** Structured diagnostics shared by the asynchronous report views. */
final class ReportLog {

	private static final Logger LOGGER = Logger.getLogger(ReportLog.class.getName());

	private ReportLog() {
	}

	static long start(String report) {
		LOGGER.info("event=report_load_start report=" + report);
		return System.currentTimeMillis();
	}

	static void success(String report, int rows, long started) {
		LOGGER.info("event=report_load_success report=" + report + " rows=" + rows + " duration_ms="
				+ (System.currentTimeMillis() - started));
	}

	static void failure(String report, long started, Exception exception) {
		LOGGER.log(Level.WARNING,
				"event=report_load_failed report=" + report + " duration_ms=" + (System.currentTimeMillis() - started),
				exception);
	}

	static <T> T inOperation(Callable<T> work) throws Exception {
		try (LogContext.Scope ignored = LogContext.beginOperation()) {
			return work.call();
		}
	}
}
