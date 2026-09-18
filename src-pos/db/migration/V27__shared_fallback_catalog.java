package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Creates the shared, non-sellable catalogue used to prefill product creates.
 */
public class V27__shared_fallback_catalog extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			if (!SchemaObjects.tableExists(connection, "CATALOG_FALLBACK_PRODUCTS")) {
				statement.execute("CREATE TABLE CATALOG_FALLBACK_PRODUCTS (" + "ID VARCHAR(255) NOT NULL PRIMARY KEY, "
						+ "BARCODE VARCHAR(255) NOT NULL UNIQUE, "
						+ "REFERENCE VARCHAR(255), NAME VARCHAR(255), CATEGORY_ID VARCHAR(255), "
						+ "CATEGORY_NAME VARCHAR(255), PRICE_BUY DOUBLE PRECISION, "
						+ "PRICE_SELL DOUBLE PRECISION, BRAND VARCHAR(255))");
			}
			if (!SchemaObjects.tableExists(connection, "CATALOG_FALLBACK_PRICES")) {
				statement.execute(
						"CREATE TABLE CATALOG_FALLBACK_PRICES (" + "LOOKUP_CODE VARCHAR(255) NOT NULL PRIMARY KEY, "
								+ "REFERENCE VARCHAR(255), PRICE_BUY DOUBLE PRECISION, "
								+ "PRICE_SELL DOUBLE PRECISION, BRAND VARCHAR(255), SOURCE VARCHAR(255))");
			}
			if (!SchemaObjects.indexExists(connection, "CATALOG_FALLBACK_PRODUCTS",
					"CATALOG_FALLBACK_PRODUCTS_REF_INX")) {
				statement.execute(
						"CREATE INDEX CATALOG_FALLBACK_PRODUCTS_REF_INX " + "ON CATALOG_FALLBACK_PRODUCTS(REFERENCE)");
			}
		}
	}
}
