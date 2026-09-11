package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V11__product_gift_voucher extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			if (!SchemaObjects.columnExists(connection, "PRODUCTS", "ISVOUCHER")) {
				statement.execute("ALTER TABLE PRODUCTS ADD COLUMN ISVOUCHER BOOLEAN DEFAULT FALSE NOT NULL");
			}
			statement.execute("UPDATE PRODUCTS SET ISVOUCHER = TRUE WHERE ID IN "
					+ "('gift-voucher-10', 'gift-voucher-20', 'gift-voucher-50', 'gift-voucher-100')");
		}
	}
}
