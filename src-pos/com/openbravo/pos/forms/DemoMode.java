package com.openbravo.pos.forms;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.util.AltEncrypter;

/** Creates and manages the database used by the local demo mode. */
public final class DemoMode {
	public static final String ACTIVE_KEY = "demo.active";
	public static final String URL_KEY = "demo.db.URL";
	public static final String USER_KEY = "demo.db.user";
	public static final String PASSWORD_KEY = "demo.db.password";
	public static final String SOURCE_KEY = "demo.source";
	private static final String PRODUCTION_URL_KEY = "production.db.URL";
	private static final String PRODUCTION_USER_KEY = "production.db.user";
	private static final String PRODUCTION_PASSWORD_KEY = "production.db.password";

	private DemoMode() {
	}

	public static boolean isActive(AppProperties props) {
		return "true".equalsIgnoreCase(props.getProperty(ACTIVE_KEY));
	}

	public static void removeIncompleteDerbyDatabase(Properties props) {
		String url = props.getProperty("db.URL");
		if (!"true".equalsIgnoreCase(props.getProperty(ACTIVE_KEY)) || url == null || !url.startsWith("jdbc:derby:")) {
			return;
		}
		Path path = derbyPath(url);
		File database = path.toFile();
		if (!database.exists() || new File(database, "service.properties").isFile()) {
			return;
		}
		try {
			deleteRecursively(path);
		} catch (IOException e) {
			// The normal connection error contains the path and remains actionable.
		}
	}

	public static void prepare(AppConfig props, boolean copyProduction, boolean recreate) throws BasicException {
		String existingUrl = props.getProperty(URL_KEY);
		if (!recreate && existingUrl != null && !existingUrl.trim().isEmpty()) {
			props.setProperty(ACTIVE_KEY, "true");
			save(props, "Cannot save demo mode configuration");
			return;
		}

		String productionUrl = props.getProperty("db.URL");
		String user = props.getProperty("db.user");
		props.setProperty(PRODUCTION_URL_KEY, productionUrl);
		props.setProperty(PRODUCTION_USER_KEY, user);
		props.setProperty(PRODUCTION_PASSWORD_KEY, props.getProperty("db.password"));
		String password = decryptPassword(user, props.getProperty("db.password"));
		DatabaseBackup.ConnectionInfo info = DatabaseBackup.parseConnectionInfo(productionUrl);
		String demoUrl = demoUrl(productionUrl, info.getDatabaseName() + "_demo");

		switch (info.getType()) {
			case POSTGRESQL :
				preparePostgres(props, info, user, password, copyProduction, recreate);
				break;
			case MYSQL :
				prepareMySql(productionUrl, info, user, password, copyProduction, recreate);
				break;
			case DERBY :
				prepareDerby(productionUrl, demoUrl, user, password, copyProduction, recreate);
				break;
			default :
				throw new BasicException("Unsupported database driver for demo mode: " + productionUrl);
		}

		props.setProperty(URL_KEY, demoUrl);
		props.setProperty(USER_KEY, user == null ? "" : user);
		props.setProperty(PASSWORD_KEY, props.getProperty("db.password"));
		props.setProperty(SOURCE_KEY, copyProduction ? "production" : "empty");
		props.setProperty(ACTIVE_KEY, "true");
		save(props, "Cannot save demo mode configuration");
	}

	public static void exit(AppConfig props) throws BasicException {
		props.setProperty(ACTIVE_KEY, "false");
		String productionUrl = props.getProperty(PRODUCTION_URL_KEY);
		if (productionUrl != null) {
			props.setProperty("db.URL", productionUrl);
		}
		String productionUser = props.getProperty(PRODUCTION_USER_KEY);
		if (productionUser != null) {
			props.setProperty("db.user", productionUser);
		}
		String productionPassword = props.getProperty(PRODUCTION_PASSWORD_KEY);
		if (productionPassword != null) {
			props.setProperty("db.password", productionPassword);
		}
		save(props, "Cannot save production mode configuration");
	}

