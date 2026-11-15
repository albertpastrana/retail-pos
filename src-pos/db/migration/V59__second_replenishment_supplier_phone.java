package db.migration;

import java.sql.Connection;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds an optional second mobile number to shared supplier contacts. */
public class V59__second_replenishment_supplier_phone extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (Statement sql = connection.createStatement()) {
			sql.executeUpdate("ALTER TABLE REPLENISHMENT_SUPPLIERS ADD COLUMN WHATSAPP_2 VARCHAR(64)");
		}
	}
}
