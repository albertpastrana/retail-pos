package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.data.loader.Session;
import com.openbravo.data.user.BrowsableData;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.ticket.TaxInfo;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductBatchNavigatorUiTest {
	@TempDir
	Path temp;

	@Test
	void applySavesImmediatelyAndConflictedRowCanBeRetriedWhileOthersCanBeUndone() throws Exception {
		Session session = new Session("jdbc:derby:" + temp.resolve("products") + ";create=true", null, null);
		try {
			try (Statement sql = session.getConnection().createStatement()) {
				sql.executeUpdate("CREATE TABLE PRICE_RULES (ID VARCHAR(30), BRAND VARCHAR(30), "
						+ "MARKUP_PERCENT DOUBLE, ROUNDING VARCHAR(30))");
				sql.executeUpdate("INSERT INTO PRICE_RULES VALUES ('default', NULL, 47.5, 'CHARM')");
				sql.executeUpdate("INSERT INTO PRICE_RULES VALUES ('plain', 'PLAIN', 47.5, 'NONE')");
				sql.executeUpdate(
						"CREATE TABLE CATEGORIES (ID VARCHAR(30), NAME VARCHAR(30), PARENTID VARCHAR(30), IMAGE BLOB)");
				sql.executeUpdate("INSERT INTO CATEGORIES (ID, NAME) VALUES ('c1', 'Shirts'), ('c2', 'Coats')");
				sql.executeUpdate("CREATE TABLE PRODUCTS (ID VARCHAR(30), REFERENCE VARCHAR(30), NAME VARCHAR(30), "
						+ "BRAND VARCHAR(30), PRICEBUY DOUBLE, PRICESELL DOUBLE, CATEGORY VARCHAR(30), "
						+ "TAXCAT VARCHAR(30), UPDATED_AT TIMESTAMP)");
				for (int i = 0; i < 4; i++)
					sql.executeUpdate("INSERT INTO PRODUCTS "
							+ "(ID, REFERENCE, NAME, PRICEBUY, PRICESELL, CATEGORY, TAXCAT) VALUES " + "('p" + i
							+ "', 'FAMILY-" + i + "', 'Shirt', 0, 10, 'c1', 'vat')");
				sql.executeUpdate("UPDATE PRODUCTS SET BRAND = 'PLAIN' WHERE ID = 'p2'");
			}
			DataLogicSales sales = new DataLogicSales();
			sales.init(session);
			BrowsableData list = new BrowsableData(null);
			List<Object[]> rows = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				Object[] row = new Object[18];
				row[0] = "p" + i;
				row[1] = "FAMILY-SIZE-EXTRA-LONG-" + i;
				row[3] = "Shirt";
				row[6] = 10.0;
				row[7] = "c1";
				row[8] = "vat";
				rows.add(row);
			}
			list.loadList(rows);
			BrowsableEditableData data = new BrowsableEditableData(list, new EmptyEditor(), new DirtyManager());
			TaxesLogic taxes = new TaxesLogic(
					Arrays.asList(new TaxInfo("tax", "VAT", "vat", new Date(0), null, null, 0.21, false, 0)));
			StubPanel owner = new StubPanel(session);
			AtomicReference<ProductTableNavigator> navigator = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				try {
					navigator.set(new ProductTableNavigator(data, taxes, sales, owner));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			ProductTableNavigator ui = navigator.get();
			SwingUtilities.invokeAndWait(() -> {
				JTable table = (JTable) ((JScrollPane) ui.getComponent(0)).getViewport().getView();
				assertEquals("PVP", table.getColumnName(4));
				assertEquals(80, table.getColumnModel().getColumn(4).getPreferredWidth());
				assertEquals(90, table.getColumnModel().getColumn(4).getMaxWidth());
				javax.swing.JLabel status = (javax.swing.JLabel) ((JPanel) ui.getComponent(1)).getComponent(0);
				assertFalse(status.isVisible());
				assertEquals(5, table.getColumnCount());
				for (int i = 0; i < 4; i++)
					table.getModel().setValueAt(true, i, 0);
				assertFalse(status.isVisible());
				assertTrue(component(ui.getBatchEditor(), JTextArea.class, 1).getText().isEmpty());
				List<javax.swing.JLabel> labels = new ArrayList<>();
				collect(ui.getBatchEditor(), javax.swing.JLabel.class, labels);
				assertTrue(labels.stream().anyMatch(label -> "FAMILY-SIZE-EXTRA-LONG-0".equals(label.getText())));
				assertFalse(table.getModel().isCellEditable(0, 3));
				assertFalse(table.getModel().isCellEditable(0, 4));
				assertFalse(table.editCellAt(0, 4));
				table.getModel().setValueAt("30", 0, 4);
				assertFalse(ui.hasPending());
				assertTrue(table.getValueAt(0, 4).toString().contains("12"));
				assertTrue(ui.getBatchEditor().getComponentCount() > 0);
				JTextField amount = component(ui.getBatchEditor(), JTextField.class, 0);
				amount.setText("12,10");
				assertFalse(((javax.swing.JLabel) table.prepareRenderer(table.getCellRenderer(0, 4), 0, 4)).getText()
						.contains("<strike>"));
				assertFalse(button(ui.getBatchEditor(), "batch.apply").isEnabled());
				JComboBox<?> category = component(ui.getBatchEditor(), JComboBox.class, 0);
				assertEquals(AppLocal.getIntString("batch.noChange"), category.getSelectedItem().toString());
				category.setSelectedIndex(1);
				assertFalse(((javax.swing.JLabel) table.prepareRenderer(table.getCellRenderer(0, 4), 0, 4)).getText()
						.contains("<strike>"));
				assertTrue(((javax.swing.JLabel) table.prepareRenderer(table.getCellRenderer(0, 3), 0, 3)).getText()
						.contains("<strike>Shirts</strike>"));
				amount.setText("12,50");
				assertTrue(table.getValueAt(0, 4).toString().contains("12"));
				assertTrue(table.getValueAt(0, 3).toString().contains("Coats"));
				String preview = component(ui.getBatchEditor(), JTextArea.class, 1).getText();
				assertTrue(preview.contains(AppLocal.getIntString("batch.previewCategory")));
				assertTrue(preview.contains(AppLocal.getIntString("batch.previewPrice")));
				assertFalse(preview.contains("?"));
				assertTrue(((javax.swing.JLabel) table.prepareRenderer(table.getCellRenderer(0, 4), 0, 4)).getText()
						.contains("<strike>"));
				assertFalse(ui.hasPending());
				category.setSelectedIndex(2);
				assertTrue(table.getValueAt(0, 3).toString().contains("Shirts"));
				assertFalse(((javax.swing.JLabel) table.prepareRenderer(table.getCellRenderer(0, 3), 0, 3)).getText()
						.contains("<strike>"));
				category.setSelectedIndex(1);
			});
			assertEquals(10.0, net(session, "p0"), 0.0001);
			try (Statement sql = session.getConnection().createStatement()) {
				sql.executeUpdate("UPDATE PRODUCTS SET PRICESELL = 15 WHERE ID = 'p1'");
			}
			SwingUtilities.invokeAndWait(() -> {
				button(ui.getBatchEditor(), "batch.apply").doClick();
				assertTrue(((javax.swing.JLabel) ((JPanel) ui.getComponent(1)).getComponent(0)).isVisible());
				assertEquals(AppLocal.getIntString("batch.noChange"),
						component(ui.getBatchEditor(), JComboBox.class, 0).getSelectedItem().toString());
				button(ui, "batch.clearSelection").doClick();
				assertTrue(owner.batchExpanded);
				assertTrue(button(ui.getBatchEditor(), "batch.discard").isEnabled());
				assertTrue(button(ui.getBatchEditor(), "batch.retry").isEnabled());
			});
			assertTrue(ui.hasPending());
			assertEquals(12.5 / 1.21, net(session, "p0"), 0.0001);
			assertEquals("c2", category(session, "p0"));
			assertEquals(15.0, net(session, "p1"), 0.0001);
			SwingUtilities.invokeAndWait(() -> button(ui.getBatchEditor(), "batch.retry").doClick());
			assertFalse(ui.hasPending());
			assertFalse(owner.batchExpanded);
			assertEquals(12.5 / 1.21, net(session, "p1"), 0.0001);
			SwingUtilities.invokeAndWait(() -> button(ui, "batch.undoSaved").doClick());
			assertEquals(10.0, net(session, "p0"), 0.0001);
			assertEquals("c1", category(session, "p0"));
			assertEquals(15.0, net(session, "p1"), 0.0001);
			SwingUtilities.invokeAndWait(() -> {
				ui.stageEditorDraft(ProductBatchDraft.classify("p0", 10.0, "c1", 12.5 / 1.21, "c1", 12.5, false));
				assertTrue(ui.hasPending());
				assertTrue(component(ui.getBatchEditor(), JTextArea.class, 1).getText().isEmpty());
				assertEquals(10.0, tableNet(data, 0));
				button(ui.getBatchEditor(), "batch.apply").doClick();
				assertFalse(ui.hasPending());
				assertEquals(12.5 / 1.21, tableNet(data, 0), 0.0001);
				button(ui, "batch.undoSaved").doClick();
				assertEquals(10.0, tableNet(data, 0));
				ui.clearSelection();
				JTable table = (JTable) ((JScrollPane) ui.getComponent(0)).getViewport().getView();
				table.getModel().setValueAt(true, 0, 0);
				table.getModel().setValueAt(true, 2, 0);
				component(ui.getBatchEditor(), JToggleButton.class, 1).doClick();
				JTextField amount = component(ui.getBatchEditor(), JTextField.class, 0);
				amount.setText("10");
				assertTrue(table.getValueAt(0, 4).toString().contains("13"));
				assertTrue(table.getValueAt(2, 4).toString().contains("13"));
				assertFalse(table.getValueAt(0, 4).equals(table.getValueAt(2, 4)));
				assertFalse(ui.hasPending());
				button(ui.getBatchEditor(), "batch.apply").doClick();
				assertFalse(ui.hasPending());
			});
			assertEquals(13.95 / 1.21, net(session, "p0"), 0.0001);
			assertEquals(13.31 / 1.21, net(session, "p2"), 0.0001);
			SwingUtilities.invokeAndWait(() -> {
				JTable table = (JTable) ((JScrollPane) ui.getComponent(0)).getViewport().getView();
				component(ui.getBatchEditor(), JComboBox.class, 0).setSelectedIndex(1);
				assertFalse(ui.hasPending());
				assertTrue(((javax.swing.JLabel) table.prepareRenderer(table.getCellRenderer(0, 3), 0, 3)).getText()
						.contains("<strike>Shirts</strike>"));
				button(ui.getBatchEditor(), "batch.discard").doClick();
				assertFalse(ui.hasPending());
				assertFalse(((javax.swing.JLabel) table.prepareRenderer(table.getCellRenderer(0, 3), 0, 3)).getText()
						.contains("<strike>"));
			});
			assertEquals(13.95 / 1.21, net(session, "p0"), 0.0001);
			assertEquals("c1", category(session, "p0"));
			assertThreeLocaleLayout(session);
		} finally {
			session.close();
		}
	}

	private double tableNet(BrowsableEditableData data, int row) {
		return ((Number) ((Object[]) data.getListModel().getElementAt(row))[6]).doubleValue();
	}

	private void assertThreeLocaleLayout(Session session) throws Exception {
		Locale previous = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("ca"), new Locale("es")}) {
				AppLocal.setLocale(locale);
				DataLogicSales sales = new DataLogicSales();
				sales.init(session);
				BrowsableData list = new BrowsableData(null);
				Object[] first = new Object[18], second = new Object[18];
				first[0] = "p0";
				first[1] = "FAMILY-M";
				first[3] = "Shirt";
				first[6] = 10.0;
				first[7] = "c1";
				first[8] = "vat";
				second[0] = "p2";
				second[1] = "FAMILY-XL";
				second[3] = "Shirt";
				second[6] = 10.0;
				second[7] = "c1";
				second[8] = "vat";
				list.loadList(Arrays.asList(first, second));
				BrowsableEditableData data = new BrowsableEditableData(list, new EmptyEditor(), new DirtyManager());
				SwingUtilities.invokeAndWait(() -> {
					try {
						ProductTableNavigator ui = new ProductTableNavigator(data, new TaxesLogic(List.of()), sales,
								new StubPanel(session));
						JTable table = (JTable) ((JScrollPane) ui.getComponent(0)).getViewport().getView();
						assertEquals("PVP", table.getColumnName(4), locale.toString());
						table.getModel().setValueAt(true, 0, 0);
						table.getModel().setValueAt(true, 1, 0);
						JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ui, ui.getBatchEditor());
						JPanel screen = new JPanel(new BorderLayout());
						screen.add(split);
						screen.setSize(900, 650);
						screen.doLayout();
						split.setDividerLocation(0.5);
						split.doLayout();
						assertTrue(Math
								.abs(split.getLeftComponent().getWidth() - split.getRightComponent().getWidth()) < 20,
								locale.toString());
						for (String key : new String[]{"batch.apply", "batch.clearSelection"}) {
							JButton button = button(key.equals("batch.apply") ? ui.getBatchEditor() : ui, key);
							assertTrue(button.getPreferredSize().height >= 48);
							assertTrue(button.getPreferredSize().width < split.getRightComponent().getWidth(),
									locale.toString());
						}
						assertEquals(AppLocal.getIntString("batch.noChange"),
								component(ui.getBatchEditor(), JComboBox.class, 0).getSelectedItem().toString());
						for (int i = 0; i < 3; i++)
							assertTrue(component(ui.getBatchEditor(), JToggleButton.class, i)
									.getPreferredSize().height >= 48);
						BufferedImage image = new BufferedImage(900, 650, BufferedImage.TYPE_INT_RGB);
						screen.printAll(image.createGraphics());
						JButton clear = button(ui, "batch.clearSelection");
						JButton discard = button(ui.getBatchEditor(), "batch.discard");
						JButton apply = button(ui.getBatchEditor(), "batch.apply");
						assertTrue(discard.getPreferredSize().width < split.getRightComponent().getWidth(),
								locale.toString());
						assertTrue(SwingUtilities.convertPoint(apply, apply.getWidth(), 0, ui.getBatchEditor()).x <= ui
								.getBatchEditor().getWidth(), locale.toString());
						assertTrue(SwingUtilities.convertPoint(clear, clear.getWidth(), 0, ui).x <= ui.getWidth(),
								locale.toString());
						assertTrue(Math.abs(
								SwingUtilities.convertPoint(discard, 0, discard.getHeight() / 2, ui.getBatchEditor()).y
										- SwingUtilities.convertPoint(apply, 0, apply.getHeight() / 2,
												ui.getBatchEditor()).y) < 8,
								locale.toString());
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				});
			}
		} finally {
			AppLocal.setLocale(previous);
		}
	}

	private double net(Session session, String id) throws Exception {
		try (Statement sql = session.getConnection().createStatement();
				ResultSet result = sql.executeQuery("SELECT PRICESELL FROM PRODUCTS WHERE ID = '" + id + "'")) {
			result.next();
			return result.getDouble(1);
		}
	}
	private String category(Session session, String id) throws Exception {
		try (Statement sql = session.getConnection().createStatement();
				ResultSet result = sql.executeQuery("SELECT CATEGORY FROM PRODUCTS WHERE ID = '" + id + "'")) {
			result.next();
			return result.getString(1);
		}
	}

	private <T extends Component> T component(Container root, Class<T> type, int index) {
		List<T> matches = new ArrayList<>();
		collect(root, type, matches);
		return matches.get(index);
	}

	private <T extends Component> void collect(Container root, Class<T> type, List<T> matches) {
		for (Component child : root.getComponents()) {
			if (type.isInstance(child))
				matches.add(type.cast(child));
			if (child instanceof Container)
				collect((Container) child, type, matches);
		}
	}

	private JButton button(Container root, String key) {
		List<JButton> buttons = new ArrayList<>();
		collect(root, JButton.class, buttons);
		return buttons.stream().filter(b -> b.getText().startsWith(AppLocal.getIntString(key))).findFirst()
				.orElseThrow();
	}

	private static final class StubPanel extends ProductsPanel {
		private final Session session;
		private boolean batchExpanded;
		StubPanel(Session session) {
			this.session = session;
		}
		@Override
		Session getSession() {
			return session;
		}
		@Override
		boolean prepareBatchEdit() {
			return true;
		}
		@Override
		void batchStateChanged(boolean expanded) {
			batchExpanded = expanded;
		}
		@Override
		void refreshBatchEditor() {
		}
	}

	private static final class EmptyEditor implements EditorRecord {
		@Override
		public Object createValue() {
			return null;
		}
		@Override
		public void writeValueEOF() {
		}
		@Override
		public void writeValueInsert() {
		}
		@Override
		public void writeValueEdit(Object value) {
		}
		@Override
		public void writeValueDelete(Object value) {
		}
		@Override
		public void refresh() {
		}
		@Override
		public Component getComponent() {
			return new JPanel();
		}
	}
}
