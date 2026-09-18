//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//
package com.openbravo.pos.forms;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/** Single-line, Alloy-friendly formatter with operation correlation fields. */
public final class LogFormatter extends Formatter {

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	@Override
	public String format(LogRecord record) {
		StringBuilder result = new StringBuilder();
		result.append(
				TIMESTAMP.format(OffsetDateTime.ofInstant(new Date(record.getMillis()).toInstant(), ZoneOffset.UTC)));
		result.append(" level=").append(record.getLevel().getName());
		result.append(" logger=").append(record.getLoggerName());
		result.append(" runId=").append(LogContext.getRunId());
		String operationId = LogContext.getOperationId();
		if (operationId != null) {
			result.append(" operationId=").append(operationId);
		}
		result.append(" message=").append(oneLine(formatMessage(record)));
		if (record.getThrown() != null) {
			StringWriter stack = new StringWriter();
			record.getThrown().printStackTrace(new PrintWriter(stack));
			result.append(" exception=").append(oneLine(stack.toString()));
		}
		return result.append(System.lineSeparator()).toString();
	}

	private static String oneLine(String value) {
		return value == null ? "" : value.replace('\r', ' ').replace("\n", "\\n");
	}
}
