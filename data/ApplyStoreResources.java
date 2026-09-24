import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class ApplyStoreResources {
	public static void main(String[] args) throws Exception {
		String db = args[0];
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		Connection c = DriverManager.getConnection("jdbc:derby:" + db);
		c.setAutoCommit(false);
		try {
			File t = templatesDir();
			upsert(c, "Printer.Ticket", 0, new File(t, "Printer.Ticket.xml"));
			upsert(c, "Printer.TicketPreview", 0, new File(t, "Printer.TicketPreview.xml"));
			upsert(c, "Printer.TicketGift", 0, new File(t, "Printer.TicketGift.xml"));
			upsert(c, "Printer.TicketLine", 0, new File(t, "Printer.TicketLine.xml"));
			upsert(c, "Printer.CloseCash", 0, new File(t, "Printer.CloseCash.xml"));
			upsert(c, "Printer.Ticket.Logo", 1, new File(t, "Printer.Ticket.Logo.png"));
			upsert(c, "Window.Logo", 1, new File(t, "Window.Logo.png"));
			upsert(c, "Window.Title", 0, new File(t, "Window.Title.txt"));
			upsert(c, "Button.Print", 1, new File(t, "Button.Print.png"));
			upsert(c, "Button.OpenDrawer", 1, new File(t, "Button.OpenDrawer.png"));
			patchRoles(c);
			c.commit();
		} catch (Exception e) {
			c.rollback();
			throw e;
		} finally {
			c.close();
			try {
				DriverManager.getConnection("jdbc:derby:;shutdown=true");
			} catch (Exception ignored) {
			}
		}
	}

	private static File templatesDir() {
		String home = System.getProperty("pos.home", System.getProperty("user.dir"));
		File[] candidates = {new File(home, "src-pos/com/openbravo/pos/templates"),
				new File("src-pos/com/openbravo/pos/templates"), new File("../src-pos/com/openbravo/pos/templates")};
		for (File dir : candidates) {
			if (new File(dir, "Printer.Ticket.xml").isFile()) {
				return dir;
			}
		}
		throw new IllegalStateException(
				"Cannot find src-pos/com/openbravo/pos/templates. Run ./gradlew applyStoreResources from the repo root.");
	}

	private static void upsert(Connection c, String name, int restype, File file) throws Exception {
		byte[] content = readAll(file.getPath());
		PreparedStatement upd = c.prepareStatement("UPDATE RESOURCES SET CONTENT = ?, RESTYPE = ? WHERE NAME = ?");
		upd.setBytes(1, content);
		upd.setInt(2, restype);
		upd.setString(3, name);
		int n = upd.executeUpdate();
		upd.close();
		if (n == 0) {
			PreparedStatement ins = c
					.prepareStatement("INSERT INTO RESOURCES (ID, NAME, RESTYPE, CONTENT) VALUES (?, ?, ?, ?)");
			ins.setString(1, UUID.randomUUID().toString());
			ins.setString(2, name);
			ins.setInt(3, restype);
			ins.setBytes(4, content);
			ins.executeUpdate();
			ins.close();
			System.out.println("inserted " + name);
		} else {
			System.out.println("updated " + name);
		}
	}

	private static void patchRoles(Connection c) throws Exception {
		String[] extra = {"    <class name=\"button.discount\"/>\n", "    <class name=\"button.discount.total\"/>\n",
				"    <class name=\"sales.EditLines\"/>\n",
				"    <class name=\"com.openbravo.pos.panels.JPanelClosedCash\"/>\n",
				"    <class name=\"com.openbravo.pos.inventory.SaleMarkPanel\"/>\n"};
		PreparedStatement sel = c.prepareStatement("SELECT ID, NAME, PERMISSIONS FROM ROLES");
		ResultSet rs = sel.executeQuery();
		PreparedStatement upd = c.prepareStatement("UPDATE ROLES SET PERMISSIONS = ? WHERE ID = ?");
		while (rs.next()) {
			String id = rs.getString(1);
			String name = rs.getString(2);
			byte[] raw = rs.getBytes(3);
			if (raw == null) {
				continue;
			}
			// Stock databases name these roles "Administrator role" and "Manager
			// role", so an exact match patches nothing.
			if (name == null || (!name.startsWith("Administrator") && !name.startsWith("Manager"))) {
				continue;
			}
			String xml = new String(raw, "UTF-8");
			String next = xml;
			for (String line : extra) {
				String marker = line.trim();
				if (next.contains(marker)) {
					continue;
				}
				int end = next.lastIndexOf("</permissions>");
				if (end < 0) {
					continue;
				}
				next = next.substring(0, end) + line + next.substring(end);
			}
			if (!next.equals(xml)) {
				upd.setBytes(1, next.getBytes("UTF-8"));
				upd.setString(2, id);
				upd.executeUpdate();
				System.out.println("patched role " + name);
			}
		}
		rs.close();
		sel.close();
		upd.close();
	}

	private static byte[] readAll(String path) throws Exception {
		InputStream in = new FileInputStream(path);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = in.read(buf)) > 0) {
			out.write(buf, 0, n);
		}
		in.close();
		return out.toByteArray();
	}
}
