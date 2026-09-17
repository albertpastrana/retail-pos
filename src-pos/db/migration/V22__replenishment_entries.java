package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Stores the shared, app-owned replenishment capture list. */
public class V22__replenishment_entries extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection c = context.getConnection();
		if (tableExists(c)) {
			updateResource(c, "Menu.Root", "/com/openbravo/pos/templates/Menu.Root.txt");
			updateResource(c, "Ticket.Buttons", "/com/openbravo/pos/templates/Ticket.Buttons.xml");
			updateRole(c, "0", "/com/openbravo/pos/templates/Role.Administrator.xml");
			updateRole(c, "1", "/com/openbravo/pos/templates/Role.Manager.xml");
			updateRole(c, "2", "/com/openbravo/pos/templates/Role.Employee.xml");
			return;
		}
		try (PreparedStatement s = c.prepareStatement("CREATE TABLE REPLENISHMENT_ENTRIES ("
				+ "ID VARCHAR(255) NOT NULL, PRODUCT_ID VARCHAR(255), PRODUCT_REFERENCE VARCHAR(255), "
				+ "PRODUCT_NAME VARCHAR(255), PRODUCT_EAN VARCHAR(255), VARIANT_SIZE VARCHAR(255), "
				+ "VARIANT_COLOUR VARCHAR(255), MANUAL_DESCRIPTION VARCHAR(255), MANUAL_EAN VARCHAR(255), "
				+ "NOTE VARCHAR(255), CUSTOMER_ID VARCHAR(255), CUSTOMER_NAME VARCHAR(255), STATUS VARCHAR(20) NOT NULL, "
				+ "OPEN_PRODUCT_ID VARCHAR(255), CREATED_AT TIMESTAMP NOT NULL, UPDATED_AT TIMESTAMP NOT NULL, "
				+ "CREATED_BY VARCHAR(255) NOT NULL, UPDATED_BY VARCHAR(255) NOT NULL, PRIMARY KEY (ID), "
				+ "CONSTRAINT REPLENISHMENT_STATUS CHECK (STATUS IN ('PENDING', 'ORDERED', 'RECEIVED')))")) {
			s.executeUpdate();
		}

		try (PreparedStatement s = c.prepareStatement(
				"CREATE UNIQUE INDEX REPLENISHMENT_OPEN_PRODUCT_INX ON REPLENISHMENT_ENTRIES(OPEN_PRODUCT_ID)")) {
			s.executeUpdate();
		}
		try (PreparedStatement s = c.prepareStatement(
				"CREATE INDEX REPLENISHMENT_STATUS_INX ON REPLENISHMENT_ENTRIES(STATUS, CREATED_AT)")) {
			s.executeUpdate();
		}
		updateResource(c, "Menu.Root", "/com/openbravo/pos/templates/Menu.Root.txt");
		updateResource(c, "Ticket.Buttons", "/com/openbravo/pos/templates/Ticket.Buttons.xml");
		updateRole(c, "0", "/com/openbravo/pos/templates/Role.Administrator.xml");
		updateRole(c, "1", "/com/openbravo/pos/templates/Role.Manager.xml");
		updateRole(c, "2", "/com/openbravo/pos/templates/Role.Employee.xml");
	}

	private boolean tableExists(Connection c) throws SQLException {
		try (java.sql.ResultSet tables = c.getMetaData().getTables(null, null, "REPLENISHMENT_ENTRIES", null)) {
			return tables.next();
		}
	}

	private void updateResource(Connection c, String name, String resource) throws SQLException, IOException {
		try (PreparedStatement s = c.prepareStatement("UPDATE RESOURCES SET CONTENT=? WHERE NAME=?")) {
			s.setBytes(1, read(resource));
			s.setString(2, name);
			s.executeUpdate();
		}
	}

	private void updateRole(Connection c, String id, String resource) throws SQLException, IOException {
		try (PreparedStatement s = c.prepareStatement("UPDATE ROLES SET PERMISSIONS=? WHERE ID=?")) {
			s.setBytes(1, read(resource));
			s.setString(2, id);
			s.executeUpdate();
		}
	}

	private byte[] read(String resource) throws IOException {
		try (InputStream in = getClass().getResourceAsStream(resource)) {
			if (in == null)
				throw new IOException("Missing database resource " + resource);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int count;
			while ((count = in.read(buffer)) != -1)
				out.write(buffer, 0, count);
			return out.toByteArray();
		}
	}
}
