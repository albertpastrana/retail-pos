package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.data.loader.Session;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.inventory.DataLogicReplenishment;
import com.openbravo.pos.inventory.ReplenishmentEntry;
import com.openbravo.pos.inventory.ReplenishmentPanel;
import com.openbravo.pos.inventory.ReplenishmentSupplier;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ReplenishmentSupplierFlowTest {
	@Test
	void creatingFromDetailAndBulkAssignsAndDeletingRestoresAllView() throws Exception {
		String url = "jdbc:derby:memory:replenishmentSupplierFlow" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		Locale previous = Locale.getDefault();
		try {
			AppLocal.setLocale(new Locale("ca"));
			DataLogicReplenishment data = new DataLogicReplenishment();
			data.init(session);
			List<String> ids = new ArrayList<>();
			for (String description : List.of("Samarreta de prova", "Pantalons de prova")) {
				ReplenishmentEntry entry = new ReplenishmentEntry();
				entry.manualDescription = description;
				entry.createdBy = "2";
				ids.add(data.add(entry).id);
			}
			AppUser user = new AppUser("2", "Marta", null, null, "2", null);
			user.setAdministrationMode(true);
			AppUserView view = (AppUserView) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[]{AppUserView.class},
					(proxy, method, args) -> "getUser".equals(method.getName()) ? user : null);
			AppView app = (AppView) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AppView.class},
					(proxy, method, args) -> {
						if ("getAppUserView".equals(method.getName()))
							return view;
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
					panel.setSize(1280, 750);
					layout(panel);
					JTable table = field(panel, "table", JTable.class);
					int samarretaRow = 0;
					while (!table.getValueAt(samarretaRow, 1).toString().contains("Samarreta"))
						samarretaRow++;
					table.setRowSelectionInterval(samarretaRow, samarretaRow);
					field(panel, "detailNote", JTextArea.class).setText("Unsaved note");
					Method assignDetail = ReplenishmentPanel.class.getDeclaredMethod("setDetailSupplier", String.class);
					assignDetail.setAccessible(true);
					java.util.concurrent.atomic.AtomicBoolean called = new java.util.concurrent.atomic.AtomicBoolean();
					Consumer<ReplenishmentSupplier> onDetail = saved -> {
						called.set(true);
						invoke(assignDetail, panel, saved.id);
					};
					JDialog create = form(panel, null, onDetail);
					assertEquals(AppLocal.getIntString("Replenishment.NewSupplier"), create.getTitle());
					fillAndSave(create, "Proveïdor detall");
					assertTrue(called.get(), "Creating from Change assigns the new supplier");
					ReplenishmentSupplier detailSupplier = data.suppliers().get(0);
					assertNotNull(detailSupplier.id);
					assertNull(detailSupplier.whatsapp, "The untouched country prefix is not a phone number");
					ReplenishmentEntry assigned = data.list("Samarreta", "ALL", "PENDING").get(0);
					assertEquals(detailSupplier.id, assigned.supplierId,
							"editing=" + field(panel, "editingEntry", ReplenishmentEntry.class) + " draft="
									+ field(panel, "draftSupplierId", String.class));
					assertEquals("Unsaved note", assigned.note);
					JDialog edit = form(panel, detailSupplier.id, null);
					assertEquals(AppLocal.getIntString("Replenishment.EditSupplier"), edit.getTitle());
					assertEquals("Proveïdor detall", inputs(edit).get(0).getText());
					inputs(edit).get(0).setText("Proveïdor editat");
					edit.getRootPane().getDefaultButton().doClick();
					assertEquals("Proveïdor editat", data.suppliers().get(0).name);
					table.setRowSelectionInterval(0, 1);
					Method assignBulk = ReplenishmentPanel.class.getDeclaredMethod("assignPendingSelected",
							String.class);
					assignBulk.setAccessible(true);
					JDialog bulk = form(panel, null, saved -> invoke(assignBulk, panel, saved.id));
					fillAndSave(bulk, "Proveïdor lot");
					ReplenishmentSupplier bulkSupplier = data.suppliers().stream()
							.filter(s -> "Proveïdor lot".equals(s.name)).findFirst().orElseThrow();
					for (ReplenishmentEntry entry : data.list("", "ALL", "PENDING"))
						assertEquals(bulkSupplier.id, entry.supplierId);
					assertEquals("Unsaved note", data.list("Samarreta", "ALL", "PENDING").get(0).note);
					Method select = ReplenishmentPanel.class.getDeclaredMethod("selectSupplier", String.class);
					select.setAccessible(true);
					invoke(select, panel, bulkSupplier.id);
					assertEquals(bulkSupplier.id, field(panel, "selectedSupplier", String.class));
					assertEquals(
							String.format(AppLocal.getIntString("Replenishment.ConfirmDeleteSupplierRequests"),
									ids.size(), bulkSupplier.name),
							"Les 2 sol·licituds de Proveïdor lot passaran a Sense proveïdor.");
					Method remove = ReplenishmentPanel.class.getDeclaredMethod("removeSupplier",
							ReplenishmentSupplier.class);
					remove.setAccessible(true);
					invoke(remove, panel, bulkSupplier);
					assertEquals("ALL", field(panel, "selectedSupplier", String.class));
					assertFalse(data.suppliers().stream().anyMatch(s -> bulkSupplier.id.equals(s.id)));
					for (ReplenishmentEntry entry : data.list("", "ALL", "PENDING"))
						assertEquals(null, entry.supplierId);
				} catch (Exception failure) {
					throw new AssertionError(failure);
				}
			});
		} finally {
			AppLocal.setLocale(previous);
			session.close();
		}
	}
	private static JDialog form(ReplenishmentPanel panel, String supplierId, Consumer<ReplenishmentSupplier> onCreated)
			throws Exception {
		Method method = ReplenishmentPanel.class.getDeclaredMethod("createSupplierFormDialog", String.class,
				Consumer.class);
		method.setAccessible(true);
		return (JDialog) method.invoke(panel, supplierId, onCreated);
	}
	private static void fillAndSave(JDialog dialog, String name) {
		List<JTextField> fields = inputs(dialog);
		fields.get(0).setText(name);
		fields.get(1).setText("Marta");
		fields.get(2).setText("Soler");
		dialog.getRootPane().getDefaultButton().doClick();
	}
	private static List<JTextField> inputs(Container root) {
		List<JTextField> result = new ArrayList<>();
		for (Component child : root.getComponents()) {
			if (child instanceof JTextField)
				result.add((JTextField) child);
			if (child instanceof Container)
				result.addAll(inputs((Container) child));
		}
		return result;
	}
	private static void invoke(Method method, Object target, Object arg) {
		try {
			method.invoke(target, arg);
		} catch (Exception failure) {
			throw new AssertionError(failure);
		}
	}
	@SuppressWarnings("unchecked")
	private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
		Field field = owner.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(owner);
	}
	private static void layout(Container root) {
		root.doLayout();
		for (Component child : root.getComponents())
			if (child instanceof Container)
				layout((Container) child);
	}
}
