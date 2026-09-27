package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.data.loader.Session;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductBatchUpdateTest {
	@TempDir
	Path temp;

	@Test
	void preservesOtherFieldsAndRejectsConcurrentChangesIndependently() throws Exception {
		Session session = new Session("jdbc:derby:" + temp.resolve("batch") + ";create=true", null, null);
		try {
			try (Statement sql = session.getConnection().createStatement()) {
				sql.executeUpdate("CREATE TABLE PRODUCTS (ID VARCHAR(20) PRIMARY KEY, PRICESELL DOUBLE, "
						+ "CATEGORY VARCHAR(30), NAME VARCHAR(50), IMAGE VARCHAR(50), UPDATED_AT TIMESTAMP)");
				sql.executeUpdate("INSERT INTO PRODUCTS (ID, PRICESELL, CATEGORY, NAME, IMAGE) "
						+ "VALUES ('small', 10, 'old', 'shirt', 'photo'), ('large', 10, NULL, 'shirt', 'photo')");
			}
			ProductBatchUpdate batch = new ProductBatchUpdate(session);
			assertTrue(batch.save("small", 10, "old", 12, "new"));
			assertFalse(batch.save("small", 10, "old", 15, "wrong"));
			assertTrue(batch.save("large", 10, null, 11, "new"));
			try (PreparedStatement sql = session.getConnection()
					.prepareStatement("SELECT PRICESELL, CATEGORY, NAME, IMAGE FROM PRODUCTS WHERE ID = 'small'");
					ResultSet result = sql.executeQuery()) {
				result.next();
				assertEquals(12, result.getDouble(1));
				assertEquals("new", result.getString(2));
				assertEquals("shirt", result.getString(3));
				assertEquals("photo", result.getString(4));
			}
			assertTrue(batch.save("small", 12, "new", 10, "old"));
		} finally {
			session.close();
		}
	}
}
