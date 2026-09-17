package db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Makes privileged sales controls available before supervisor authorization.
 */
public class V23__seller_privileged_sales_permissions extends BaseJavaMigration {

	private static final String SELLER_ROLE_ID = "2";
	private static final String[] PERMISSIONS = {"    <class name=\"button.opendrawer\"/>\n",
			"    <class name=\"button.discount\"/>\n", "    <class name=\"button.discount.total\"/>\n",
			"    <class name=\"com.openbravo.pos.panels.JPanelCloseMoney\"/>\n"};

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		byte[] rawPermissions;
		try (PreparedStatement select = connection.prepareStatement("SELECT PERMISSIONS FROM ROLES WHERE ID = ?")) {
			select.setString(1, SELLER_ROLE_ID);
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
		String updated = permissions;
		for (String permission : PERMISSIONS) {
			String marker = permission.trim();
			if (!updated.contains(marker)) {
				int end = updated.lastIndexOf("</permissions>");
				if (end >= 0) {
					updated = updated.substring(0, end) + permission + updated.substring(end);
				}
			}
		}

		if (!permissions.equals(updated)) {
			try (PreparedStatement update = connection
					.prepareStatement("UPDATE ROLES SET PERMISSIONS = ? WHERE ID = ?")) {
				update.setBytes(1, updated.getBytes(StandardCharsets.UTF_8));
				update.setString(2, SELLER_ROLE_ID);
				update.executeUpdate();
			}
		}
	}
}
