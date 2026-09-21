package com.openbravo.pos.forms;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.openbravo.pos.forms.DatabaseBackup.ConnectionInfo;
import com.openbravo.pos.forms.DatabaseBackup.DatabaseType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseBackupTest {

	@TempDir
	public File temp;

	@Test
	public void parsesPostgresUrls() {
		ConnectionInfo info = DatabaseBackup
				.parseConnectionInfo("jdbc:postgresql://192.168.1.100:5433/retailpos?sslmode=disable");
		assertEquals(DatabaseType.POSTGRESQL, info.getType());
		assertEquals("192.168.1.100", info.getHost());
		assertEquals(5433, info.getPort());
		assertEquals("retailpos", info.getDatabaseName());

		ConnectionInfo defaultPort = DatabaseBackup.parseConnectionInfo("jdbc:postgresql://dbserver/pos");
		assertEquals(DatabaseType.POSTGRESQL, defaultPort.getType());
		assertEquals("dbserver", defaultPort.getHost());
		assertEquals(5432, defaultPort.getPort());
		assertEquals("pos", defaultPort.getDatabaseName());

		ConnectionInfo localSimple = DatabaseBackup.parseConnectionInfo("jdbc:postgresql:testdb");
		assertEquals(DatabaseType.POSTGRESQL, localSimple.getType());
		assertEquals("localhost", localSimple.getHost());
		assertEquals(5432, localSimple.getPort());
		assertEquals("testdb", localSimple.getDatabaseName());
	}

	@Test
	public void parsesMySqlAndDerbyUrls() {
		ConnectionInfo mysql = DatabaseBackup.parseConnectionInfo("jdbc:mysql://mysqlhost:3307/mysqldb?useSSL=false");
		assertEquals(DatabaseType.MYSQL, mysql.getType());
		assertEquals("mysqlhost", mysql.getHost());
		assertEquals(3307, mysql.getPort());
		assertEquals("mysqldb", mysql.getDatabaseName());

		ConnectionInfo derby = DatabaseBackup.parseConnectionInfo("jdbc:derby:/path/to/myposdb;create=true");
		assertEquals(DatabaseType.DERBY, derby.getType());
		assertEquals("myposdb", derby.getDatabaseName());
	}

	@Test
	public void buildsPgDumpCommandLine() {
		ConnectionInfo info = new ConnectionInfo(DatabaseType.POSTGRESQL, "dbhost", 5432, "posdb");
		File target = new File("/tmp/backup.sql");
		List<String> cmd = DatabaseBackup.buildPgDumpCommand("/usr/bin/pg_dump", info, "posuser", target);

		assertEquals(10, cmd.size());
		assertEquals("/usr/bin/pg_dump", cmd.get(0));
		assertEquals("-h", cmd.get(1));
		assertEquals("dbhost", cmd.get(2));
		assertEquals("-p", cmd.get(3));
		assertEquals("5432", cmd.get(4));
		assertEquals("-U", cmd.get(5));
		assertEquals("posuser", cmd.get(6));
		assertEquals("-f", cmd.get(7));
		assertEquals(target.getAbsolutePath(), cmd.get(8));
		assertEquals("posdb", cmd.get(9));
	}

	@Test
	public void checksDailyBackupStatus() throws Exception {
		File backupDir = new File(temp, "backups");
		assertTrue(backupDir.mkdirs());
		final Properties props = new Properties();
		AppProperties appProps = new AppProperties() {
			public String getProperty(String sKey) {
				return props.getProperty(sKey);
			}
			public String getHost() {
				return "localhost";
			}
		};

		// Daily disabled
		props.setProperty("backup.daily", "false");
		props.setProperty("backup.dir", backupDir.getAbsolutePath());
		assertFalse(DatabaseBackup.isDailyBackupNeeded(appProps));

		// Daily enabled, no dir
		props.setProperty("backup.daily", "true");
		props.remove("backup.dir");
		assertFalse(DatabaseBackup.isDailyBackupNeeded(appProps));

		// Daily enabled, valid dir, no backups yet today
		props.setProperty("backup.dir", backupDir.getAbsolutePath());
		assertTrue(DatabaseBackup.isDailyBackupNeeded(appProps));

		// Simulate backup file created today
		String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
		File todayBackup = new File(backupDir, "backup-pos-" + today + "-120000.sql");
		FileOutputStream out = new FileOutputStream(todayBackup);
		out.write("DUMP".getBytes());
		out.close();

		assertFalse(DatabaseBackup.isDailyBackupNeeded(appProps));
	}

	@Test
	public void performsDerbyBackupEndToEnd() throws Exception {
		File dbDir = new File(temp, "derby-db");
		String url = "jdbc:derby:" + dbDir.getAbsolutePath() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);

		File backupDir = new File(temp, "derby-backups");
		assertTrue(backupDir.mkdirs());
		final Properties props = new Properties();
		props.setProperty("db.URL", url);
		props.setProperty("backup.dir", backupDir.getAbsolutePath());
		props.setProperty("backup.daily", "true");

		AppProperties appProps = new AppProperties() {
			public String getProperty(String sKey) {
				return props.getProperty(sKey);
			}
			public String getHost() {
				return "localhost";
			}
		};

		assertTrue(DatabaseBackup.isDailyBackupNeeded(appProps));

		File result = DatabaseBackup.backup(appProps);
		assertNotNull(result);
		assertTrue(result.exists());
		assertTrue(result.isDirectory());

		// After backup, daily backup is no longer needed
		assertFalse(DatabaseBackup.isDailyBackupNeeded(appProps));
	}
}
