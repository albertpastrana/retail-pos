package db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds account payments to the canonical Employee role. */
public class V24__employee_account_payment_permission extends BaseJavaMigration {

	private static final String EMPLOYEE_ROLE_ID = "2";
	private static final String PERMISSION = "<class name=\"payment.debt\"/>";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		byte[] rawPermissions;
		try (PreparedStatement select = connection.prepareStatement("SELECT PERMISSIONS FROM ROLES WHERE ID = ?")) {
			select.setString(1, EMPLOYEE_ROLE_ID);
			try (ResultSet result = select.executeQuery()) {
				if (!result.next()) {
					return;
				}
				rawPermissions = result.getBytes(1);
			}
		}

		if (rawPermissions == null) {
			return;
		}
		String permissions = new String(rawPermissions, StandardCharsets.UTF_8);
		if (permissions.contains(PERMISSION)) {
			return;
		}
		int end = permissions.lastIndexOf("</permissions>");
		if (end < 0) {
			return;
		}

		String updated = permissions.substring(0, end) + "    " + PERMISSION + "\n" + permissions.substring(end);
		try (PreparedStatement update = connection.prepareStatement("UPDATE ROLES SET PERMISSIONS = ? WHERE ID = ?")) {
			update.setBytes(1, updated.getBytes(StandardCharsets.UTF_8));
			update.setString(2, EMPLOYEE_ROLE_ID);
			update.executeUpdate();
		}
	}
}
