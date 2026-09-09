package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__ticket_line_net_price extends BaseJavaMigration {

	private static final String TEMPLATE = "/com/openbravo/pos/templates/Ticket.Line.xml";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (PreparedStatement statement = connection
				.prepareStatement("UPDATE RESOURCES SET CONTENT = ? WHERE NAME = 'Ticket.Line'")) {
			statement.setBytes(1, readTemplate());
			statement.executeUpdate();
		}
	}

	private byte[] readTemplate() throws IOException {
		try (InputStream input = getClass().getResourceAsStream(TEMPLATE)) {
			if (input == null) {
				throw new IOException("Missing database resource " + TEMPLATE);
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