	private static void preparePostgres(AppProperties props, DatabaseBackup.ConnectionInfo info, String user,
			String password, boolean copy, boolean recreate) throws BasicException {
		String productionUrl = props.getProperty("db.URL");
		String adminUrl = demoUrl(productionUrl, "postgres");
		String demoName = info.getDatabaseName() + "_demo";
		if (recreate) {
			executeDatabaseStatement(adminUrl, user, password, "DROP DATABASE IF EXISTS " + quote(demoName));
		}
		executeDatabaseStatement(adminUrl, user, password, "CREATE DATABASE " + quote(demoName));
		if (copy) {
			File dump = dumpPostgres(info, user, password);
			runRestore(info, user, password, demoName, dump);
			if (!dump.delete()) {
				dump.deleteOnExit();
			}
		}
	}

	private static void prepareMySql(String productionUrl, DatabaseBackup.ConnectionInfo info, String user,
			String password, boolean copy, boolean recreate) throws BasicException {
		String adminUrl = demoUrl(productionUrl, "");
		String demoName = info.getDatabaseName() + "_demo";
		if (recreate) {
			executeDatabaseStatement(adminUrl, user, password, "DROP DATABASE IF EXISTS " + quoteMySql(demoName));
		}
		executeDatabaseStatement(adminUrl, user, password, "CREATE DATABASE IF NOT EXISTS " + quoteMySql(demoName));
		if (copy) {
			File dump = dumpMySql(info, user, password);
			restoreMySql(info, user, password, demoName, dump);
			if (!dump.delete()) {
				dump.deleteOnExit();
			}
		}
	}

