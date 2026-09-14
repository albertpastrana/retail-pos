package com.openbravo.pos.forms;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Report queries live in BeanShell strings that nothing else compiles, so a
 * typo or an engine-specific construct only shows up when a shop opens the
 * report. Run each of them, on every engine the till supports.
 */
public class ReportStatementsIT {

	private static final File REPORTS = new File("reports/com/openbravo/reports");

	private static final Pattern SENTENCE = Pattern.compile("report\\.setSentence\\((.*?)\\);", Pattern.DOTALL);

	private static final Pattern LITERAL = Pattern.compile("\"([^\"]*)\"");

	private static final Pattern GLUE = Pattern.compile("[\\s+]");

	/**
	 * Both select columns they do not group by, which only MySQL tolerates. They
	 * were already broken on Derby and PostgreSQL before this test existed.
	 */
	private static final List<String> KNOWN_BROKEN = java.util.Arrays.asList("productsaletotals.bs", "soldproducts.bs");

	private static final long WAIT_MS = 60000L;

	@Test
	public void runOnDerby() throws Exception {
		assertStatementsRun("jdbc:derby:memory:reportStatementsIT;create=true", null, null);
	}

	@Test
	public void runOnMysql() throws Exception {
		String url = System.getProperty("pos.mysql.url", "jdbc:mysql://127.0.0.1:13306/pos");
		String user = System.getProperty("pos.mysql.user", "root");
		String password = System.getProperty("pos.mysql.password", "pos");
		waitFor(url, user, password).close();
		assertStatementsRun(url, user, password);
	}

	@Test
	public void runOnPostgres() throws Exception {
		String url = System.getProperty("pos.postgres.url", "jdbc:postgresql://127.0.0.1:15432/pos");
		String user = System.getProperty("pos.postgres.user", "pos");
		String password = System.getProperty("pos.postgres.password", "pos");
		waitFor(url, user, password).close();
		assertStatementsRun(url, user, password);
	}

	private static void assertStatementsRun(String url, String user, String password) throws Exception {
		Map<String, String> statements = reportStatements();
		assertFalse("No report statements found in " + REPORTS, statements.isEmpty());

		DatabaseMigrator.migrate(url, user, password);
		List<String> failures = new ArrayList<String>();
		Connection connection = open(url, user, password);
		try {
			for (Map.Entry<String, String> statement : statements.entrySet()) {
				try {
					run(connection, statement.getValue());
				} catch (SQLException e) {
					failures.add(statement.getKey() + ": " + e.getMessage());
				}
			}
		} finally {
			connection.close();
		}
		assertTrue(failures.toString(), failures.isEmpty());
	}

	private static void run(Connection connection, String sql) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(sql);
		try {
			ResultSet rows = statement.executeQuery();
			rows.close();
		} finally {
			statement.close();
		}
	}

	/**
	 * The filter placeholder is expanded by QBFBuilder at runtime from whatever the
	 * operator typed; "1=1" stands in for it here. Sentences glued together from
	 * runtime calls rather than plain literals are skipped, since there is no
	 * statement to read without running the script.
	 */
	private static Map<String, String> reportStatements() throws IOException {
		Map<String, String> statements = new LinkedHashMap<String, String>();
		File[] scripts = REPORTS.listFiles();
		if (scripts == null) {
			return statements;
		}
		java.util.Arrays.sort(scripts);
		for (File script : scripts) {
			if (!script.getName().endsWith(".bs") || KNOWN_BROKEN.contains(script.getName())) {
				continue;
			}
			String source = new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8);
			Matcher sentence = SENTENCE.matcher(source);
			if (!sentence.find()) {
				continue;
			}
			String argument = sentence.group(1);
			if (!GLUE.matcher(LITERAL.matcher(argument).replaceAll("")).replaceAll("").isEmpty()) {
				continue;
			}
			StringBuilder sql = new StringBuilder();
			Matcher literal = LITERAL.matcher(argument);
			while (literal.find()) {
				sql.append(literal.group(1));
			}
			statements.put(script.getName(), sql.toString().replace("?(QBF_FILTER)", "1=1"));
		}
		return statements;
	}

	private static Connection waitFor(String url, String user, String password) {
		long deadline = System.currentTimeMillis() + WAIT_MS;
		SQLException last = null;
		while (System.currentTimeMillis() < deadline) {
			try {
				return open(url, user, password);
			} catch (SQLException e) {
				last = e;
				try {
					Thread.sleep(500L);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					throw new AssertionError("Interrupted waiting for " + url, interrupted);
				}
			}
		}
		throw new AssertionError("Database not reachable: " + url, last);
	}

	private static Connection open(String url, String user, String password) throws SQLException {
		if (user == null || user.isEmpty()) {
			return DriverManager.getConnection(url);
		}
		return DriverManager.getConnection(url, user, password);
	}
}
