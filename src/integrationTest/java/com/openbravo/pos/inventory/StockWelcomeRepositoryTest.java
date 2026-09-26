package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class StockWelcomeRepositoryTest {
	@Test
	void scanCombinesCatalogueTaxStockAndOpenOrderWithoutDuplicatingAProduct() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:derby:memory:stockWelcomeIT;create=true");
				Statement sql = connection.createStatement()) {
			sql.executeUpdate("CREATE TABLE CATEGORIES (ID VARCHAR(40), NAME VARCHAR(80))");
			sql.executeUpdate("CREATE TABLE TAXCATEGORIES (ID VARCHAR(40), NAME VARCHAR(80))");
			sql.executeUpdate(
					"CREATE TABLE TAXES (CATEGORY VARCHAR(40), CUSTCATEGORY VARCHAR(40), VALIDFROM TIMESTAMP, RATE DOUBLE)");
			sql.executeUpdate(
					"CREATE TABLE PRODUCTS (ID VARCHAR(40), NAME VARCHAR(80), REFERENCE VARCHAR(40), CODE VARCHAR(40), PRICESELL DOUBLE, SALE_PERCENT DOUBLE, CATEGORY VARCHAR(40), TAXCAT VARCHAR(40), BRAND VARCHAR(40))");
			sql.executeUpdate(
					"CREATE TABLE PRICE_RULES (BRAND VARCHAR(40), MARKUP_PERCENT DOUBLE, ROUNDING VARCHAR(30))");
			sql.executeUpdate("CREATE TABLE STOCKCURRENT (PRODUCT VARCHAR(40), UNITS DOUBLE)");
			sql.executeUpdate("CREATE TABLE STOCKDIARY (PRODUCT VARCHAR(40), DATENEW TIMESTAMP)");
			sql.executeUpdate(
					"CREATE TABLE REPLENISHMENT_ENTRIES (OPEN_PRODUCT_ID VARCHAR(40), STATUS VARCHAR(20), CUSTOMER_ID VARCHAR(40), CUSTOMER_NAME VARCHAR(80), CREATED_AT TIMESTAMP)");
			sql.executeUpdate("INSERT INTO CATEGORIES VALUES ('c', 'Dairy'), ('gift-vouchers', 'Vouchers')");
			sql.executeUpdate("INSERT INTO TAXCATEGORIES VALUES ('t', 'Reduced')");
			sql.executeUpdate(
					"INSERT INTO TAXES VALUES ('t', NULL, '2020-01-01 00:00:00', 0.08), ('t', NULL, '2025-01-01 00:00:00', 0.10)");
			sql.executeUpdate(
					"INSERT INTO PRODUCTS VALUES ('milk', 'Milk', 'REF1', '12345678', 2.50, 15, 'c', 't', 'Farm')");
			sql.executeUpdate("INSERT INTO PRICE_RULES VALUES ('Farm', 25, 'CENT')");
			sql.executeUpdate("INSERT INTO STOCKCURRENT VALUES ('milk', 3), ('milk', -5)");
			sql.executeUpdate("INSERT INTO STOCKDIARY VALUES ('milk', '2026-09-20 12:00:00')");
			sql.executeUpdate(
					"INSERT INTO REPLENISHMENT_ENTRIES VALUES ('milk', 'PENDING', 'customer', 'Ana', '2026-09-18 12:00:00')");
			StockWelcomeRepository repository = new StockWelcomeRepository(connection);
			assertFalse(repository.emptyCatalogue());
			for (String term : new String[]{"12345678", "REF1", "REF", "mil"}) {
				assertEquals(1, repository.search(term).size());
				StockWelcomeRepository.Product product = repository.search(term).get(0);
				assertEquals(2.50, product.price);
				assertEquals(15, product.salePercent);
				assertEquals(-2, product.units);
				assertEquals("Dairy", product.category);
				assertTrue(product.tax.startsWith("Reduced · 10"));
				assertEquals("PENDING", product.status);
				assertEquals("Ana", product.customer);
			}
			assertTrue(repository.search("not found").isEmpty());
			StockWelcomeRepository.Queue queue = repository.queue();
			assertEquals(1, queue.pending);
			assertEquals(0, queue.ordered);
			assertEquals(1, queue.customers);
			assertEquals("2026-09-18 12:00:00.0", queue.oldest.toString());
			sql.executeUpdate(
					"INSERT INTO REPLENISHMENT_ENTRIES VALUES (NULL, 'ORDERED', NULL, NULL, '2026-09-19 12:00:00')");
			sql.executeUpdate(
					"INSERT INTO REPLENISHMENT_ENTRIES VALUES (NULL, 'RECEIVED', 'closed', 'Closed', '2026-09-19 12:00:00')");
			queue = repository.queue();
			assertEquals(1, queue.pending);
			assertEquals(1, queue.ordered);
			assertEquals(1, queue.customers);
			sql.executeUpdate("DELETE FROM REPLENISHMENT_ENTRIES");
			assertEquals(0, repository.queue().pending + repository.queue().ordered + repository.queue().customers);
			sql.executeUpdate("DELETE FROM PRODUCTS");
			assertTrue(repository.emptyCatalogue());
		}
	}
}
