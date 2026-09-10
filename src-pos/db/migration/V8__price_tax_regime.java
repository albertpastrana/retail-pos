package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V8__price_tax_regime extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (SchemaObjects.columnExists(connection, "PRICE_RULES", "TAX_REGIME")) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("ALTER TABLE PRICE_RULES ADD COLUMN TAX_REGIME VARCHAR(32)");
			statement.execute("UPDATE PRICE_RULES SET TAX_REGIME = 'EQUIVALENCE_SURCHARGE' WHERE BRAND IS NULL");
		}
	}
}
