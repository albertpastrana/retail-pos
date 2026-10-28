package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.openbravo.data.loader.Session;

public class AdHocSqlMigrationImporterIT {
	@TempDir
	Path directory;

	@Test
	public void importsSignedMigrationOnceAndRecordsChecksum() throws Exception {
		String url = "jdbc:derby:memory:adhocSqlMigrationImporterIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		KeyPair keyPair = keyPair();
		Path publicKey = writePublicKey(keyPair);
		byte[] content = ("INSERT INTO CATALOG_FALLBACK_PRODUCTS (ID, BARCODE, NAME) VALUES "
				+ "('adhoc-1', '9990000000501', 'SQL; product');\n").getBytes(StandardCharsets.UTF_8);
		Path file = writeMigration("migration-one.sql", content, keyPair);

		Session session = new Session(url, null, null);
		try {
			AdHocSqlMigrationImporter.importAtStartup(session, properties(publicKey));
			assertEquals(1, queryInt(session, "SELECT COUNT(*) FROM CATALOG_FALLBACK_PRODUCTS WHERE ID = 'adhoc-1'"));
			assertEquals(1, queryInt(session, "SELECT COUNT(*) FROM AD_HOC_MIGRATIONS WHERE ID = 'migration-one.sql'"));
			AdHocSqlMigrationImporter.importAtStartup(session, properties(publicKey));
			assertEquals(1, queryInt(session, "SELECT COUNT(*) FROM CATALOG_FALLBACK_PRODUCTS WHERE ID = 'adhoc-1'"));
		} finally {
			session.close();
		}
		assertTrue(Files.isRegularFile(directory.resolve("processed/migration-one.sql")));
		assertTrue(!Files.exists(file));
	}

	@Test
	public void rollsBackWholeMigrationWhenAStatementFails() throws Exception {
		String url = "jdbc:derby:memory:adhocSqlMigrationImporterRollbackIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		KeyPair keyPair = keyPair();
		Path publicKey = writePublicKey(keyPair);
		byte[] content = ("INSERT INTO CATALOG_FALLBACK_PRODUCTS (ID, BARCODE, NAME) VALUES "
				+ "('adhoc-2', '9990000000502', 'Rolled back');\n"
				+ "INSERT INTO TABLE_DOES_NOT_EXIST (ID) VALUES ('adhoc-2');\n").getBytes(StandardCharsets.UTF_8);
		writeMigration("migration-rollback.sql", content, keyPair);

		Session session = new Session(url, null, null);
		try {
			AdHocSqlMigrationImporter.importAtStartup(session, properties(publicKey));
			assertEquals(0, queryInt(session, "SELECT COUNT(*) FROM CATALOG_FALLBACK_PRODUCTS WHERE ID = 'adhoc-2'"));
			assertEquals(0,
					queryInt(session, "SELECT COUNT(*) FROM AD_HOC_MIGRATIONS WHERE ID = 'migration-rollback.sql'"));
		} finally {
			session.close();
		}
		assertTrue(Files.isRegularFile(directory.resolve("rejected/migration-rollback.sql")));
	}

	@Test
	public void rejectsUnsignedMigration() throws Exception {
		String url = "jdbc:derby:memory:adhocSqlMigrationImporterRejectIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		KeyPair keyPair = keyPair();
		Path publicKey = writePublicKey(keyPair);
		Path file = directory.resolve("migration-unsigned.sql");
		Files.writeString(file, "INSERT INTO TABLE_DOES_NOT_EXIST (ID) VALUES ('unsigned');", StandardCharsets.UTF_8);

		Session session = new Session(url, null, null);
		try {
			AdHocSqlMigrationImporter.importAtStartup(session, properties(publicKey));
		} finally {
			session.close();
		}
		assertTrue(Files.isRegularFile(directory.resolve("rejected/migration-unsigned.sql")));
	}

	@Test
	public void importsSignedMigrationOnConfiguredDatabaseServers() throws Exception {
		KeyPair keyPair = keyPair();
		Path publicKey = writePublicKey(keyPair);
		importOnDatabase("mysql", System.getProperty("pos.mysql.url", "jdbc:mysql://127.0.0.1:13306/pos"),
				System.getProperty("pos.mysql.user", "root"), System.getProperty("pos.mysql.password", "pos"), keyPair,
				publicKey);
		importOnDatabase("postgres", System.getProperty("pos.postgres.url", "jdbc:postgresql://127.0.0.1:15432/pos"),
				System.getProperty("pos.postgres.user", "pos"), System.getProperty("pos.postgres.password", "pos"),
				keyPair, publicKey);
	}

	private void importOnDatabase(String database, String url, String user, String password, KeyPair keyPair,
			Path publicKey) throws Exception {
		DatabaseMigrator.migrate(url, user, password);
		String id = "adhoc-" + database + "-" + UUID.randomUUID();
		String fileName = "migration-" + id + ".sql";
		String table = "ADHOC_IMPORT_" + UUID.randomUUID().toString().replace("-", "");
		byte[] content = ("CREATE TABLE " + table + " (ID VARCHAR(255) NOT NULL PRIMARY KEY);" + "INSERT INTO " + table
				+ " (ID) VALUES ('" + id + "');").getBytes(StandardCharsets.UTF_8);
		writeMigration(fileName, content, keyPair);
		Session session = new Session(url, user, password);
		try {
			AdHocSqlMigrationImporter.importAtStartup(session, properties(publicKey));
			assertEquals(1, queryInt(session, "SELECT COUNT(*) FROM " + table + " WHERE ID = '" + id + "'"));
			assertEquals(1, queryInt(session, "SELECT COUNT(*) FROM AD_HOC_MIGRATIONS WHERE ID = '" + fileName + "'"));
		} finally {
			try (java.sql.Statement cleanup = session.getConnection().createStatement();
					java.sql.PreparedStatement deleteHistory = session.getConnection()
							.prepareStatement("DELETE FROM AD_HOC_MIGRATIONS WHERE ID = ?")) {
				cleanup.executeUpdate("DROP TABLE " + table);
				deleteHistory.setString(1, fileName);
				deleteHistory.executeUpdate();
			}
			session.close();
		}
	}

	private KeyPair keyPair() throws Exception {
		return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
	}

	private Path writePublicKey(KeyPair keyPair) throws Exception {
		String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
				.encodeToString(keyPair.getPublic().getEncoded());
		Path path = directory.resolve("public.pem");
		Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n",
				StandardCharsets.US_ASCII);
		return path;
	}

	private Path writeMigration(String name, byte[] content, KeyPair keyPair) throws Exception {
		Path file = directory.resolve(name);
		Files.write(file, content);
		Files.write(file.resolveSibling(name + ".sig"), sign(content, keyPair));
		return file;
	}

	private byte[] sign(byte[] content, KeyPair keyPair) throws Exception {
		Signature signature = Signature.getInstance("Ed25519");
		signature.initSign(keyPair.getPrivate());
		signature.update(content);
		return signature.sign();
	}

	private AppProperties properties(Path publicKey) {
		return new AppProperties() {
			@Override
			public String getHost() {
				return "test";
			}

			@Override
			public String getProperty(String key) {
				if ("catalog.import.directory".equals(key)) {
					return directory.toString();
				}
				if ("catalog.import.publicKey".equals(key)) {
					return publicKey.toString();
				}
				return null;
			}
		};
	}

	private int queryInt(Session session, String sql) throws Exception {
		try (java.sql.Statement statement = session.getConnection().createStatement();
				java.sql.ResultSet result = statement.executeQuery(sql)) {
			result.next();
			return result.getInt(1);
		}
	}
}
