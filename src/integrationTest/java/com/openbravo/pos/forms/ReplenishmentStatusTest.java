package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.openbravo.data.loader.Session;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.inventory.DataLogicReplenishment;
import com.openbravo.pos.inventory.ReplenishmentEntry;
import com.openbravo.pos.inventory.ReplenishmentPanel;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.util.UUID;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ReplenishmentStatusTest {
	@Test
	void refreshingSelectedRequestKeepsUnsavedNote() throws Exception {
		String url = "jdbc:derby:memory:replenishmentRefresh" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			DataLogicReplenishment data = new DataLogicReplenishment();
			data.init(session);
			ReplenishmentEntry request = entry(null);
			data.add(request);
			AppUser user = new AppUser("user", "Employee", null, null, "2", null);
			AppUserView userView = (AppUserView) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[]{AppUserView.class},
					(proxy, method, args) -> "getUser".equals(method.getName()) ? user : null);
			AppView app = (AppView) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AppView.class},
					(proxy, method, args) -> {
						if ("getAppUserView".equals(method.getName()))
							return userView;
						if ("getBean".equals(method.getName())) {
							if (args[0] == DataLogicReplenishment.class)
								return data;
							if (args[0] == DataLogicSales.class)
								return new DataLogicSales();
							if (args[0] == DataLogicCustomers.class)
								return new DataLogicCustomers();
							throw new BeanFactoryException();
						}
						return null;
					});
			SwingUtilities.invokeAndWait(() -> {
				try {
					ReplenishmentPanel panel = new ReplenishmentPanel();
					panel.init(app);
					panel.activate();
					JTable table = first(panel, JTable.class);
					table.setRowSelectionInterval(0, 0);
					JTextArea note = editableArea(panel);
					note.setText("Keep this note");
					JTextField search = first(panel, JTextField.class);
					search.setText("Test item");
					search.postActionEvent();
					assertEquals(0, table.getSelectedRow());
					assertEquals("Keep this note", note.getText());
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			});
		} finally {
			session.close();
		}
	}
	private static <T extends Component> T first(Container root, Class<T> type) {
		for (Component child : root.getComponents()) {
			if (type.isInstance(child))
				return type.cast(child);
			if (child instanceof Container) {
				T match = first((Container) child, type);
				if (match != null)
					return match;
			}
		}
		return null;
	}
	private static JTextArea editableArea(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JTextArea && ((JTextArea) child).isEditable())
				return (JTextArea) child;
			if (child instanceof Container) {
				JTextArea match = editableArea((Container) child);
				if (match != null)
					return match;
			}
		}
		return null;
	}
	@Test
	void onlyCustomerOrdersCanBeDelivered() throws Exception {
		String url = "jdbc:derby:memory:replenishmentStatus" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		DataLogicReplenishment data = new DataLogicReplenishment();
		data.init(new Session(url, null, null));

		ReplenishmentEntry replenishment = entry(null);
		data.add(replenishment);
		data.updateStatus(replenishment.id, "RECEIVED", "user");
		assertThrows(com.openbravo.basic.BasicException.class,
				() -> data.updateStatus(replenishment.id, "DELIVERED", "user"));

		replenishment.customerId = "customer-1";
		replenishment.customerName = "Customer";
		replenishment.status = "RECEIVED";
		data.update(replenishment, "user");
		data.updateStatus(replenishment.id, "DELIVERED", "user");
		assertEquals("DELIVERED", data.list("", "ALL", "DELIVERED").get(0).status);
		assertThrows(com.openbravo.basic.BasicException.class,
				() -> data.updateDetails(replenishment.id, "new note", null, null, "user"));
		assertEquals("customer-1", data.list("", "ALL", "DELIVERED").get(0).customerId);
		assertEquals(0, data.list("", "ENCARGO", "OPEN").size());
		data.updateStatus(replenishment.id, "PENDING", "user");
		assertEquals("PENDING", data.list("", "ENCARGO", "OPEN").get(0).status);
	}

	@Test
	void editingSavedRequestPreservesProductAndStatusAndResolvesCreatorName() throws Exception {
		String url = "jdbc:derby:memory:replenishmentEdit" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		DataLogicReplenishment data = new DataLogicReplenishment();
		data.init(new Session(url, null, null));
		ReplenishmentEntry request = entry(null);
		request.createdBy = "2";
		request.manualEan = "12345";
		data.add(request);
		data.updateStatus(request.id, "RECEIVED", "2");
		data.updateDetails(request.id, "Call on arrival", "customer-2", "Maria", "2");
		ReplenishmentEntry saved = data.list("", "ALL", "RECEIVED").get(0);
		assertEquals("12345", saved.manualEan);
		assertEquals("Test item", saved.manualDescription);
		assertEquals("RECEIVED", saved.status);
		assertEquals("Maria", saved.customerName);
		assertEquals("Call on arrival", saved.note);
		assertEquals("Employee", saved.creatorName);
	}

	@Test
	void formSavesCustomerNoteAndStatusTogetherWithoutChangingProduct() throws Exception {
		String url = "jdbc:derby:memory:replenishmentForm" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		DataLogicReplenishment data = new DataLogicReplenishment();
		data.init(new Session(url, null, null));
		ReplenishmentEntry order = entry(null);
		order.manualEan = "56789";
		data.add(order);
		assertThrows(com.openbravo.basic.BasicException.class,
				() -> data.updateDetailsAndStatus(order.id, "Too early", "customer", "Anna", "DELIVERED", "user"));
		assertEquals("PENDING", data.list("", "ALL", "ALL").get(0).status);
		assertEquals(null, data.list("", "ALL", "ALL").get(0).note);
		data.updateStatus(order.id, "RECEIVED", "user");
		data.updateDetailsAndStatus(order.id, "Collect at counter", "customer", "Anna", "DELIVERED", "user");
		ReplenishmentEntry delivered = data.list("", "ALL", "DELIVERED").get(0);
		assertEquals("56789", delivered.manualEan);
		assertEquals("Test item", delivered.manualDescription);
		assertEquals("Collect at counter", delivered.note);
		assertEquals("Anna", delivered.customerName);
		data.updateDetailsAndStatus(order.id, "Reopened", null, null, "PENDING", "user");
		assertEquals(null, data.list("", "ALL", "PENDING").get(0).customerId);
	}

	private ReplenishmentEntry entry(String customerId) {
		ReplenishmentEntry entry = new ReplenishmentEntry();
		entry.manualDescription = "Test item";
		entry.customerId = customerId;
		entry.customerName = customerId == null ? null : "Customer";
		entry.createdBy = "user";
		return entry;
	}
}
