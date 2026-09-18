//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//
package com.openbravo.pos.forms;

import java.util.regex.Pattern;

/** Removes credentials from values that may be written to diagnostics. */
public final class LogSanitizer {

	private static final Pattern CREDENTIAL = Pattern.compile("(?i)(password|passwd|pwd|user)=([^&;]*)");

	private LogSanitizer() {
	}

	public static String jdbcUrl(String url) {
		return url == null ? null : CREDENTIAL.matcher(url).replaceAll("$1=<redacted>");
	}
}
