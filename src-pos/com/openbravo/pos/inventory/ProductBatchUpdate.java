package com.openbravo.pos.inventory;

import com.openbravo.data.loader.Session;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Updates only the batch-editable fields, protecting other product data. */
final class ProductBatchUpdate {
	private final Session session;

	ProductBatchUpdate(Session session) {
		this.session = session;
	}

	/**
	 * Returns false if another user changed either field since the list was loaded.
	 */
	boolean save(String id, double oldNet, String oldCategory, double net, String category) throws SQLException {
		Connection connection = session.getConnection();
		String sql = "UPDATE PRODUCTS SET PRICESELL = ?, CATEGORY = ?, UPDATED_AT = CURRENT_TIMESTAMP "
				+ "WHERE ID = ? AND PRICESELL = ? AND " + (oldCategory == null ? "CATEGORY IS NULL" : "CATEGORY = ?");
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setDouble(1, net);
			statement.setString(2, category);
			statement.setString(3, id);
			statement.setDouble(4, oldNet);
			if (oldCategory != null) {
				statement.setString(5, oldCategory);
			}
			return statement.executeUpdate() == 1;
		}
	}

	Object[] current(String id) throws SQLException {
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("SELECT PRICESELL, CATEGORY FROM PRODUCTS WHERE ID = ?")) {
			statement.setString(1, id);
			try (ResultSet result = statement.executeQuery()) {
				return result.next() ? new Object[]{result.getDouble(1), result.getString(2)} : null;
			}
		}
	}
}
