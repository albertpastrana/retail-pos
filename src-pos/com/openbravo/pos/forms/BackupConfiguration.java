package com.openbravo.pos.forms;

/** Typed database backup settings. */
public final class BackupConfiguration {
	private final String directory;
	private final boolean daily;
	private final String pgDump;
	private final String lastDate;

	private BackupConfiguration(String directory, boolean daily, String pgDump, String lastDate) {
		this.directory = directory;
		this.daily = daily;
		this.pgDump = pgDump;
		this.lastDate = lastDate;
	}

	public static BackupConfiguration from(AppProperties properties) {
		return new BackupConfiguration(properties.getProperty(DatabaseBackup.BACKUP_DIR_KEY),
				"true".equalsIgnoreCase(properties.getProperty(DatabaseBackup.BACKUP_DAILY_KEY)),
				properties.getProperty(DatabaseBackup.BACKUP_PG_DUMP_KEY),
				properties.getProperty(DatabaseBackup.BACKUP_LASTDATE_KEY));
	}

	public String getDirectory() {
		return directory;
	}

	public boolean isDaily() {
		return daily;
	}

	public String getPgDump() {
		return pgDump;
	}

	public String getLastDate() {
		return lastDate;
	}
}
