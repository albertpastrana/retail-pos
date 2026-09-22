package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Removes CSV field quoting accidentally persisted in product names.
 */
public class V38__clean_quoted_product_names extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		List<ProductName> products = new ArrayList<ProductName>();
		Map<String, String> idsByName = new HashMap<String, String>();
		try (PreparedStatement select = connection.prepareStatement("SELECT ID, NAME FROM PRODUCTS");
				ResultSet result = select.executeQuery()) {
			while (result.next()) {
				String id = result.getString(1);
				String name = result.getString(2);
				idsByName.put(name, id);
				String cleanedName = normalize(name);
				if (!name.equals(cleanedName)) {
					products.add(new ProductName(id, name, cleanedName));
				}
			}
		}

		Map<String, String> cleanedIds = new HashMap<String, String>();
		try (PreparedStatement update = connection.prepareStatement("UPDATE PRODUCTS SET NAME = ? WHERE ID = ?")) {
			for (ProductName product : products) {
				String existingId = idsByName.get(product.cleanedName);
				String cleanedId = cleanedIds.put(product.cleanedName, product.id);
				if ((existingId != null && !existingId.equals(product.id))
						|| (cleanedId != null && !cleanedId.equals(product.id))) {
					throw new SQLException(
							"Cannot clean product name '" + product.name + "': the cleaned name is not unique");
				}
				update.setString(1, product.cleanedName);
				update.setString(2, product.id);
				update.addBatch();
			}
			update.executeBatch();
		}
	}

	static String normalize(String name) {
		if (name.length() < 2 || name.charAt(0) != '"' || name.charAt(name.length() - 1) != '"') {
			return name;
		}
		return name.substring(1, name.length() - 1).replace("\"\"", "\"");
	}

	private static final class ProductName {
		private final String id;
		private final String name;
		private final String cleanedName;

		private ProductName(String id, String name, String cleanedName) {
			this.id = id;
			this.name = name;
			this.cleanedName = cleanedName;
		}
	}
}
