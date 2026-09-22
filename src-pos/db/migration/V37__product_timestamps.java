package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds creation and last-modification timestamps to products. */
public class V37__product_timestamps extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		addColumn(connection, "CREATED_AT");
		addColumn(connection, "UPDATED_AT");

		Timestamp migratedAt = new Timestamp(System.currentTimeMillis());
		try (PreparedStatement update = connection.prepareStatement(
				"UPDATE PRODUCTS SET CREATED_AT = ?, UPDATED_AT = ? WHERE CREATED_AT IS NULL OR UPDATED_AT IS NULL")) {
			update.setTimestamp(1, migratedAt);
			update.setTimestamp(2, migratedAt);
			update.executeUpdate();
		}
	}

	private void addColumn(Connection connection, String column) throws Exception {
		if (!SchemaObjects.columnExists(connection, "PRODUCTS", column)) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("ALTER TABLE PRODUCTS ADD " + column + " TIMESTAMP");
			}
		}
	}
}
