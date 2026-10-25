package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Keep open-product uniqueness while allowing any number of closed requests on
 * Derby.
 */
public class V55__replenishment_open_key extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		boolean exists = false;
		try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
			while (tables.next())
				if ("REPLENISHMENT_ENTRIES".equalsIgnoreCase(tables.getString("TABLE_NAME")))
					exists = true;
		}
		if (!exists)
			return;
		String database = connection.getMetaData().getDatabaseProductName();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("ALTER TABLE REPLENISHMENT_ENTRIES ADD COLUMN OPEN_UNIQUE_KEY VARCHAR(260)");
			String expression = database.equalsIgnoreCase("MySQL")
					? "CASE WHEN OPEN_PRODUCT_ID IS NOT NULL THEN CONCAT('P:', OPEN_PRODUCT_ID) ELSE CONCAT('I:', ID) END"
					: "CASE WHEN OPEN_PRODUCT_ID IS NOT NULL THEN 'P:' || OPEN_PRODUCT_ID ELSE 'I:' || ID END";
			statement.executeUpdate("UPDATE REPLENISHMENT_ENTRIES SET OPEN_UNIQUE_KEY=" + expression);
			statement.executeUpdate(database.equalsIgnoreCase("MySQL")
					? "ALTER TABLE REPLENISHMENT_ENTRIES DROP INDEX REPLENISHMENT_OPEN_PRODUCT_INX"
					: "DROP INDEX REPLENISHMENT_OPEN_PRODUCT_INX");
			statement.executeUpdate(
					"CREATE UNIQUE INDEX REPLENISHMENT_OPEN_KEY_INX ON REPLENISHMENT_ENTRIES(OPEN_UNIQUE_KEY)");
		}
	}
}
