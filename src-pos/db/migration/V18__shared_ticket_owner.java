package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V18__shared_ticket_owner extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (!SchemaObjects.columnExists(connection, "SHAREDTICKETS", "HOST")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("ALTER TABLE SHAREDTICKETS ADD COLUMN HOST VARCHAR(255)");
			}
		}
		if (!SchemaObjects.columnExists(connection, "SHAREDTICKETS", "PAYING")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("ALTER TABLE SHAREDTICKETS ADD COLUMN PAYING BOOLEAN NOT NULL DEFAULT FALSE");
			}
		}
	}
}
