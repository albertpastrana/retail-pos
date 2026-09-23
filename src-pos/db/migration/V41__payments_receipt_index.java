package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Indexes the payment-to-receipt join used by recent sales and ticket views.
 */
public class V41__payments_receipt_index extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (!SchemaObjects.indexExists(connection, "PAYMENTS", "PAYMENTS_RECEIPT_INX")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE INDEX PAYMENTS_RECEIPT_INX ON PAYMENTS(RECEIPT)");
			}
		}
	}
}
