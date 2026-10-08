package com.openbravo.pos.customers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.openbravo.basic.BasicException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class CustomersPanelSelectionTest {
	@Test
	void formattedDebtCanBeReadBackWithoutMarkingCustomerDirty() throws Exception {
		CustomersPanel panel = new CustomersPanel();
		CustomerInfoExt customer = new CustomerInfoExt("first");
		customer.setName("Aina");
		customer.setMaxdebt(1234.0);
		Method show = CustomersPanel.class.getDeclaredMethod("showCustomer", CustomerInfoExt.class);
		Method dirty = CustomersPanel.class.getDeclaredMethod("isDirty");
		Method parse = CustomersPanel.class.getDeclaredMethod("parseMaxDebt");
		show.setAccessible(true);
		dirty.setAccessible(true);
		parse.setAccessible(true);
		JTextField maxDebt = field(panel, "maxDebt", JTextField.class);
		SwingUtilities.invokeAndWait(() -> {
			try {
				show.invoke(panel, customer);
				assertEquals("1.234,00", maxDebt.getText());
				assertEquals(false, dirty.invoke(panel));
				maxDebt.setText("1250.50");
				assertEquals(1250.50, (double) parse.invoke(panel));
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
	}

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

	@Test
	void emptySearchDoesNotDiscardUnsavedDetails() throws Exception {
		CustomersPanel panel = new CustomersPanel();
		Field customers = CustomersPanel.class.getDeclaredField("customers");
		customers.setAccessible(true);
		customers.set(panel, new DataLogicCustomers() {
			@Override
			public List<CustomerInfoExt> searchCustomerSummaries(String value, boolean debtOnly, boolean inactive) {
				if (!value.isEmpty())
					return List.of();
				CustomerInfoExt customer = new CustomerInfoExt("first");
				customer.setName("Aina");
				customer.setVisible(true);
				return List.of(customer);
			}
		});
		JLabel title = field(panel, "detailTitle", JLabel.class);
		JTextField search = field(panel, "search", JTextField.class);
		JTextField name = field(panel, "name", JTextField.class);
		JTable table = field(panel, "table", JTable.class);
		SwingUtilities.invokeAndWait(() -> {
			try {
				panel.activate();
			} catch (BasicException e) {
				throw new RuntimeException(e);
			}
		});
		awaitTitle(title, "Aina");
		SwingUtilities.invokeAndWait(() -> {
			name.setText("Unsaved Aina");
			search.setText("nobody");
		});
		awaitRows(table, 0);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals("Unsaved Aina", name.getText());
			assertEquals("Aina", title.getText());
			assertEquals(-1, table.getSelectedRow());
		});
	}

	private static void awaitRows(JTable table, int expected) throws Exception {
		AtomicReference<Integer> actual = new AtomicReference<>();
		for (int i = 0; i < 100; i++) {
			SwingUtilities.invokeAndWait(() -> actual.set(table.getRowCount()));
			if (expected == actual.get())
				return;
			Thread.sleep(20);
		}
		assertEquals(expected, actual.get());
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
