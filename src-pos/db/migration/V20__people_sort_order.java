package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20__people_sort_order extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (!SchemaObjects.columnExists(connection, "PEOPLE", "SORT_ORDER")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("ALTER TABLE PEOPLE ADD COLUMN SORT_ORDER INTEGER DEFAULT 0 NOT NULL");
			}

			try (Statement select = connection.createStatement();
					ResultSet people = select.executeQuery("SELECT ID FROM PEOPLE ORDER BY NAME, ID");
					PreparedStatement update = connection
							.prepareStatement("UPDATE PEOPLE SET SORT_ORDER = ? WHERE ID = ?")) {
				int sortOrder = 0;
				while (people.next()) {
					update.setInt(1, sortOrder++);
					update.setString(2, people.getString(1));
					update.addBatch();
				}
				update.executeBatch();
			}
		}
	}
}
