package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Records when a replenishment entry became ready for customer pickup. */
public class V60__replenishment_received_at extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		boolean tableExists = false;
		try (ResultSet tables = connection.getMetaData().getTables(null, null, null, null)) {
			while (tables.next())
				if ("REPLENISHMENT_ENTRIES".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
					tableExists = true;
					break;
				}
		}
		if (!tableExists)
			return;
		try (PreparedStatement statement = connection
				.prepareStatement("ALTER TABLE REPLENISHMENT_ENTRIES ADD COLUMN RECEIVED_AT TIMESTAMP")) {
			statement.executeUpdate();
		}
	}
}
