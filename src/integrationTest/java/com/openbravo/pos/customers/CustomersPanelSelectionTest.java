package com.openbravo.pos.customers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.openbravo.basic.BasicException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class CustomersPanelSelectionTest {
	@Test
	void initialSelectionShowsFirstCustomerAndAllowsReselectingIt() throws Exception {
		CustomersPanel panel = new CustomersPanel();
		Field customers = CustomersPanel.class.getDeclaredField("customers");
		customers.setAccessible(true);
		customers.set(panel, new DataLogicCustomers() {
			@Override
			public List<CustomerInfoExt> searchCustomerSummaries(String value, boolean debtOnly, boolean inactive)
					throws BasicException {
				CustomerInfoExt first = new CustomerInfoExt("first");
				first.setName("Aina");
				first.setVisible(true);
				CustomerInfoExt second = new CustomerInfoExt("second");
				second.setName("Berta");
				second.setVisible(true);
				return value.isEmpty() ? List.of(first, second) : value.equals("Berta") ? List.of(second) : List.of();
			}
		});
		JTable table = field(panel, "table", JTable.class);
		JLabel title = field(panel, "detailTitle", JLabel.class);
		JTextField search = field(panel, "search", JTextField.class);
		SwingUtilities.invokeAndWait(() -> {
			try {
				panel.activate();
			} catch (BasicException e) {
				throw new RuntimeException(e);
			}
		});
		awaitTitle(title, "Aina");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(0, table.getSelectedRow());
			table.setRowSelectionInterval(1, 1);
			assertEquals("Berta", title.getText());
			table.setRowSelectionInterval(0, 0);
			assertEquals("Aina", title.getText());
			search.setText("Berta");
		});
		awaitTitle(title, "Berta");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(1, table.getRowCount());
			assertEquals(0, table.getSelectedRow());
			search.setText("nobody");
		});
		awaitTitle(title, "");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(0, table.getRowCount());
			search.setText("Berta");
		});
		awaitTitle(title, "Berta");
	}

	private static void awaitTitle(JLabel title, String expected) throws Exception {
		AtomicReference<String> actual = new AtomicReference<>();
		for (int i = 0; i < 100; i++) {
			SwingUtilities.invokeAndWait(() -> actual.set(title.getText()));
			if (expected.equals(actual.get()))
				return;
			Thread.sleep(20);
		}
		assertEquals(expected, actual.get());
	}

	private static <T> T field(CustomersPanel panel, String name, Class<T> type) throws Exception {
		Field field = CustomersPanel.class.getDeclaredField(name);
		field.setAccessible(true);
		return type.cast(field.get(panel));
	}
}
