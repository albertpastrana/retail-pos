package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AttributeManagementPersistenceTest {

	@TempDir
	Path temp;

	@Test
	public void storesCaseInsensitiveProductCharacteristics() throws Exception {
		String url = "jdbc:derby:" + temp.resolve("product-attributes") + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		try (Connection connection = java.sql.DriverManager.getConnection(url)) {
			insert(connection, "one", "Talla", "talla", "XL", "xl");
			assertEquals(1, count(connection, "SELECT COUNT(*) FROM PRODUCT_ATTRIBUTES"
					+ " WHERE PRODUCT_ID = 'gift-voucher-10' AND ATTRIBUTE_KEY_NORMALIZED = 'talla'"));

			assertThrows(Exception.class, () -> insert(connection, "two", "talla", "talla", "L", "l"));
		}
	}

	private void insert(Connection connection, String id, String key, String normalizedKey, String value,
			String normalizedValue) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO PRODUCT_ATTRIBUTES (ID, PRODUCT_ID, ATTRIBUTE_KEY, ATTRIBUTE_KEY_NORMALIZED, "
						+ "ATTRIBUTE_VALUE, ATTRIBUTE_VALUE_NORMALIZED) VALUES (?, 'gift-voucher-10', ?, ?, ?, ?)")) {
			statement.setString(1, id);
			statement.setString(2, key);
			statement.setString(3, normalizedKey);
			statement.setString(4, value);
			statement.setString(5, normalizedValue);
			statement.executeUpdate();
		}
	}

	private int count(Connection connection, String sql) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement(sql);
				java.sql.ResultSet result = statement.executeQuery()) {
			result.next();
			return result.getInt(1);
		}
	}
}
