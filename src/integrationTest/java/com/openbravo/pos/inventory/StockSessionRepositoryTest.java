package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class StockSessionRepositoryTest {
	@Test
	void receiptSurvivesRestartAndOnlyPostsValidLinesTogether() throws Exception {
		String url = "jdbc:derby:memory:receiving" + UUID.randomUUID() + ";create=true";
		try (Connection c = DriverManager.getConnection(url); Statement sql = c.createStatement()) {
			sql.execute("CREATE TABLE LOCATIONS (ID VARCHAR(255) PRIMARY KEY)");
			sql.execute("INSERT INTO LOCATIONS VALUES ('0')");
			sql.execute(
					"CREATE TABLE PRODUCTS (ID VARCHAR(255) PRIMARY KEY,NAME VARCHAR(255),CODE VARCHAR(255),REFERENCE VARCHAR(255))");
			sql.execute("INSERT INTO PRODUCTS VALUES ('p1','Milk','123','M1'),('p2','Wine','456','W2')");
			sql.execute("CREATE TABLE STOCKCURRENT (LOCATION VARCHAR(255),PRODUCT VARCHAR(255),UNITS DOUBLE)");
			sql.execute("INSERT INTO STOCKCURRENT VALUES ('0','p1',4)");
			sql.execute(
					"CREATE TABLE STOCKDIARY (ID VARCHAR(255) PRIMARY KEY,DATENEW TIMESTAMP,REASON INTEGER,LOCATION VARCHAR(255),PRODUCT VARCHAR(255),UNITS DOUBLE,PRICE DOUBLE)");
			sql.execute(
					"CREATE TABLE REPLENISHMENT_ENTRIES (OPEN_PRODUCT_ID VARCHAR(255),STATUS VARCHAR(20),UPDATED_AT TIMESTAMP,UPDATED_BY VARCHAR(255))");
			sql.execute("INSERT INTO REPLENISHMENT_ENTRIES VALUES ('p1','ORDERED',NULL,NULL)");
			sql.execute("CREATE TABLE ROLES (ID VARCHAR(255),PERMISSIONS BLOB)");
			try (PreparedStatement role = c.prepareStatement("INSERT INTO ROLES VALUES ('0',?)")) {
				role.setBytes(1,
						"<permissions><class name=\"com.openbravo.pos.inventory.StockDiaryPanel\"/></permissions>"
								.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				role.executeUpdate();
			}
			Flyway flyway = Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration")
					.baselineOnMigrate(true).baselineVersion("49").load();
			flyway.migrate();
			flyway.migrate();
			StockSessionRepository repo = new StockSessionRepository(c);
			StockSessionRepository.Session receipt = repo.open("Supplier", "A-42", "0", "user");
			assertThrows(IllegalArgumentException.class, () -> repo.post(receipt.id, "user"));
			assertThrows(IllegalArgumentException.class, () -> repo.scan(receipt.id, "123", 1.5, true));
			assertThrows(IllegalArgumentException.class, () -> repo.scanProduct(receipt.id, "missing", "123", 2, true));
			repo.scanProduct(receipt.id, "p1", "123", 1, true);
			assertEquals(1, repo.lines(receipt.id, false).size());
			assertThrows(IllegalArgumentException.class,
					() -> repo.quantity(receipt.id, repo.lines(receipt.id, false).get(0).id, 1.5));
			repo.scan(receipt.id, "123", 1, false);
			repo.scan(receipt.id, "123", 1, false);
			repo.scan(receipt.id, "456", 6, true);
			repo.scan(receipt.id, "unknown", 3, true);
			assertEquals(3, repo.lines(receipt.id, false).size());
			repo.tickAll(receipt.id, true);
			assertTrue(repo.lines(receipt.id, false).stream().allMatch(line -> line.ticked));
			repo.tickAll(receipt.id, false);
			assertTrue(repo.lines(receipt.id, false).stream().noneMatch(line -> line.ticked));
			assertEquals(4, stock(c, "p1"));
			try (Connection reopened = DriverManager.getConnection(url)) {
				StockSessionRepository continued = new StockSessionRepository(reopened);
				assertEquals(1, continued.openSessions().size());
				assertThrows(IllegalArgumentException.class, () -> continued.post(receipt.id, "user"));
				assertEquals(0, count(c, "STOCKDIARY"));
				StockSessionRepository.Line unknown = continued.lines(receipt.id, false).stream()
						.filter(l -> l.product == null).findFirst().orElseThrow();
				continued.remove(receipt.id, unknown.id);
				StockSessionRepository.Line milk = continued.lines(receipt.id, false).stream()
						.filter(l -> "p1".equals(l.product)).findFirst().orElseThrow();
				assertEquals(3, milk.units);
				continued.quantity(receipt.id, milk.id, 5);
				continued.tick(receipt.id, milk.id, true);
				assertTrue(continued.lines(receipt.id, false).stream().filter(l -> "p1".equals(l.product)).findFirst()
						.orElseThrow().ticked);
				assertEquals(4, stock(c, "p1"));
				continued.scan(receipt.id, "newcode", 2, true);
				try (Statement products = c.createStatement()) {
					products.execute("INSERT INTO PRODUCTS VALUES ('p3','New item','newcode','N3')");
				}
				assertTrue(continued.lines(receipt.id, false).stream().anyMatch(line -> "p3".equals(line.product)));
				continued.post(receipt.id, "user");
				assertEquals(9, stock(c, "p1"));
				assertEquals(6, stock(c, "p2"));
				assertEquals(3, count(c, "STOCKDIARY"));
				assertTrue(continued.openSessions().isEmpty());
				assertThrows(IllegalStateException.class, () -> continued.post(receipt.id, "user"));
			}
			try (ResultSet rs = sql.executeQuery("SELECT SUPPLIER,DELIVERYNOTE,STOCKSESSION FROM STOCKDIARY")) {
				while (rs.next()) {
					assertEquals("Supplier", rs.getString(1));
					assertEquals("A-42", rs.getString(2));
					assertEquals(receipt.id, rs.getString(3));
				}
			}
			try (ResultSet rs = sql.executeQuery("SELECT STATUS,OPEN_PRODUCT_ID FROM REPLENISHMENT_ENTRIES")) {
				assertTrue(rs.next());
				assertEquals("RECEIVED", rs.getString(1));
				assertNull(rs.getString(2));
			}
			try (ResultSet rs = sql.executeQuery("SELECT PERMISSIONS FROM ROLES")) {
				assertTrue(rs.next());
				assertTrue(new String(rs.getBytes(1), java.nio.charset.StandardCharsets.UTF_8)
						.contains("StockReceivingPanel"));
			}
			// Newer installations may have variant rows; a basic receipt updates only
			// the unassigned row and does not multiply the stock of every variant.
			sql.execute("ALTER TABLE STOCKCURRENT ADD ATTRIBUTESETINSTANCE_ID VARCHAR(255)");
			sql.execute("INSERT INTO STOCKCURRENT VALUES ('0','p1',7,'variant-1')");
			StockSessionRepository.Session next = repo.open("Supplier", "A-43", "0", "user");
			repo.scan(next.id, "123", 2, true);
			assertEquals(9, repo.lines(next.id, false).get(0).stock);
			repo.post(next.id, "user");
			try (ResultSet rows = sql
					.executeQuery("SELECT UNITS,ATTRIBUTESETINSTANCE_ID FROM STOCKCURRENT WHERE PRODUCT='p1'")) {
				int seen = 0;
				while (rows.next()) {
					if (rows.getString(2) == null)
						assertEquals(11, rows.getDouble(1));
					else
						assertEquals(7, rows.getDouble(1));
					seen++;
				}
				assertEquals(2, seen);
			}
		}
	}

	private static int count(Connection c, String table) throws Exception {
		try (ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM " + table)) {
			rs.next();
			return rs.getInt(1);
		}
	}
	private static double stock(Connection c, String product) throws Exception {
		try (PreparedStatement sql = c.prepareStatement("SELECT UNITS FROM STOCKCURRENT WHERE PRODUCT=?")) {
			sql.setString(1, product);
			try (ResultSet rs = sql.executeQuery()) {
				return rs.next() ? rs.getDouble(1) : 0;
			}
		}
	}
}
