package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Removes the unused application metadata table. */
public class V34__remove_applications extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (SchemaObjects.tableExists(connection, "APPLICATIONS")) {
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("DROP TABLE APPLICATIONS");
			}
		}
	}
}
