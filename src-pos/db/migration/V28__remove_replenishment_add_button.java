package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Removes the obsolete add-product button from the sales screen resource. */
public class V28__remove_replenishment_add_button extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		try (PreparedStatement statement = context.getConnection()
				.prepareStatement("UPDATE RESOURCES SET CONTENT=? WHERE NAME=?")) {
			statement.setBytes(1, readTemplate());
			statement.setString(2, "Ticket.Buttons");
			statement.executeUpdate();
		}
	}

	private byte[] readTemplate() throws IOException {
		try (InputStream input = getClass().getResourceAsStream("/com/openbravo/pos/templates/Ticket.Buttons.xml")) {
			if (input == null) {
				throw new IOException("Missing database resource Ticket.Buttons.xml");
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
