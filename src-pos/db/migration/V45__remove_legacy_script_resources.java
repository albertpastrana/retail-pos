package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Removes resources that are no longer interpreted by the application. */
public class V45__remove_legacy_script_resources extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (PreparedStatement statement = connection
				.prepareStatement("DELETE FROM RESOURCES WHERE NAME IN (?, ?, ?)")) {
			statement.setString(1, "payment.cash");
			statement.setString(2, "Script.Discount");
			statement.setString(3, "Script.DiscountTotal2");
			statement.executeUpdate();
		}
	}
}
