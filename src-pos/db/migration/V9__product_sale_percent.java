package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V9__product_sale_percent extends BaseJavaMigration {

	private static final String TEMPLATE_PATH = "/com/openbravo/pos/templates/";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (!SchemaObjects.columnExists(connection, "PRODUCTS", "SALE_PERCENT")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("ALTER TABLE PRODUCTS ADD COLUMN SALE_PERCENT DOUBLE PRECISION");
			}
		}
		updateResource(connection, "Menu.Root", "Menu.Root.txt");
		updateRole(connection, "0", "Role.Administrator.xml");
		updateRole(connection, "1", "Role.Manager.xml");
	}

	private void updateResource(Connection connection, String name, String file) throws Exception {
		try (PreparedStatement statement = connection
				.prepareStatement("UPDATE RESOURCES SET CONTENT = ? WHERE NAME = ?")) {
			statement.setBytes(1, readTemplate(file));
			statement.setString(2, name);
			statement.executeUpdate();
		}
	}

	private void updateRole(Connection connection, String id, String file) throws Exception {
		try (PreparedStatement statement = connection
				.prepareStatement("UPDATE ROLES SET PERMISSIONS = ? WHERE ID = ?")) {
			statement.setBytes(1, readTemplate(file));
			statement.setString(2, id);
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
