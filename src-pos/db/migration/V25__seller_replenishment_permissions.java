package db.migration;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V25__seller_replenishment_permissions extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection c = context.getConnection();
		updateMenu(c);
		try (PreparedStatement select = c.prepareStatement("SELECT PERMISSIONS FROM ROLES WHERE ID = '2'")) {
			try (ResultSet result = select.executeQuery()) {
				if (!result.next() || result.getBytes(1) == null)
					return;
				String permissions = new String(result.getBytes(1), StandardCharsets.UTF_8);
				String updated = permissions;
				String[] required = {"com.openbravo.pos.inventory.ReplenishmentPanel", "Menu.Replenishment.Add"};
				for (String permission : required) {
					String marker = "<class name=\"" + permission + "\"/>";
					if (!updated.contains(marker)) {
						int end = updated.lastIndexOf("</permissions>");
						updated = updated.substring(0, end) + "    " + marker + "\n" + updated.substring(end);
					}
				}
				if (!permissions.equals(updated))
					try (PreparedStatement update = c.prepareStatement("UPDATE ROLES SET PERMISSIONS=? WHERE ID='2'")) {
						update.setBytes(1, updated.getBytes(StandardCharsets.UTF_8));
						update.executeUpdate();
					}
			}
		}
	}

	private void updateMenu(Connection c) throws Exception {
		try (PreparedStatement update = c.prepareStatement("UPDATE RESOURCES SET CONTENT=? WHERE NAME='Menu.Root'")) {
			try (InputStream input = getClass().getResourceAsStream("/com/openbravo/pos/templates/Menu.Root.txt")) {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				byte[] buffer = new byte[4096];
				int count;
				while ((count = input.read(buffer)) != -1)
					output.write(buffer, 0, count);
				update.setBytes(1, output.toByteArray());
				update.executeUpdate();
			}
		}
	}
}
