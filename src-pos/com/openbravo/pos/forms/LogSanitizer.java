//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//
package com.openbravo.pos.forms;

import java.util.regex.Pattern;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Removes credentials from values that may be written to diagnostics. */
public final class LogSanitizer {

	private static final Pattern CREDENTIAL = Pattern.compile("(?i)(password|passwd|pwd|user)=([^&;\\s]*)");
	private static final Pattern SECRET = Pattern
			.compile("(?i)(token|secret|authorization|card(number)?|pan)=([^\\s&;,)]*)");
	private static final Pattern URI_CREDENTIAL = Pattern.compile("(?i)(://)[^/@\\s:]+:[^/@\\s]+@");

	private LogSanitizer() {
	}

	public static String jdbcUrl(String url) {
		return url == null ? null : sanitize(url);
	}

	public static String throwable(Throwable throwable) {
		if (throwable == null) {
			return null;
		}
		StringWriter stack = new StringWriter();
		throwable.printStackTrace(new PrintWriter(stack));
		return sanitize(stack.toString());
	}

	public static String sanitize(String value) {
		if (value == null) {
			return null;
		}
		String sanitized = CREDENTIAL.matcher(value).replaceAll("$1=<redacted>");
		sanitized = SECRET.matcher(sanitized).replaceAll("$1=<redacted>");
		return URI_CREDENTIAL.matcher(sanitized).replaceAll("$1<redacted>@");
	}

	/**
	 * Returns only non-sensitive configuration fields suitable for startup logs.
	 */
	public static String configuration(AppProperties properties) {
		String databaseUrl = properties.getProperty("db.URL");
		DatabaseBackup.ConnectionInfo database = DatabaseBackup.parseConnectionInfo(databaseUrl);
		return "databaseType=" + field(String.valueOf(database.getType())) + " databaseHost="
				+ field(database.getHost()) + " databasePort=" + database.getPort() + " databaseName="
				+ field(database.getDatabaseName()) + " databaseDriver=" + field(properties.getProperty("db.driver"))
				+ " databaseUserConfigured=" + configured(properties.getProperty("db.user")) + " hostname="
				+ field(properties.getProperty("machine.hostname")) + " locale="
				+ field(properties.getProperty("user.language")) + "_" + field(properties.getProperty("user.country"))
				+ " lookAndFeel=com.formdev.flatlaf.FlatLightLaf printer="
				+ field(properties.getProperty("machine.printer")) + " screenMode="
				+ field(properties.getProperty("machine.screenmode")) + " scannerConfigured="
				+ configured(properties.getProperty("machine.scanner")) + " demoMode="
				+ field(properties.getProperty("demo.active")) + " backupEnabled="
				+ field(properties.getProperty(DatabaseBackup.BACKUP_DAILY_KEY)) + " backupDirectoryConfigured="
				+ configured(properties.getProperty(DatabaseBackup.BACKUP_DIR_KEY)) + " updateCheck="
				+ field(properties.getProperty("update.check"));
	}

	private static String configured(String value) {
		return value == null || value.trim().isEmpty() || "Not defined".equalsIgnoreCase(value.trim())
				? "false"
				: "true";
	}

	public static String field(String value) {
		if (value == null || value.isEmpty()) {
			return "<unset>";
		}
		return value.replace(' ', '_').replace('=', '_').replace('\n', '_').replace('\r', '_');
	}
}
