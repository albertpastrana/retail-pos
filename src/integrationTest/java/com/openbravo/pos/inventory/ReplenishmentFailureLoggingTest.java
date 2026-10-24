package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.basic.BasicException;
import java.sql.SQLException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class ReplenishmentFailureLoggingTest {
	@Test
	void statusFailureIncludesConstraintButNotFailingRow() {
		Logger logger = Logger.getLogger(ReplenishmentPanel.class.getName());
		StringBuilder captured = new StringBuilder();
		Handler handler = new Handler() {
			@Override
			public void publish(LogRecord record) {
				captured.append(record.getMessage());
				assertTrue(record.getThrown() == null);
			}
			@Override
			public void flush() {
			}
			@Override
			public void close() {
			}
		};
		logger.addHandler(handler);
		try {
			SQLException cause = new SQLException("violates check constraint \"replenishment_status\" "
					+ "Detail: Failing row contains (Customer Private Data)", "23514");
			ReplenishmentPanel.logStatusFailure("change", "DELIVERED", "entry-1", new BasicException(cause));
			assertTrue(captured.toString().contains("status=DELIVERED entryId=entry-1"));
			assertTrue(captured.toString().contains("sqlState=23514"));
			assertTrue(captured.toString().contains("constraint=replenishment_status"));
			assertFalse(captured.toString().contains("Customer Private Data"));
		} finally {
			logger.removeHandler(handler);
		}
	}
}
