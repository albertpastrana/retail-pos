package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V21__employee_guest_roles extends BaseJavaMigration {
	private static final String TEMPLATE_PATH = "/com/openbravo/pos/templates/";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		updateRole(connection, "0", "Administrator", "Role.Administrator.xml");
		updateRole(connection, "1", "Manager", "Role.Manager.xml");
		updateRole(connection, "2", "Employee", "Role.Employee.xml");
		insertGuestRole(connection, "Role.Guest.xml");
		updatePerson(connection, "2", "Employee", "2");
		insertGuestPerson(connection);
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

	private void insertGuestRole(Connection connection, String template) throws SQLException, IOException {
		if (!exists(connection, "SELECT ID FROM ROLES WHERE ID = '3'")) {
			try (PreparedStatement statement = connection
					.prepareStatement("INSERT INTO ROLES(ID, NAME, PERMISSIONS) VALUES ('3', 'Guest', ?)")) {
				statement.setBytes(1, readTemplate(template));
				statement.executeUpdate();
			}
		}
	}

	private void updatePerson(Connection connection, String id, String name, String role) throws SQLException {
		try (PreparedStatement statement = connection
				.prepareStatement("UPDATE PEOPLE SET NAME = ?, ROLE = ? WHERE ID = ?")) {
			statement.setString(1, name);
			statement.setString(2, role);
			statement.setString(3, id);
			statement.executeUpdate();
		}
	}

	private void insertGuestPerson(Connection connection) throws SQLException {
		if (!exists(connection, "SELECT ID FROM PEOPLE WHERE ID = '3'")) {
			try (PreparedStatement statement = connection
					.prepareStatement("INSERT INTO PEOPLE(ID, SORT_ORDER, NAME, APPPASSWORD, ROLE, VISIBLE, IMAGE) "
							+ "VALUES (?, ?, ?, NULL, ?, ?, NULL)")) {
				statement.setString(1, "3");
				statement.setInt(2, 3);
				statement.setString(3, "Guest");
				statement.setString(4, "3");
				statement.setBoolean(5, true);
				statement.executeUpdate();
			}
		}
	}

	private boolean exists(Connection connection, String sql) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet results = statement.executeQuery()) {
			return results.next();
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
