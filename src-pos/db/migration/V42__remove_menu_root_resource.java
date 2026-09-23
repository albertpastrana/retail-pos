package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** The application menu is versioned in the application resources. */
public class V42__remove_menu_root_resource extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM RESOURCES WHERE NAME = ?")) {
			statement.setString(1, "Menu.Root");
			statement.executeUpdate();
		}
	}
}
