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

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseBackupTest {

	@TempDir
	public File temp;

	@Test
	public void parsesPostgresUrls() {
		ConnectionInfo info = DatabaseBackup
				.parseConnectionInfo("jdbc:postgresql://192.168.1.100:5433/retailpos?sslmode=disable");
		assertThat(info.getType()).isEqualTo(DatabaseType.POSTGRESQL);
		assertThat(info.getHost()).isEqualTo("192.168.1.100");
		assertThat(info.getPort()).isEqualTo(5433);
		assertThat(info.getDatabaseName()).isEqualTo("retailpos");

		ConnectionInfo defaultPort = DatabaseBackup.parseConnectionInfo("jdbc:postgresql://dbserver/pos");
		assertThat(defaultPort.getType()).isEqualTo(DatabaseType.POSTGRESQL);
		assertThat(defaultPort.getHost()).isEqualTo("dbserver");
		assertThat(defaultPort.getPort()).isEqualTo(5432);
		assertThat(defaultPort.getDatabaseName()).isEqualTo("pos");

		ConnectionInfo localSimple = DatabaseBackup.parseConnectionInfo("jdbc:postgresql:testdb");
		assertThat(localSimple.getType()).isEqualTo(DatabaseType.POSTGRESQL);
		assertThat(localSimple.getHost()).isEqualTo("localhost");
		assertThat(localSimple.getPort()).isEqualTo(5432);
		assertThat(localSimple.getDatabaseName()).isEqualTo("testdb");
	}

	@Test
	public void parsesMySqlAndDerbyUrls() {
		ConnectionInfo mysql = DatabaseBackup.parseConnectionInfo("jdbc:mysql://mysqlhost:3307/mysqldb?useSSL=false");
		assertThat(mysql.getType()).isEqualTo(DatabaseType.MYSQL);
		assertThat(mysql.getHost()).isEqualTo("mysqlhost");
		assertThat(mysql.getPort()).isEqualTo(3307);
		assertThat(mysql.getDatabaseName()).isEqualTo("mysqldb");

		ConnectionInfo derby = DatabaseBackup.parseConnectionInfo("jdbc:derby:/path/to/myposdb;create=true");
		assertThat(derby.getType()).isEqualTo(DatabaseType.DERBY);
		assertThat(derby.getDatabaseName()).isEqualTo("myposdb");
	}

	@Test
	public void buildsPgDumpCommandLine() {
		ConnectionInfo info = new ConnectionInfo(DatabaseType.POSTGRESQL, "dbhost", 5432, "posdb");
		File target = new File("/tmp/backup.sql");
		List<String> cmd = DatabaseBackup.buildPgDumpCommand("/usr/bin/pg_dump", info, "posuser", target);

		assertThat(cmd).containsExactly("/usr/bin/pg_dump", "-h", "dbhost", "-p", "5432", "-U", "posuser", "-f",
				target.getAbsolutePath(), "posdb");
	}

	@Test
	public void checksDailyBackupStatus() throws Exception {
		File backupDir = new File(temp, "backups");
		assertThat(backupDir.mkdirs()).isTrue();
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
		assertThat(DatabaseBackup.isDailyBackupNeeded(appProps)).isFalse();

		// Daily enabled, no dir
		props.setProperty("backup.daily", "true");
		props.remove("backup.dir");
		assertThat(DatabaseBackup.isDailyBackupNeeded(appProps)).isFalse();

		// Daily enabled, valid dir, no backups yet today
		props.setProperty("backup.dir", backupDir.getAbsolutePath());
		assertThat(DatabaseBackup.isDailyBackupNeeded(appProps)).isTrue();

		// Simulate backup file created today
		String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
		File todayBackup = new File(backupDir, "backup-pos-" + today + "-120000.sql");
		FileOutputStream out = new FileOutputStream(todayBackup);
		out.write("DUMP".getBytes());
		out.close();

		assertThat(DatabaseBackup.isDailyBackupNeeded(appProps)).isFalse();
	}

	@Test
	public void performsDerbyBackupEndToEnd() throws Exception {
		File dbDir = new File(temp, "derby-db");
		String url = "jdbc:derby:" + dbDir.getAbsolutePath() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);

		File backupDir = new File(temp, "derby-backups");
		assertThat(backupDir.mkdirs()).isTrue();
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

		assertThat(DatabaseBackup.isDailyBackupNeeded(appProps)).isTrue();

		File result = DatabaseBackup.backup(appProps);
		assertThat(result).isNotNull().exists().isDirectory();

		// After backup, daily backup is no longer needed
		assertThat(DatabaseBackup.isDailyBackupNeeded(appProps)).isFalse();
	}
}
