package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V13__victorines_on_ticket extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		update(connection, "Printer.Ticket", "/com/openbravo/pos/templates/Printer.Ticket.xml");
		update(connection, "Printer.TicketPreview", "/com/openbravo/pos/templates/Printer.TicketPreview.xml");
	}

	private void update(Connection connection, String name, String template) throws Exception {
		try (PreparedStatement statement = connection
				.prepareStatement("UPDATE RESOURCES SET CONTENT = ? WHERE NAME = ?")) {
			statement.setBytes(1, readTemplate(template));
			statement.setString(2, name);
			statement.executeUpdate();
		}
	}

	private byte[] readTemplate(String template) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(template)) {
			if (input == null) {
				throw new IOException("Missing database resource " + template);
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int count;
			while ((count = input.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}
			return output.toByteArray();
		}
	}
}
