package db.migration;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Moves shared loyalty settings out of the hostname-keyed resource properties.
 */
public class V47__shared_loyalty_settings extends BaseJavaMigration {

	private static final String DEFAULT_NAME = "";
	private static final double DEFAULT_SPEND_PER_STAMP = 10.0;
	private static final double DEFAULT_REDEMPTION_VALUE = 5.0;

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE LOYALTY_SETTINGS ("
					+ "ID VARCHAR(255) NOT NULL, ENABLED BOOLEAN NOT NULL, NAME VARCHAR(255) NOT NULL, "
					+ "ELIGIBLE_SPEND_PER_STAMP DOUBLE PRECISION NOT NULL, REDEMPTION_VALUE DOUBLE PRECISION NOT NULL, "
					+ "PRIMARY KEY (ID))");
		}

		Properties legacy = findLegacySettings(connection);
		try (PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO LOYALTY_SETTINGS (ID, ENABLED, NAME, ELIGIBLE_SPEND_PER_STAMP, REDEMPTION_VALUE) "
						+ "VALUES (?, ?, ?, ?, ?)")) {
			statement.setString(1, "0");
			statement.setBoolean(2, Boolean.parseBoolean(legacy.getProperty("loyalty.enabled", "false")));
			statement.setString(3, legacy.getProperty("loyalty.name", DEFAULT_NAME));
			statement.setDouble(4,
					positive(legacy.getProperty("loyalty.eligible_spend_per_stamp"), DEFAULT_SPEND_PER_STAMP));
			statement.setDouble(5, positive(legacy.getProperty("loyalty.redemption_value"), DEFAULT_REDEMPTION_VALUE));
			statement.executeUpdate();
		}
	}

	private Properties findLegacySettings(Connection connection) throws Exception {
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT CONTENT FROM RESOURCES WHERE NAME LIKE ? ORDER BY NAME")) {
			statement.setString(1, "%/properties");
			try (ResultSet result = statement.executeQuery()) {
				if (result.next() && result.getBytes(1) != null) {
					Properties properties = new Properties();
					properties.loadFromXML(new ByteArrayInputStream(result.getBytes(1)));
					return properties;
				}
			}
		}
		return new Properties();
	}

	private double positive(String value, double defaultValue) {
		try {
			double parsed = Double.parseDouble(value);
			return Double.isFinite(parsed) && parsed > 0.0 ? parsed : defaultValue;
		} catch (RuntimeException e) {
			return defaultValue;
		}
	}
}
