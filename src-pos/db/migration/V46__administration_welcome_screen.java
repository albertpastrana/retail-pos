package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Adds the administration welcome screen to the administrator and manager
 * roles.
 */
public class V46__administration_welcome_screen extends BaseJavaMigration {

	private static final String TEMPLATE_PATH = "/com/openbravo/pos/templates/";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		updateRole(connection, "0", "Role.Administrator.xml");
		updateRole(connection, "1", "Role.Manager.xml");
	}

	private void updateRole(Connection connection, String id, String template) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("UPDATE ROLES SET PERMISSIONS=? WHERE ID=?")) {
			statement.setBytes(1, readTemplate(template));
			statement.setString(2, id);
			statement.executeUpdate();
		}
	}

	private byte[] readTemplate(String template) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(TEMPLATE_PATH + template)) {
			if (input == null) {
				throw new IOException("Missing database resource " + template);
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int count;
			while ((count = input.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}
			return output.toByteArray();
		}
	}
}
