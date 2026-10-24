package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Repairs V52 on PostgreSQL, where the uppercase metadata lookup missed the
 * table.
 */
public class V53__repair_postgres_replenishment_status extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (!connection.getMetaData().getDatabaseProductName().toLowerCase(java.util.Locale.ROOT)
				.contains("postgresql"))
			return;
		boolean tableExists = false;
		try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(),
				null, new String[]{"TABLE"})) {
			while (tables.next()) {
				if ("REPLENISHMENT_ENTRIES".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
					tableExists = true;
					break;
				}
			}
		}
		if (!tableExists)
			return;
		try (Statement statement = connection.createStatement()) {
			// A baseline-on-migrate database may have the table but no status check.
			statement.executeUpdate("ALTER TABLE REPLENISHMENT_ENTRIES DROP CONSTRAINT IF EXISTS REPLENISHMENT_STATUS");
			statement.executeUpdate("ALTER TABLE REPLENISHMENT_ENTRIES ADD CONSTRAINT REPLENISHMENT_STATUS "
					+ "CHECK (STATUS IN ('PENDING', 'ORDERED', 'RECEIVED', 'DELIVERED'))");
		}
	}
}
