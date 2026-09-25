package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Removes the legacy cash denomination controls and their payment template. */
public class V48__remove_cash_denomination_resources extends BaseJavaMigration {
	private static final String[] RESOURCE_NAMES = {"payment.cash", "banknote.50euro", "banknote.20euro",
			"banknote.10euro", "banknote.5euro", "coin.2euro", "coin.1euro", "coin.50cent", "coin.20cent",
			"coin.10cent", "coin.5cent", "coin.2cent", "coin.1cent"};

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (PreparedStatement statement = connection
				.prepareStatement("DELETE FROM RESOURCES WHERE NAME IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			for (int i = 0; i < RESOURCE_NAMES.length; i++) {
				statement.setString(i + 1, RESOURCE_NAMES[i]);
			}
			statement.executeUpdate();
		}
	}
}
