package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Add shared external-voucher naming and preserve the label on each payment.
 */
public class V61__external_voucher_payment extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE EXTERNAL_VOUCHER_SETTINGS (ID VARCHAR(255) NOT NULL PRIMARY KEY, "
					+ "NAME VARCHAR(255) NOT NULL)");
		}
		if (tableExists(connection, "PAYMENTS")) {
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("ALTER TABLE PAYMENTS ADD COLUMN PAYMENTLABEL VARCHAR(255)");
			}
		}
		try (PreparedStatement insert = connection
				.prepareStatement("INSERT INTO EXTERNAL_VOUCHER_SETTINGS (ID, NAME) VALUES (?, ?)")) {
			insert.setString(1, "0");
			insert.setString(2, "");
			insert.executeUpdate();
		}
		if (tableExists(connection, "ROLES")) {
			grantExistingCashRoles(connection);
		}
		if (tableExists(connection, "RESOURCES")) {
			addReceiptLine(connection, "Printer.Ticket", "Printer.Ticket.xml");
			addReceiptLine(connection, "Printer.TicketPreview", "Printer.TicketPreview.xml");
		}
	}

	private boolean tableExists(Connection connection, String table) throws Exception {
		try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), "%",
				new String[]{"TABLE"})) {
			while (tables.next()) {
				if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
					return true;
				}
			}
			return false;
		}
	}

	private void grantExistingCashRoles(Connection connection) throws Exception {
		try (PreparedStatement select = connection.prepareStatement("SELECT ID, PERMISSIONS FROM ROLES");
				ResultSet roles = select.executeQuery();
				PreparedStatement update = connection
						.prepareStatement("UPDATE ROLES SET PERMISSIONS = ? WHERE ID = ?")) {
			while (roles.next()) {
				byte[] raw = roles.getBytes(2);
				if (raw == null) {
					continue;
				}
				String xml = new String(raw, StandardCharsets.UTF_8);
				String cash = "<class name=\"payment.cash\"/>";
				String voucher = "<class name=\"payment.externalvoucher\"/>";
				if (xml.contains(cash) && !xml.contains(voucher)) {
					update.setBytes(1, xml.replace(cash, cash + "\n    " + voucher).getBytes(StandardCharsets.UTF_8));
					update.setString(2, roles.getString(1));
					update.executeUpdate();
				}
			}
		}
	}

	private void addReceiptLine(Connection connection, String resource, String template) throws Exception {
		String source = readTemplate(template);
		String marker = "#if ($paymentline.name == \"cash\")";
		String blockStart = "#if ($paymentline.name == \"externalvoucher\")";
		String block = source.substring(source.indexOf(blockStart), source.indexOf(marker));
		try (PreparedStatement select = connection.prepareStatement("SELECT CONTENT FROM RESOURCES WHERE NAME = ?");
				PreparedStatement update = connection
						.prepareStatement("UPDATE RESOURCES SET CONTENT = ? WHERE NAME = ?")) {
			select.setString(1, resource);
			try (ResultSet result = select.executeQuery()) {
				if (result.next() && result.getBytes(1) != null) {
					String old = new String(result.getBytes(1), StandardCharsets.UTF_8);
					if (old.contains(marker) && !old.contains(blockStart)) {
						update.setBytes(1, old.replace(marker, block + marker).getBytes(StandardCharsets.UTF_8));
						update.setString(2, resource);
						update.executeUpdate();
					}
				}
			}
		}
	}

	private String readTemplate(String file) throws Exception {
		try (InputStream input = getClass().getResourceAsStream("/com/openbravo/pos/templates/" + file)) {
			if (input == null) {
				throw new IllegalStateException("Missing receipt template " + file);
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int count;
			while ((count = input.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		}
	}
}
