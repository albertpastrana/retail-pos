package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V16__opendrawer_eject_icon extends BaseJavaMigration {

	private static final String NAME = "Button.OpenDrawer";
	private static final String TEMPLATE = "/com/openbravo/pos/templates/Button.OpenDrawer.png";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("DELETE FROM RESOURCES WHERE NAME = '" + NAME + "'");
		}
		try (PreparedStatement statement = connection
				.prepareStatement("INSERT INTO RESOURCES (ID, NAME, RESTYPE, CONTENT) VALUES ('34', ?, 1, ?)")) {
			statement.setString(1, NAME);
			statement.setBytes(2, readTemplate());
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
