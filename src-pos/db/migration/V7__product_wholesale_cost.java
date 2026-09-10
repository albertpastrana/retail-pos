package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V7__product_wholesale_cost extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (SchemaObjects.columnExists(connection, "PRODUCTS", "PRICEBUY_WHOLESALE")) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("ALTER TABLE PRODUCTS ADD COLUMN PRICEBUY_WHOLESALE DOUBLE PRECISION");
		}
	}
}
