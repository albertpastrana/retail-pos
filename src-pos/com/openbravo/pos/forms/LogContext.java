//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//
package com.openbravo.pos.forms;

import java.util.UUID;

/** Lightweight per-thread context for correlating one POS operation. */
public final class LogContext {

	private static final String RUN_ID = UUID.randomUUID().toString();
	private static final ThreadLocal<String> OPERATION_ID = new ThreadLocal<String>();

	private LogContext() {
	}

	public static String getRunId() {
		return RUN_ID;
	}

	public static String getOperationId() {
		return OPERATION_ID.get();
	}

	public static Scope beginOperation() {
		String previous = OPERATION_ID.get();
		String operationId = UUID.randomUUID().toString();
		OPERATION_ID.set(operationId);
		return new Scope(previous);
	}

	public static final class Scope implements AutoCloseable {
		private final String previous;

		private Scope(String previous) {
			this.previous = previous;
		}

		@Override
		public void close() {
			if (previous == null) {
				OPERATION_ID.remove();
			} else {
				OPERATION_ID.set(previous);
			}
		}
	}
}
