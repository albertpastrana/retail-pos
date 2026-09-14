package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__seed_resources extends BaseJavaMigration {

	private static final String TEMPLATE_PATH = "/com/openbravo/pos/templates/";

	private static final String[][] ROLES = {{"0", "Role.Administrator.xml"}, {"1", "Role.Manager.xml"},
			{"2", "Role.Employee.xml"}, {"3", "Role.Guest.xml"}};

	private static final ResourceSeed[] RESOURCES = {resource("0", "Printer.Start", 0, "Printer.Start.xml"),
			resource("1", "Printer.Ticket", 0, "Printer.Ticket.xml"),
			resource("2", "Printer.Ticket2", 0, "Printer.Ticket2.xml"),
			resource("3", "Printer.TicketPreview", 0, "Printer.TicketPreview.xml"),
			resource("4", "Printer.TicketTotal", 0, "Printer.TicketTotal.xml"),
			resource("5", "Printer.OpenDrawer", 0, "Printer.OpenDrawer.xml"),
			resource("6", "Printer.Ticket.Logo", 1, "Printer.Ticket.Logo.png"),
			resource("7", "Printer.TicketLine", 0, "Printer.TicketLine.xml"),
			resource("8", "Printer.CloseCash", 0, "Printer.CloseCash.xml"),
			resource("9", "Window.Logo", 1, "Window.Logo.png"), resource("10", "Window.Title", 0, "Window.Title.txt"),
			resource("11", "Ticket.Buttons", 0, "Ticket.Buttons.xml"),
			resource("12", "Ticket.Line", 0, "Ticket.Line.xml"),
			resource("13", "Printer.Inventory", 0, "Printer.Inventory.xml"),
			resource("14", "Menu.Root", 0, "Menu.Root.txt"),
			resource("15", "Printer.CustomerPaid", 0, "Printer.CustomerPaid.xml"),
			resource("16", "Printer.CustomerPaid2", 0, "Printer.CustomerPaid2.xml"),
			resource("17", "payment.cash", 0, "payment.cash.txt"),
			resource("18", "banknote.50euro", 1, "banknote.50euro.png"),
			resource("19", "banknote.20euro", 1, "banknote.20euro.png"),
			resource("20", "banknote.10euro", 1, "banknote.10euro.png"),
			resource("21", "banknote.5euro", 1, "banknote.5euro.png"),
			resource("22", "coin.2euro", 1, "coin.2euro.png"), resource("23", "coin.1euro", 1, "coin.1euro.png"),
			resource("24", "coin.50cent", 1, "coin.50cent.png"), resource("25", "coin.20cent", 1, "coin.20cent.png"),
			resource("26", "coin.10cent", 1, "coin.10cent.png"), resource("27", "coin.5cent", 1, "coin.5cent.png"),
			resource("28", "coin.2cent", 1, "coin.2cent.png"), resource("29", "coin.1cent", 1, "coin.1cent.png"),
			resource("30", "Printer.PartialCash", 0, "Printer.PartialCash.xml"),
			resource("31", "Script.Discount", 0, "Script.Discount.txt"),
			resource("32", "Script.DiscountTotal2", 0, "Script.DiscountTotal2.txt"),
			resource("33", "Printer.TicketGift", 0, "Printer.TicketGift.xml")};

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		seedRoles(connection);
		seedResources(connection);
	}

	private void seedRoles(Connection connection) throws SQLException, IOException {
		try (PreparedStatement statement = connection
				.prepareStatement("UPDATE ROLES SET PERMISSIONS = ? WHERE ID = ?")) {
			for (String[] role : ROLES) {
				statement.setBytes(1, readTemplate(role[1]));
				statement.setString(2, role[0]);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private void seedResources(Connection connection) throws SQLException, IOException {
		try (PreparedStatement statement = connection
				.prepareStatement("INSERT INTO RESOURCES (ID, NAME, RESTYPE, CONTENT) VALUES (?, ?, ?, ?)")) {
			for (ResourceSeed resource : RESOURCES) {
				statement.setString(1, resource.id);
				statement.setString(2, resource.name);
				statement.setInt(3, resource.type);
				statement.setBytes(4, readTemplate(resource.file));
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private byte[] readTemplate(String file) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(TEMPLATE_PATH + file)) {
			if (input == null) {
				throw new IOException("Missing database resource " + TEMPLATE_PATH + file);
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

	private static ResourceSeed resource(String id, String name, int type, String file) {
		return new ResourceSeed(id, name, type, file);
	}

	private static final class ResourceSeed {
		private final String id;
		private final String name;
		private final int type;
		private final String file;

		private ResourceSeed(String id, String name, int type, String file) {
			this.id = id;
			this.name = name;
			this.type = type;
			this.file = file;
		}
	}
}
