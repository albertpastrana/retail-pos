package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V6__price_rules extends BaseJavaMigration {

	private static final String TEMPLATE_PATH = "/com/openbravo/pos/templates/";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (!SchemaObjects.tableExists(connection, "PRICE_RULES")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE TABLE PRICE_RULES (" + "ID VARCHAR(255) NOT NULL PRIMARY KEY, "
						+ "BRAND VARCHAR(255), " + "MARKUP_PERCENT DOUBLE PRECISION NOT NULL, "
						+ "ROUNDING VARCHAR(32) NOT NULL, " + "CONSTRAINT PRICE_RULES_BRAND_UNIQUE UNIQUE (BRAND))");
				statement.execute("INSERT INTO PRICE_RULES " + "(ID, BRAND, MARKUP_PERCENT, ROUNDING) "
						+ "VALUES ('DEFAULT', NULL, 47.5, 'CHARM')");
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