	private static void prepareDerby(String productionUrl, String demoUrl, String user, String password, boolean copy,
			boolean recreate) throws BasicException {
		Path target = derbyPath(demoUrl);
		try {
			if (recreate) {
				deleteRecursively(target);
			}
			if (!copy) {
				return;
			}

			Path backup = Files.createTempDirectory("retail-pos-derby-demo-");
			deleteRecursively(backup);
			try (Connection connection = DriverManager.getConnection(productionUrl, user, password);
					CallableStatement statement = connection.prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)")) {
				statement.setString(1, backup.toAbsolutePath().toString());
				statement.execute();
			}
			Path backupDatabase = backup.resolve(derbyPath(productionUrl).getFileName());
			copyRecursively(Files.exists(backupDatabase) ? backupDatabase : backup, target);
			deleteRecursively(backup);
		} catch (IOException | SQLException e) {
			throw new BasicException("Cannot copy the Derby production database", e);
		}
	}

	private static Path derbyPath(String url) {
		String path = url.substring("jdbc:derby:".length());
		int options = path.indexOf(';');
		return new File(options < 0 ? path : path.substring(0, options)).toPath();
	}

	private static void copyRecursively(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
					throws IOException {
				Files.createDirectories(target.resolve(source.relativize(directory)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				Files.copy(file, target.resolve(source.relativize(file)));
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static void deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
				Files.delete(directory);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static File dumpPostgres(DatabaseBackup.ConnectionInfo info, String user, String password)
			throws BasicException {
		File target = new File(System.getProperty("java.io.tmpdir"), "retail-pos-demo.sql");
		List<String> command = DatabaseBackup.buildPgDumpCommand("pg_dump", info, user, target);
		ProcessBuilder process = new ProcessBuilder(command);
		if (password != null && !password.isEmpty()) {
			process.environment().put("PGPASSWORD", password);
		}
		runProcess(process, "pg_dump");
		return target;
	}

	private static void runRestore(DatabaseBackup.ConnectionInfo info, String user, String password, String database,
			File dump) throws BasicException {
		List<String> command = new ArrayList<String>();
		command.add("psql");
		command.add("-h");
		command.add(info.getHost());
		command.add("-p");
		command.add(String.valueOf(info.getPort()));
		command.add("-d");
		command.add(database);
		command.add("-f");
		command.add(dump.getAbsolutePath());
		ProcessBuilder process = new ProcessBuilder(command);
		if (password != null && !password.isEmpty()) {
			process.environment().put("PGPASSWORD", password);
		}
		runProcess(process, "psql");
	}

	private static File dumpMySql(DatabaseBackup.ConnectionInfo info, String user, String password)
			throws BasicException {
		File target = new File(System.getProperty("java.io.tmpdir"), "retail-pos-demo.sql");
		List<String> command = new ArrayList<String>();
		command.add("mysqldump");
		command.add("-h");
		command.add(info.getHost());
		command.add("-P");
		command.add(String.valueOf(info.getPort()));
		if (user != null && !user.isEmpty()) {
			command.add("-u");
			command.add(user);
		}
		command.add("-r");
		command.add(target.getAbsolutePath());
		command.add(info.getDatabaseName());
		ProcessBuilder process = new ProcessBuilder(command);
		if (password != null && !password.isEmpty()) {
			process.environment().put("MYSQL_PWD", password);
		}
		runProcess(process, "mysqldump");
		return target;
	}

	private static void restoreMySql(DatabaseBackup.ConnectionInfo info, String user, String password, String database,
			File dump) throws BasicException {
		List<String> command = new ArrayList<String>();
		command.add("mysql");
		command.add("-h");
		command.add(info.getHost());
		command.add("-P");
		command.add(String.valueOf(info.getPort()));
		if (user != null && !user.isEmpty()) {
			command.add("-u");
			command.add(user);
		}
		command.add(database);
		ProcessBuilder process = new ProcessBuilder(command);
		process.redirectInput(dump);
		if (password != null && !password.isEmpty()) {
			process.environment().put("MYSQL_PWD", password);
		}
		runProcess(process, "mysql");
	}

	private static void executeDatabaseStatement(String url, String user, String password, String sql)
			throws BasicException {
		try (Connection connection = DriverManager.getConnection(url, user, password);
				Statement statement = connection.createStatement()) {
			statement.execute(sql);
		} catch (SQLException e) {
			String error = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
			if (!sql.startsWith("CREATE DATABASE") || !error.contains("already exists")) {
				throw new BasicException("Cannot prepare demo database: " + e.getMessage(), e);
			}
		}
	}

	private static String demoUrl(String url, String database) {
		int slash = url.lastIndexOf('/');
		int query = url.indexOf('?', slash);
		int options = url.indexOf(';', slash);
		int suffixStart = query < 0 ? options : options < 0 ? query : Math.min(query, options);
		String suffix = suffixStart < 0 ? "" : url.substring(suffixStart);
		return url.substring(0, slash + 1) + database + suffix;
	}

	private static String quote(String identifier) {
		return "\"" + identifier.replace("\"", "\"\"") + "\"";
	}

	private static String quoteMySql(String identifier) {
		return "`" + identifier.replace("`", "``") + "`";
	}

	private static String decryptPassword(String user, String password) {
		if (password != null && password.startsWith("crypt:") && user != null) {
			return new AltEncrypter("cypherkey" + user).decrypt(password.substring(6));
		}
		return password == null ? "" : password;
	}

	private static void save(AppConfig props, String message) throws BasicException {
		try {
			props.save();
		} catch (IOException e) {
			throw new BasicException(message, e);
		}
	}

	private static void runProcess(ProcessBuilder builder, String command) throws BasicException {
		try {
			Process process = builder.start();
			int exit = process.waitFor();
			if (exit != 0) {
				throw new BasicException(command + " failed with exit code " + exit);
			}
		} catch (IOException e) {
			throw new BasicException("Cannot run " + command, e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BasicException(command + " was interrupted", e);
		}
	}
}
