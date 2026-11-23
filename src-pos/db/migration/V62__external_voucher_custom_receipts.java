package db.migration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Apply the voucher line to previously customized receipt templates. */
public class V62__external_voucher_custom_receipts extends BaseJavaMigration {
	private static final Logger LOGGER = Logger.getLogger(V62__external_voucher_custom_receipts.class.getName());
	private static final Pattern CASH = paymentCondition("cash");
	private static final Pattern EXTERNAL_VOUCHER = paymentCondition("externalvoucher");
	private static final String BUNDLED_CASH = "#if ($paymentline.name == \"cash\")";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), "%",
				new String[]{"TABLE"})) {
			boolean resourcesExist = false;
			while (tables.next()) {
				if ("RESOURCES".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
					resourcesExist = true;
					break;
				}
			}
			if (!resourcesExist) {
				return;
			}
		}
		updateReceipt(connection, "Printer.Ticket", "Printer.Ticket.xml");
		updateReceipt(connection, "Printer.TicketPreview", "Printer.TicketPreview.xml");
	}

	private static Pattern paymentCondition(String method) {
		return Pattern.compile("#if\\s*\\(\\s*\\$paymentline\\.name\\s*==\\s*(['\"])" + method + "\\1\\s*\\)");
	}

	private void updateReceipt(Connection connection, String resource, String file) throws Exception {
		try (PreparedStatement select = connection.prepareStatement("SELECT CONTENT FROM RESOURCES WHERE NAME = ?");
				PreparedStatement update = connection
						.prepareStatement("UPDATE RESOURCES SET CONTENT = ? WHERE NAME = ?")) {
			select.setString(1, resource);
			try (ResultSet result = select.executeQuery()) {
				if (!result.next() || result.getBytes(1) == null) {
					LOGGER.warning("Missing receipt resource for external voucher: " + resource);
					return;
				}
				String original = new String(result.getBytes(1), StandardCharsets.UTF_8);
				if (EXTERNAL_VOUCHER.matcher(original).find()) {
					return;
				}
				Matcher cash = CASH.matcher(original);
				if (!cash.find()) {
					LOGGER.warning(
							"Cannot add external voucher to receipt resource without a cash condition: " + resource);
					return;
				}
				String template = readTemplate(file);
				String block = template.substring(template.indexOf("#if ($paymentline.name == \"externalvoucher\")"),
						template.indexOf(BUNDLED_CASH));
				String updated = original.substring(0, cash.start()) + block + original.substring(cash.start());
				update.setBytes(1, updated.getBytes(StandardCharsets.UTF_8));
				update.setString(2, resource);
				update.executeUpdate();
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
