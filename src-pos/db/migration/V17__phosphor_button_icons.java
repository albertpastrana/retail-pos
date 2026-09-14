package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V17__phosphor_button_icons extends BaseJavaMigration {

	private static final String[][] ICONS = {{"35", "Button.Print"}, {"34", "Button.OpenDrawer"}};

	private static final String TEMPLATES = "/com/openbravo/pos/templates/";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		for (String[] icon : ICONS) {
			try (PreparedStatement statement = connection.prepareStatement("DELETE FROM RESOURCES WHERE NAME = ?")) {
				statement.setString(1, icon[1]);
				statement.executeUpdate();
			}
			try (PreparedStatement statement = connection
					.prepareStatement("INSERT INTO RESOURCES (ID, NAME, RESTYPE, CONTENT) VALUES (?, ?, 1, ?)")) {
				statement.setString(1, icon[0]);
				statement.setString(2, icon[1]);
				statement.setBytes(3, readTemplate(icon[1] + ".png"));
				statement.executeUpdate();
			}
		}
	}

	private byte[] readTemplate(String file) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(TEMPLATES + file)) {
			if (input == null) {
				throw new IOException("Missing database resource " + TEMPLATES + file);
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
