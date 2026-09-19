package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds tendered cash and change details to printed tickets and previews. */
public class V33__cash_payment_details_on_ticket extends BaseJavaMigration {

	private static final String TEMPLATE_PATH = "/com/openbravo/pos/templates/";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		updateResource(connection, "Printer.Ticket", "Printer.Ticket.xml");
		updateResource(connection, "Printer.TicketPreview", "Printer.TicketPreview.xml");
	}

	private void updateResource(Connection connection, String name, String template) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("UPDATE RESOURCES SET CONTENT=? WHERE NAME=?")) {
			statement.setBytes(1, readTemplate(template));
			statement.setString(2, name);
			statement.executeUpdate();
		}
	}

	private byte[] readTemplate(String template) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(TEMPLATE_PATH + template)) {
			if (input == null) {
				throw new IOException("Missing database resource " + template);
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int count;
			while ((count = input.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}
			return output.toByteArray();
		}
	}
}
