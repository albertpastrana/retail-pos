package com.openbravo.pos.forms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.util.AltEncrypter;

public final class DatabaseBackup {

	private static final Logger logger = Logger.getLogger(DatabaseBackup.class.getName());

	public static final String BACKUP_DIR_KEY = "backup.dir";
	public static final String BACKUP_DAILY_KEY = "backup.daily";
	public static final String BACKUP_PG_DUMP_KEY = "backup.pg_dump";
	public static final String BACKUP_LASTDATE_KEY = "backup.lastdate";

	public enum DatabaseType {
		POSTGRESQL, MYSQL, DERBY, UNKNOWN
	}

	public static class ConnectionInfo {
		private final DatabaseType type;
		private final String host;
		private final int port;
		private final String databaseName;

		public ConnectionInfo(DatabaseType type, String host, int port, String databaseName) {
			this.type = type;
			this.host = host;
			this.port = port;
			this.databaseName = databaseName;
		}

		public DatabaseType getType() {
			return type;
		}

		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		public String getDatabaseName() {
			return databaseName;
		}
	}

	private DatabaseBackup() {
	}

	public static File backup(AppProperties props) throws BasicException {
		long started = System.currentTimeMillis();
		BackupConfiguration backup = BackupConfiguration.from(props);
		DatabaseConfiguration database = DatabaseConfiguration.from(props);
		String backupDir = backup.getDirectory();
		if (backupDir == null || backupDir.trim().isEmpty()) {
			throw new BasicException(AppLocal.getIntString("message.backupnodir"));
		}

		File dir = new File(backupDir.trim());
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new BasicException("Cannot create backup directory: " + dir.getAbsolutePath());
			}
		}
		if (!dir.isDirectory() || !dir.canWrite()) {
			throw new BasicException("Backup directory is not writable: " + dir.getAbsolutePath());
		}

		String url = database.getUrl();
		if (url == null || url.trim().isEmpty()) {
			throw new BasicException("Database URL is not configured");
		}

		String user = database.getUser();
		if (user == null) {
			user = "";
		}

		String password = database.getPassword();
		if (password != null && password.startsWith("crypt:")) {
			AltEncrypter cypher = new AltEncrypter("cypherkey" + user);
			password = cypher.decrypt(password.substring(6));
		}
		if (password == null) {
			password = "";
		}

		ConnectionInfo info = parseConnectionInfo(url);
		logger.info("event=backup_start databaseType=" + info.getType() + " databaseHost=" + info.getHost()
				+ " databaseName=" + LogSanitizer.field(info.getDatabaseName()) + " directoryConfigured=true");
		String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

		File resultFile;
		switch (info.getType()) {
			case POSTGRESQL :
				resultFile = backupPostgres(backup, info, user, password, dir, timestamp);
				break;
			case MYSQL :
				resultFile = backupMySQL(props, info, user, password, dir, timestamp);
				break;
			case DERBY :
				resultFile = backupDerby(url, user, password, info, dir, timestamp);
				break;
			default :
				throw new BasicException("Unsupported database driver for backup: " + url);
		}

		String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
		if (props instanceof AppConfig) {
			((AppConfig) props).setProperty(BACKUP_LASTDATE_KEY, today);
		}

		logger.info("event=backup_success databaseType=" + info.getType() + " duration_ms="
				+ (System.currentTimeMillis() - started));
		return resultFile;
	}

	public static boolean isDailyBackupNeeded(AppProperties props) {
		BackupConfiguration backup = BackupConfiguration.from(props);
		if (!backup.isDaily()) {
			return false;
		}

		String backupDir = backup.getDirectory();
		if (backupDir == null || backupDir.trim().isEmpty()) {
			return false;
		}

		File dir = new File(backupDir.trim());
		if (!dir.exists() || !dir.isDirectory()) {
			return true;
		}

		String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
		String lastDate = backup.getLastDate();
		if (today.equals(lastDate)) {
			return false;
		}

		return !isDailyBackupFilePresent(dir, today);
	}

	public static boolean isDailyBackupFilePresent(File dir, final String datePattern) {
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			return false;
		}
		File[] matching = dir.listFiles(new FilenameFilter() {
			public boolean accept(File d, String name) {
				return name.contains(datePattern);
			}
		});
		return matching != null && matching.length > 0;
	}

	public static void runDailyBackupIfDue(final AppProperties props) {
		if (!isDailyBackupNeeded(props)) {
			return;
		}

		Thread backupThread = new Thread(new Runnable() {
			public void run() {
				try (LogContext.Scope ignored = LogContext.beginOperation()) {
					File target = backup(props);
					if (props instanceof AppConfig) {
						ConfigurationStore.save((AppConfig) props);
					}
					logger.info("event=backup_daily_success databaseType="
							+ parseConnectionInfo(props.getProperty("db.URL")).getType());
				} catch (Exception e) {
					logger.log(Level.WARNING, "event=backup_daily_failed", e);
				}
			}
		}, "DailyDatabaseBackupThread");
		backupThread.setDaemon(true);
		backupThread.start();
	}

	public static ConnectionInfo parseConnectionInfo(String url) {
		if (url == null) {
			return new ConnectionInfo(DatabaseType.UNKNOWN, "localhost", 0, "");
		}

		String cleanUrl = url.trim();
		if (cleanUrl.startsWith("jdbc:postgresql:")) {
			return parseNetworkUrl(cleanUrl.substring("jdbc:postgresql:".length()), DatabaseType.POSTGRESQL, 5432);
		} else if (cleanUrl.startsWith("jdbc:mysql:")) {
			return parseNetworkUrl(cleanUrl.substring("jdbc:mysql:".length()), DatabaseType.MYSQL, 3306);
		} else if (cleanUrl.startsWith("jdbc:derby:")) {
			String path = cleanUrl.substring("jdbc:derby:".length());
			int semicolon = path.indexOf(';');
			if (semicolon >= 0) {
				path = path.substring(0, semicolon);
			}
			String name = new File(path).getName();
			if (name.isEmpty()) {
				name = "derby";
			}
			return new ConnectionInfo(DatabaseType.DERBY, "localhost", 0, name);
		}

		return new ConnectionInfo(DatabaseType.UNKNOWN, "localhost", 0, "");
	}

	private static ConnectionInfo parseNetworkUrl(String remaining, DatabaseType type, int defaultPort) {
		int question = remaining.indexOf('?');
		if (question >= 0) {
			remaining = remaining.substring(0, question);
		}

		String host = "localhost";
		int port = defaultPort;
		String dbName = "";

		if (remaining.startsWith("//")) {
			String hostAndDb = remaining.substring(2);
			int slash = hostAndDb.indexOf('/');
			if (slash >= 0) {
				String hostPort = hostAndDb.substring(0, slash);
				dbName = hostAndDb.substring(slash + 1);
				int colon = hostPort.indexOf(':');
				if (colon >= 0) {
					host = hostPort.substring(0, colon);
					try {
						port = Integer.parseInt(hostPort.substring(colon + 1));
					} catch (NumberFormatException e) {
						port = defaultPort;
					}
				} else if (!hostPort.isEmpty()) {
					host = hostPort;
				}
			} else {
				dbName = hostAndDb;
			}
		} else {
			dbName = remaining;
		}

		if (dbName.isEmpty()) {
			dbName = "pos";
		}

		return new ConnectionInfo(type, host, port, dbName);
	}

	public static List<String> buildPgDumpCommand(String pgDumpExecutable, ConnectionInfo info, String user,
			File targetFile) {
		List<String> cmd = new ArrayList<String>();
		cmd.add(pgDumpExecutable);
		cmd.add("-h");
		cmd.add(info.getHost());
		cmd.add("-p");
		cmd.add(String.valueOf(info.getPort()));
		if (user != null && !user.isEmpty()) {
			cmd.add("-U");
			cmd.add(user);
		}
		cmd.add("-f");
		cmd.add(targetFile.getAbsolutePath());
		cmd.add(info.getDatabaseName());
		return cmd;
	}

	private static File backupPostgres(BackupConfiguration backup, ConnectionInfo info, String user, String password,
			File dir, String timestamp) throws BasicException {
		String pgDump = findExecutable(backup.getPgDump(), "pg_dump",
				new String[]{"/usr/bin/pg_dump", "/usr/local/bin/pg_dump", "/opt/homebrew/bin/pg_dump",
						"/opt/homebrew/opt/libpq/bin/pg_dump", "/usr/lib/postgresql/16/bin/pg_dump",
						"/usr/lib/postgresql/15/bin/pg_dump", "/usr/lib/postgresql/14/bin/pg_dump"});

		if (pgDump == null) {
			throw new BasicException(
					"Cannot find pg_dump executable in PATH or standard locations. Please install PostgreSQL client tools or set backup.pg_dump.");
		}

		File targetFile = new File(dir, "backup-" + info.getDatabaseName() + "-" + timestamp + ".sql.gz");
		File sqlFile = new File(dir, targetFile.getName().substring(0, targetFile.getName().length() - 3));
		try {
			List<String> cmd = buildPgDumpCommand(pgDump, info, user, sqlFile);
			ProcessBuilder pb = new ProcessBuilder(cmd);
			if (password != null && !password.isEmpty()) {
				pb.environment().put("PGPASSWORD", password);
			}
			if (user != null && !user.isEmpty()) {
				pb.environment().put("PGUSER", user);
			}

			runProcess(pb, "pg_dump");
			gzip(sqlFile, targetFile);
			return targetFile;
		} finally {
			deleteFile(sqlFile, "temporary PostgreSQL dump");
		}
	}

	private static File backupMySQL(AppProperties props, ConnectionInfo info, String user, String password, File dir,
			String timestamp) throws BasicException {
		String mysqldump = findExecutable(null, "mysqldump",
				new String[]{"/usr/bin/mysqldump", "/usr/local/bin/mysqldump", "/opt/homebrew/bin/mysqldump"});

		if (mysqldump == null) {
			throw new BasicException(
					"Cannot find mysqldump executable in PATH or standard locations. Please install MySQL client tools.");
		}

		File targetFile = new File(dir, "backup-" + info.getDatabaseName() + "-" + timestamp + ".sql.gz");
		File sqlFile = new File(dir, targetFile.getName().substring(0, targetFile.getName().length() - 3));
		List<String> cmd = new ArrayList<String>();
		cmd.add(mysqldump);
		cmd.add("-h");
		cmd.add(info.getHost());
		cmd.add("-P");
		cmd.add(String.valueOf(info.getPort()));
		if (user != null && !user.isEmpty()) {
			cmd.add("-u");
			cmd.add(user);
		}
		cmd.add("-r");
		cmd.add(sqlFile.getAbsolutePath());
		cmd.add(info.getDatabaseName());

		ProcessBuilder pb = new ProcessBuilder(cmd);
		if (password != null && !password.isEmpty()) {
			pb.environment().put("MYSQL_PWD", password);
		}

		try {
			runProcess(pb, "mysqldump");
			gzip(sqlFile, targetFile);
			return targetFile;
		} finally {
			deleteFile(sqlFile, "temporary MySQL dump");
		}
	}

	private static File backupDerby(String url, String user, String password, ConnectionInfo info, File dir,
			String timestamp) throws BasicException {
		File targetDir = new File(dir, "backup-" + info.getDatabaseName() + "-" + timestamp);
		File targetFile = new File(dir, targetDir.getName() + ".tar.gz");
		try {
			Connection conn;
			if (user != null && !user.isEmpty()) {
				conn = DriverManager.getConnection(url, user, password);
			} else {
				conn = DriverManager.getConnection(url);
			}
			try {
				CallableStatement cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)");
				try {
					cs.setString(1, targetDir.getAbsolutePath());
					cs.execute();
				} finally {
					cs.close();
				}
			} finally {
				conn.close();
			}
			String tar = findExecutable(null, "tar", new String[]{"/usr/bin/tar", "/bin/tar"});
			if (tar == null) {
				throw new BasicException("Cannot find tar executable to compress the Derby backup.");
			}
			runProcess(new ProcessBuilder(tar, "-czf", targetFile.getAbsolutePath(), "-C", dir.getAbsolutePath(),
					targetDir.getName()), "tar");
			return targetFile;
		} catch (SQLException e) {
			throw new BasicException("Derby backup failed: " + e.getMessage(), e);
		} finally {
			deleteRecursively(targetDir);
		}
	}

	private static void gzip(File source, File target) throws BasicException {
		File staging = new File(target.getAbsolutePath() + ".tmp");
		try {
			try (FileInputStream input = new FileInputStream(source);
					GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(staging))) {
				byte[] buffer = new byte[8192];
				int read;
				while ((read = input.read(buffer)) >= 0) {
					output.write(buffer, 0, read);
				}
			}
			Files.move(staging.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new BasicException("Could not gzip database backup: " + e.getMessage(), e);
		} finally {
			deleteFile(staging, "temporary compressed backup");
		}
	}

	private static void deleteFile(File file, String description) {
		if (file.exists() && !file.delete()) {
			logger.warning("Could not delete " + description + ": " + file.getAbsolutePath());
		}
	}

	private static void deleteRecursively(File file) {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteRecursively(child);
				}
			}
		}
		deleteFile(file, "temporary Derby backup");
	}

	private static String findExecutable(String preferred, String name, String[] standardPaths) {
		if (preferred != null && !preferred.trim().isEmpty()) {
			File f = new File(preferred.trim());
			if (f.exists() && f.canExecute()) {
				return f.getAbsolutePath();
			}
		}

		// Try PATH
		try {
			Process p = new ProcessBuilder("which", name).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = reader.readLine();
			p.waitFor();
			if (p.exitValue() == 0 && line != null && !line.trim().isEmpty()) {
				File f = new File(line.trim());
				if (f.exists() && f.canExecute()) {
					return f.getAbsolutePath();
				}
			}
		} catch (Exception e) {
			// ignore and continue
		}

		for (String path : standardPaths) {
			File f = new File(path);
			if (f.exists() && f.canExecute()) {
				return f.getAbsolutePath();
			}
		}

		return null;
	}

	private static void runProcess(ProcessBuilder pb, String commandName) throws BasicException {
		try {
			Process process = pb.start();
			String errorOutput = readStream(process.getErrorStream());
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				String msg = commandName + " exited with code " + exitCode;
				if (!errorOutput.trim().isEmpty()) {
					msg += ": " + errorOutput.trim();
				}
				throw new BasicException(msg);
			}
		} catch (IOException e) {
			throw new BasicException("Failed to run " + commandName + ": " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BasicException(commandName + " process interrupted", e);
		}
	}

	private static String readStream(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append('\n');
		}
		return sb.toString();
	}
}
