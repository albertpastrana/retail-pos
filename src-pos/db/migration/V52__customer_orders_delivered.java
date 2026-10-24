package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Allows customer replenishment entries to be marked as delivered. */
public class V52__customer_orders_delivered extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		// Some baseline-on-migrate installations predate the replenishment table.
		try (ResultSet tables = connection.getMetaData().getTables(null, null, "REPLENISHMENT_ENTRIES", null)) {
			if (!tables.next())
				return;
		}
		try (PreparedStatement drop = connection.prepareStatement(dropConstraintSql(connection))) {
			drop.executeUpdate();
		} catch (SQLException ex) {
			// Baseline-on-migrate test schemas may contain a minimal table without its
			// check.
			if (!"42X86".equals(ex.getSQLState()))
				throw ex;
		}
		try (PreparedStatement add = connection.prepareStatement(
				"ALTER TABLE REPLENISHMENT_ENTRIES ADD CONSTRAINT REPLENISHMENT_STATUS CHECK (STATUS IN ('PENDING', 'ORDERED', 'RECEIVED', 'DELIVERED'))")) {
			add.executeUpdate();
		}
	}

	private String dropConstraintSql(Connection connection) throws Exception {
		String database = connection.getMetaData().getDatabaseProductName();
		return database.toLowerCase().contains("mysql")
				? "ALTER TABLE REPLENISHMENT_ENTRIES DROP CHECK REPLENISHMENT_STATUS"
				: "ALTER TABLE REPLENISHMENT_ENTRIES DROP CONSTRAINT REPLENISHMENT_STATUS";
	}
}
