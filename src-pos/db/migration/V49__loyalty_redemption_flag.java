package db.migration;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Makes loyalty redemptions queryable without deserialising ticket lines. */
public class V49__loyalty_redemption_flag extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (PreparedStatement alter = connection
				.prepareStatement("ALTER TABLE TICKETLINES ADD LOYALTYREDEMPTION BOOLEAN DEFAULT FALSE NOT NULL")) {
			alter.executeUpdate();
		}
		backfill(connection);
	}

	private void backfill(Connection connection) throws Exception {
		try (PreparedStatement select = connection.prepareStatement(
				"SELECT TICKET, LINE, ATTRIBUTES FROM TICKETLINES WHERE PRODUCT IS NULL AND PRICE < 0");
				PreparedStatement update = connection.prepareStatement(
						"UPDATE TICKETLINES SET LOYALTYREDEMPTION = TRUE WHERE TICKET = ? AND LINE = ?")) {
			try (ResultSet rows = select.executeQuery()) {
				while (rows.next()) {
					String ticket = rows.getString(1);
					int line = rows.getInt(2);
					if (isRedemption(rows.getBytes(3))) {
						update.setString(1, ticket);
						update.setInt(2, line);
						update.addBatch();
					}
				}
			}
			update.executeBatch();
		}
	}

	private boolean isRedemption(byte[] attributes) {
		if (attributes == null) {
			return false;
		}
		Properties properties = new Properties();
		try {
			properties.loadFromXML(new ByteArrayInputStream(attributes));
			return "true".equals(properties.getProperty("loyalty.redemption"));
		} catch (Exception ignored) {
			return false;
		}
	}
}
