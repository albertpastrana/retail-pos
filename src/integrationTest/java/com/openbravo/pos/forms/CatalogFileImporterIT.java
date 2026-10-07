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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.openbravo.data.loader.Session;

public class CatalogFileImporterIT {

	@TempDir
	Path directory;

	@Test
	public void importsSignedProductFileOnce() throws Exception {
		String url = "jdbc:derby:memory:catalogFileImporterIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		KeyPair keyPair = keyPair();
		Path publicKey = writePublicKey(keyPair);
		byte[] content = ("barcode\treference\tname\tcategory\tprice_buy\tprice_sell\tbrand\n"
				+ "8430000000001\tREF-1\tImported product\tgift-vouchers\t2.50\t5.00\tBrand\n")
				.getBytes(StandardCharsets.UTF_8);
		Path file = directory.resolve("products-one.tsv");
		Files.write(file, content);
		Files.write(file.resolveSibling("products-one.tsv.sig"), sign(content, keyPair));

		Session session = new Session(url, null, null);
		try {
			CatalogFileImporter.importAtStartup(session, properties(publicKey));
			assertEquals(1, queryInt(session, "SELECT COUNT(*) FROM PRODUCTS WHERE CODE = '8430000000001'"));
			CatalogFileImporter.importAtStartup(session, properties(publicKey));
			assertEquals(1, queryInt(session, "SELECT COUNT(*) FROM PRODUCTS WHERE CODE = '8430000000001'"));
			byte[] update = ("barcode\treference\tname\tcategory\tprice_buy\tprice_sell\tbrand\n"
					+ "8430000000001\tREF-1\tUpdated product\tgift-vouchers\t3.00\t6.00\tBrand\n")
					.getBytes(StandardCharsets.UTF_8);
			Path updateFile = directory.resolve("products-two.tsv");
			Files.write(updateFile, update);
			Files.write(updateFile.resolveSibling("products-two.tsv.sig"), sign(update, keyPair));
			CatalogFileImporter.importAtStartup(session, properties(publicKey));
			assertEquals("Updated product",
					queryString(session, "SELECT NAME FROM PRODUCTS WHERE CODE = '8430000000001'"));
		} finally {
			session.close();
		}
		assertTrue(Files.isRegularFile(directory.resolve("processed/products-one.tsv")));
	}

	@Test
	public void rejectsUnsignedFileWithoutChangingDatabase() throws Exception {
		String url = "jdbc:derby:memory:catalogFileImporterRejectIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		KeyPair keyPair = keyPair();
		Path publicKey = writePublicKey(keyPair);
		Path file = directory.resolve("products-unsigned.tsv");
		Files.writeString(file, "barcode\tname\tcategory\n1\tBad\tgift-vouchers\n", StandardCharsets.UTF_8);

		Session session = new Session(url, null, null);
		try {
			CatalogFileImporter.importAtStartup(session, properties(publicKey));
			assertEquals(0, queryInt(session, "SELECT COUNT(*) FROM PRODUCTS WHERE CODE = '1'"));
		} finally {
			session.close();
		}
		assertTrue(Files.isRegularFile(directory.resolve("rejected/products-unsigned.tsv")));
	}

	@Test
	public void importsSignedFallbackFileAndPriceOverlay() throws Exception {
		String url = "jdbc:derby:memory:catalogFileImporterFallbackIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		KeyPair keyPair = keyPair();
		Path publicKey = writePublicKey(keyPair);
		byte[] content = ("barcode,reference,name,category_id,category_name,price_buy,price_sell,brand,family\n"
				+ "9990000000001,REF-F,Fallback product,CAT-1,Shirts,3.00,7.00,Brand,Brand|REF\n")
				.getBytes(StandardCharsets.UTF_8);
		Path file = directory.resolve("fallback-products-one.csv");
		Files.write(file, content);
		Files.write(file.resolveSibling("fallback-products-one.csv.sig"), sign(content, keyPair));

		Session session = new Session(url, null, null);
		try {
			CatalogFileImporter.importAtStartup(session, properties(publicKey));
			assertEquals(1, queryInt(session,
					"SELECT COUNT(*) FROM CATALOG_FALLBACK_PRODUCTS WHERE BARCODE = '9990000000001'"));
			assertEquals(1, queryInt(session,
					"SELECT COUNT(*) FROM CATALOG_FALLBACK_PRICES WHERE LOOKUP_CODE = '9990000000001'"));
		} finally {
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

	private String queryString(Session session, String sql) throws Exception {
		try (java.sql.Statement statement = session.getConnection().createStatement();
				java.sql.ResultSet result = statement.executeQuery(sql)) {
			result.next();
			return result.getString(1);
		}
	}
}
