package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Keeps one open store replenishment and one open customer order per product.
 */
public class V56__separate_replenishment_request_types extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
			boolean exists = false;
			while (tables.next())
				if ("REPLENISHMENT_ENTRIES".equalsIgnoreCase(tables.getString("TABLE_NAME")))
					exists = true;
			if (!exists)
				return;
		}
		boolean hasProductId = false;
		boolean hasCustomerId = false;
		try (ResultSet columns = connection.getMetaData().getColumns(null, null, "REPLENISHMENT_ENTRIES", null)) {
			while (columns.next()) {
				hasProductId |= "PRODUCT_ID".equalsIgnoreCase(columns.getString("COLUMN_NAME"));
				hasCustomerId |= "CUSTOMER_ID".equalsIgnoreCase(columns.getString("COLUMN_NAME"));
			}
		}
		if (!hasProductId || !hasCustomerId)
			return;
		String database = connection.getMetaData().getDatabaseProductName();
		String expression = database.equalsIgnoreCase("MySQL")
				? "CASE WHEN PRODUCT_ID IS NULL OR CUSTOMER_ID IS NOT NULL OR STATUS IN ('RECEIVED', 'DELIVERED') THEN CONCAT('I:', ID) ELSE CONCAT('P:R:', PRODUCT_ID) END"
				: "CASE WHEN PRODUCT_ID IS NULL OR CUSTOMER_ID IS NOT NULL OR STATUS IN ('RECEIVED', 'DELIVERED') THEN 'I:' || ID ELSE 'P:R:' || PRODUCT_ID END";
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("UPDATE REPLENISHMENT_ENTRIES SET OPEN_UNIQUE_KEY=" + expression);
		}
	}
}
