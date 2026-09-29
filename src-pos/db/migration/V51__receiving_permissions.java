package db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Give receiving to roles already allowed to maintain the stock diary. */
public class V51__receiving_permissions extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection c = context.getConnection();
		try (PreparedStatement select = c.prepareStatement("SELECT ID,PERMISSIONS FROM ROLES");
				ResultSet roles = select.executeQuery()) {
			while (roles.next()) {
				byte[] bytes = roles.getBytes(2);
				if (bytes == null)
					continue;
				String xml = new String(bytes, StandardCharsets.UTF_8);
				String grant = "<class name=\"com.openbravo.pos.inventory.StockReceivingPanel\"/>";
				if (!xml.contains("com.openbravo.pos.inventory.StockDiaryPanel") || xml.contains(grant))
					continue;
				int end = xml.lastIndexOf("</permissions>");
				if (end < 0)
					continue;
				xml = xml.substring(0, end) + "    " + grant + "\n" + xml.substring(end);
				try (PreparedStatement update = c.prepareStatement("UPDATE ROLES SET PERMISSIONS=? WHERE ID=?")) {
					update.setBytes(1, xml.getBytes(StandardCharsets.UTF_8));
					update.setString(2, roles.getString(1));
					update.executeUpdate();
				}
			}
		}
	}
}
