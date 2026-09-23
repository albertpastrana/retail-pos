package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Removes the duplicate customers entry from the backoffice menu. */
public class V39__remove_duplicate_customers_menu extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		updateResource(connection, "Menu.Root", "/com/openbravo/pos/templates/Menu.Root.txt");
		updateRole(connection, "0", "/com/openbravo/pos/templates/Role.Administrator.xml");
		updateRole(connection, "1", "/com/openbravo/pos/templates/Role.Manager.xml");
	}

	private void updateResource(Connection connection, String name, String resource) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("UPDATE RESOURCES SET CONTENT=? WHERE NAME=?")) {
			statement.setBytes(1, read(resource));
			statement.setString(2, name);
			statement.executeUpdate();
		}
	}

	private void updateRole(Connection connection, String id, String resource) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("UPDATE ROLES SET PERMISSIONS=? WHERE ID=?")) {
			statement.setBytes(1, read(resource));
			statement.setString(2, id);
			statement.executeUpdate();
		}
	}

	private byte[] read(String resource) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			if (input == null)
				throw new IOException("Missing database resource " + resource);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int count;
			while ((count = input.read(buffer)) != -1)
				output.write(buffer, 0, count);
			return output.toByteArray();
		}
	}
}
