package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V19__canonical_roles extends BaseJavaMigration {

	private static final String TEMPLATE_PATH = "/com/openbravo/pos/templates/";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (PreparedStatement people = connection
				.prepareStatement("UPDATE PEOPLE SET ROLE = '2' WHERE ROLE NOT IN ('0', '1', '2')")) {
			people.executeUpdate();
		}
		try (PreparedStatement roles = connection
				.prepareStatement("DELETE FROM ROLES WHERE ID NOT IN ('0', '1', '2')")) {
			roles.executeUpdate();
		}
		updateRole(connection, "0", "Administrator", "Role.Administrator.xml");
		updateRole(connection, "1", "Manager", "Role.Manager.xml");
		updateRole(connection, "2", "Seller", "Role.Seller.xml");
		updateResource(connection, "Menu.Root", "Menu.Root.txt");
	}

	private void updateResource(Connection connection, String name, String template) throws SQLException, IOException {
		try (PreparedStatement statement = connection
				.prepareStatement("UPDATE RESOURCES SET CONTENT = ? WHERE NAME = ?")) {
			statement.setBytes(1, readTemplate(template));
			statement.setString(2, name);
			statement.executeUpdate();
		}
	}

	private void updateRole(Connection connection, String id, String name, String template)
			throws SQLException, IOException {
		try (PreparedStatement statement = connection
				.prepareStatement("UPDATE ROLES SET NAME = ?, PERMISSIONS = ? WHERE ID = ?")) {
			statement.setString(1, name);
			statement.setBytes(2, readTemplate(template));
			statement.setString(3, id);
			statement.executeUpdate();
		}
	}

	private byte[] readTemplate(String file) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(TEMPLATE_PATH + file)) {
			if (input == null) {
				throw new IOException("Missing database resource " + TEMPLATE_PATH + file);
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int count;
			while ((count = input.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}
			return output.toByteArray();
		}
	}
}
