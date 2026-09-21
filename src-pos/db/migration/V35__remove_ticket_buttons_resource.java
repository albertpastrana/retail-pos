package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Core till actions are now defined by JPanelButtons instead of RESOURCES. */
public class V35__remove_ticket_buttons_resource extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM RESOURCES WHERE NAME = ?")) {
			statement.setString(1, "Ticket.Buttons");
			statement.executeUpdate();
		}
	}
}
