package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Shared supplier contacts and assignment on replenishment requests. */
public class V58__replenishment_suppliers extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (Statement sql = connection.createStatement()) {
			sql.executeUpdate("CREATE TABLE REPLENISHMENT_SUPPLIERS (ID VARCHAR(255) NOT NULL PRIMARY KEY, "
					+ "NAME VARCHAR(255) NOT NULL, CONTACT_FIRST_NAME VARCHAR(255) NOT NULL, "
					+ "CONTACT_LAST_NAME VARCHAR(255) NOT NULL, WHATSAPP VARCHAR(64), EMAIL VARCHAR(255))");
			// Some legacy databases baseline Flyway after the replenishment migration
			// without having installed that optional table.
			boolean hasEntries = false;
			try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
				while (tables.next())
					hasEntries |= "REPLENISHMENT_ENTRIES".equalsIgnoreCase(tables.getString("TABLE_NAME"));
			}
			if (!hasEntries)
				return;
			sql.executeUpdate("ALTER TABLE REPLENISHMENT_ENTRIES ADD COLUMN SUPPLIER_ID VARCHAR(255)");
			sql.executeUpdate("ALTER TABLE REPLENISHMENT_ENTRIES ADD CONSTRAINT REPLENISHMENT_SUPPLIER_FK "
					+ "FOREIGN KEY (SUPPLIER_ID) REFERENCES REPLENISHMENT_SUPPLIERS(ID)");
			sql.executeUpdate("CREATE INDEX REPLENISHMENT_SUPPLIER_INX ON REPLENISHMENT_ENTRIES(SUPPLIER_ID)");
		}
	}
}
