package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.openbravo.pos.inventory.CatalogVariantModel;

/** Adds the stable family key used to find catalog variants efficiently. */
public class V36__catalog_product_families extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		addFamilyColumn(connection, "PRODUCTS");
		addFamilyColumn(connection, "CATALOG_FALLBACK_PRODUCTS");
		addFamilyIndex(connection, "PRODUCTS", "PRODUCTS_FAMILY_INX");
		addFamilyIndex(connection, "CATALOG_FALLBACK_PRODUCTS", "CATALOG_FALLBACK_PRODUCTS_FAMILY_INX");
		backfill(connection, "PRODUCTS");
		backfill(connection, "CATALOG_FALLBACK_PRODUCTS");
	}

	private void addFamilyColumn(Connection connection, String table) throws Exception {
		if (!SchemaObjects.columnExists(connection, table, "FAMILY")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("ALTER TABLE " + table + " ADD FAMILY VARCHAR(255)");
			}
		}
	}

	private void addFamilyIndex(Connection connection, String table, String index) throws Exception {
		if (!SchemaObjects.indexExists(connection, table, index)) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE INDEX " + index + " ON " + table + "(FAMILY)");
			}
		}
	}

	private void backfill(Connection connection, String table) throws Exception {
		try (PreparedStatement select = connection
				.prepareStatement("SELECT ID, REFERENCE, BRAND FROM " + table + " WHERE FAMILY IS NULL");
				PreparedStatement update = connection
						.prepareStatement("UPDATE " + table + " SET FAMILY = ? WHERE ID = ?")) {
			try (ResultSet rows = select.executeQuery()) {
				while (rows.next()) {
					String family = CatalogVariantModel.family(rows.getString("REFERENCE"), rows.getString("BRAND"));
					if (family != null) {
						update.setString(1, family);
						update.setString(2, rows.getString("ID"));
						update.addBatch();
					}
				}
			}
			update.executeBatch();
		}
	}
}
