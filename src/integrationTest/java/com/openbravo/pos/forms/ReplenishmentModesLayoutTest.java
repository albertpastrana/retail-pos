package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.inventory.DataLogicReplenishment;
import com.openbravo.pos.inventory.ReplenishmentEntry;
import com.openbravo.pos.inventory.ReplenishmentPanel;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class ReplenishmentModesLayoutTest {
	@Test
	void longProductNamesStopAtTwoLinesWithEllipsis() throws Exception {
		Method preview = ReplenishmentPanel.class.getDeclaredMethod("preview", String.class, int.class);
		preview.setAccessible(true);
		String text = "Samarreta interior de cotó de màniga llarga amb estampat de flors i una descripció llarga";
		String shown = (String) preview.invoke(null, text, 140);
		assertTrue(shown.split("\n", -1).length <= 2);
		assertTrue(shown.endsWith("…"));
	}
	@Test
	void salesAndAdministrationRebuildFromApplicationMode() throws Exception {
		String url = "jdbc:derby:memory:replenishmentModes" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		Locale originalLocale = Locale.getDefault();
		javax.swing.LookAndFeel originalTheme = UIManager.getLookAndFeel();
		Session session = new Session(url, null, null);
		try {
			DataLogicReplenishment data = new DataLogicReplenishment();
			data.init(session);
			try (java.sql.Statement sql = session.getConnection().createStatement()) {
				sql.executeUpdate("INSERT INTO CUSTOMERS (ID, SEARCHKEY, NAME, PHONE) "
						+ "VALUES ('customer', 'customer', 'Montserrat Ferrer', '34600123456')");
			}
			for (int i = 0; i < 3; i++) {
				ReplenishmentEntry entry = new ReplenishmentEntry();
				entry.createdBy = "2";
				entry.manualDescription = "Samarreta de cotó molt suau " + i;
				entry.customerId = "customer";
				entry.customerName = "Montserrat Ferrer";
				data.add(entry);
			}
			assertEquals(3, data.list("600 123 456", "ALL", "PENDING").size());
			AppUser user = new AppUser("2", "Marta", null, null, AppUser.ROLE_SELLER, null);
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
			RetailPOSTheme.registerDefaultsSource();
			Path screenshots = Path.of("build", "reports", "replenishment-screenshots");
			Files.createDirectories(screenshots);
			for (javax.swing.LookAndFeel theme : new javax.swing.LookAndFeel[]{new FlatLightLaf(), new FlatDarkLaf()}) {
				UIManager.setLookAndFeel(theme);
				RetailPOSTheme.applyFonts();
				for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
					AppLocal.setLocale(locale);
					SwingUtilities.invokeAndWait(() -> {
						try {
							ReplenishmentPanel panel = new ReplenishmentPanel();
							panel.init(app);
							user.setAdministrationMode(false);
							panel.activate();
							for (int width : new int[]{1000, 1280, 1470}) {
								panel.setSize(width, 750);
								layout(panel);
								layout(panel);
								layout(panel);
								layout(panel);
								layout(panel);
								assertSales(panel);
								layout(panel);
								layout(panel);
								if (width == 1000) {
									Method showCustomer = ReplenishmentPanel.class
											.getDeclaredMethod("setFormCustomerText", String.class, String.class);
									showCustomer.setAccessible(true);
									showCustomer.invoke(panel, "Montserrat Ferrer", "34600123456");
									layout(panel);
									javax.swing.JLabel phone = field(panel, "selectedCustomerPhone",
											javax.swing.JLabel.class);
									assertTrue(phone.isVisible());
									assertEquals("34600123456", phone.getText());
									assertEquals(13f, phone.getFont().getSize2D());
									capture(panel, screenshots, "sales-customer-phone", theme, locale, width);
									showCustomer.invoke(panel, AppLocal.getIntString("Replenishment.NoCustomer"), null);
									layout(panel);
									assertFalse(phone.isVisible());
								}
								capture(panel, screenshots, "sales", theme, locale, width);
								field(panel, "table", JTable.class).setRowSelectionInterval(0, 0);
								layout(panel);
								capture(panel, screenshots, "sales-detail", theme, locale, width);
								field(panel, "table", JTable.class).clearSelection();
							}
							user.setAdministrationMode(true);
							panel.activate();
							for (int width : new int[]{1000, 1280, 1470}) {
								panel.setSize(width, 750);
								layout(panel);
								layout(panel);
								layout(panel);
								layout(panel);
								layout(panel);
								assertAdmin(panel);
								layout(panel);
								layout(panel);
								capture(panel, screenshots, "admin", theme, locale, width);
								field(panel, "table", JTable.class).setRowSelectionInterval(0, 0);
								layout(panel);
								capture(panel, screenshots, "admin-detail", theme, locale, width);
								field(panel, "table", JTable.class).clearSelection();
							}
							panel.setSize(900, 750);
							layout(panel);
							assertFalse(field(panel, "supplierSidebar", JPanel.class).isVisible());
							assertTrue(((JComboBox<?>) field(panel, "supplierFilter", JComboBox.class)).getParent()
									.isVisible(), "Below 980 px admin uses the supplier combo");
							panel.setSize(1470, 750);
							layout(panel);
							if (theme instanceof FlatLightLaf && "ca".equals(locale.getLanguage())) {
								field(panel, "newRequestButton", javax.swing.JButton.class).doClick();
								assertTrue(field(panel, "detailDock", JPanel.class).isVisible());
								field(panel, "formCode", javax.swing.JTextField.class).setText("A-ADMIN");
								((javax.swing.JTextArea) field(panel, "formDescription",
										javax.swing.text.JTextComponent.class)).setText("Sol·licitud administrativa");
								field(panel, "save", javax.swing.JButton.class).doClick();
								assertFalse(field(panel, "detailDock", JPanel.class).isVisible(),
										"Admin list reclaims space after saving a new request");
							}
							user.setAdministrationMode(false);
							panel.activate();
							layout(panel);
							assertSales(panel);
							if (theme instanceof FlatLightLaf && "ca".equals(locale.getLanguage()))
								verifySalesFlow(panel, data, screenshots, theme, locale);
						} catch (Exception failure) {
							throw new AssertionError(failure);
						}
					});
				}
			}
		} finally {
			AppLocal.setLocale(originalLocale);
			UIManager.setLookAndFeel(originalTheme);
			session.close();
		}
	}
	private static void assertSales(ReplenishmentPanel panel) throws Exception {
		JTable table = field(panel, "table", JTable.class);
		assertFalse(field(panel, "supplierSidebar", JPanel.class).isVisible());
		assertFalse(((JComboBox<?>) field(panel, "supplierFilter", JComboBox.class)).getParent().isVisible());
		assertFalse(field(panel, "footerActions", JPanel.class).isVisible());
		assertTrue(field(panel, "detailDock", JPanel.class).isVisible());
		assertTrue(field(panel, "detailDock", JPanel.class).getWidth() >= 340);
		assertTrue(field(panel, "detailDock", JPanel.class).getWidth() <= 380);
		assertToolbarHeights(panel);
		assertEquals(-1, table.convertColumnIndexToView(0));
		assertEquals(-1, table.convertColumnIndexToView(5));
		assertTrue(table.convertColumnIndexToView(2) >= 0);
		javax.swing.JTextArea note = field(panel, "formNoteArea", javax.swing.JTextArea.class);
		javax.swing.JScrollPane noteScroll = (javax.swing.JScrollPane) note.getParent().getParent();
		assertTrue(noteScroll.getHeight() >= note.getPreferredSize().height,
				"Form note keeps its four content rows: " + noteScroll.getHeight());
		assertTrue(noteScroll.getHeight() <= noteScroll.getPreferredSize().height + 2,
				"Form note does not expand with the viewport");
		JPanel formChoices = field(panel, "formStatusButtons", JPanel.class);
		for (Component choice : formChoices.getComponents())
			if (choice.isVisible())
				assertTrue(choice.getHeight() >= choice.getPreferredSize().height,
						"Form status choice must not be clipped");
		javax.swing.JLabel customerLabel = field(panel, "selectedCustomer", javax.swing.JLabel.class);
		assertEquals(javax.swing.JLabel.CENTER, customerLabel.getVerticalAlignment());
		assertTrue(customerLabel.getWidth() >= customerLabel.getFontMetrics(customerLabel.getFont())
				.stringWidth(customerLabel.getToolTipText()), "Full customer value remains readable");
		assertFalse(field(panel, "selectedCustomerPhone", javax.swing.JLabel.class).isVisible());
		assertTabRow(panel);
		assertVisibleButtonsFit(panel);
		int customerRow = customerRow(table);
		table.setRowSelectionInterval(customerRow, customerRow);
		layout(panel);
		assertEquals(1, table.getSelectedRowCount());
		assertFalse(field(panel, "detailSupplierChange", javax.swing.JButton.class).isVisible());
		javax.swing.JLabel customerPhone = field(panel, "detailCustomerPhone", javax.swing.JLabel.class);
		assertTrue(customerPhone.isVisible());
		assertEquals("34600123456", customerPhone.getText());
		assertNextStepHeight(panel);
		panel.getActionMap().get("closeDetail").actionPerformed(null);
		assertEquals(0, table.getSelectedRowCount(), "Escape returns to the new-request form");
		table.setRowSelectionInterval(customerRow, customerRow);
		field(panel, "closeDetail", javax.swing.JButton.class).doClick();
		assertEquals(0, table.getSelectedRowCount(), "Close returns to the new-request form");
	}
	private static void assertAdmin(ReplenishmentPanel panel) throws Exception {
		JTable table = field(panel, "table", JTable.class);
		assertTrue(field(panel, "supplierSidebar", JPanel.class).isVisible());
		assertFalse(field(panel, "detailDock", JPanel.class).isVisible());
		assertTrue(field(panel, "footerActions", JPanel.class).isVisible());
		assertToolbarHeights(panel);
		if (panel.getWidth() == 1470) {
			JPanel toolbar = field(panel, "toolbar", JPanel.class);
			assertTrue(toolbar.getHeight() < 100,
					"Wide toolbar: " + toolbar.getHeight() + " / " + toolbar.getPreferredSize() + " width="
							+ toolbar.getWidth() + " holder="
							+ field(panel, "segmentHolder", JPanel.class).getPreferredSize());
		}
		assertTabRow(panel);
		assertVisibleButtonsFit(panel);
		int customerRow = customerRow(table);
		table.setRowSelectionInterval(customerRow, customerRow);
		layout(panel);
		JPanel dock = field(panel, "detailDock", JPanel.class);
		assertTrue(dock.isVisible());
		assertTrue(dock.getWidth() <= 380, "Detail width: " + dock.getWidth());
		assertNextStepHeight(panel);
		javax.swing.JLabel badge = field(panel, "detailStatus", javax.swing.JLabel.class);
		assertTrue(badge.getHeight() <= badge.getPreferredSize().height + 2);
		javax.swing.JLabel customer = field(panel, "detailCustomer", javax.swing.JLabel.class);
		javax.swing.JLabel customerPhone = field(panel, "detailCustomerPhone", javax.swing.JLabel.class);
		assertTrue(customerPhone.isVisible());
		assertEquals("34600123456", customerPhone.getText());
		javax.swing.JButton change = field(panel, "detailCustomerButton", javax.swing.JButton.class);
		assertTrue(Math.abs(customer.getParent().getY() + customer.getParent().getHeight() / 2 - change.getY()
				- change.getHeight() / 2) <= 2);
		table.addRowSelectionInterval(customerRow == 0 ? 1 : 0, customerRow == 0 ? 1 : 0);
		assertEquals(2, table.getSelectedRowCount());
		table.clearSelection();
		layout(panel);
		assertFalse(dock.isVisible());
	}
	private static void assertNextStepHeight(ReplenishmentPanel panel) throws Exception {
		javax.swing.JButton next = field(panel, "nextStep", javax.swing.JButton.class);
		int minimum = com.formdev.flatlaf.util.UIScale.scale(48);
		assertTrue(next.getHeight() >= minimum && next.getHeight() <= minimum + 4,
				"Detail action: " + next.getHeight() + " preferred=" + next.getPreferredSize().height);
	}
	private static int customerRow(JTable table) {
		for (int row = 0; row < table.getRowCount(); row++)
			if ("Montserrat Ferrer".equals(table.getModel().getValueAt(table.convertRowIndexToModel(row), 2)))
				return row;
		throw new AssertionError("Customer request missing from the list");
	}
	private static void assertTabRow(ReplenishmentPanel panel) throws Exception {
		JPanel toolbar = field(panel, "toolbar", JPanel.class);
		assertTrue(toolbar.getHeight() >= 44, "Status and search toolbar stays touch-sized");
		JPanel segments = field(panel, "statusToggles", JPanel.class);
		javax.swing.JViewport viewport = (javax.swing.JViewport) segments.getParent();
		assertTrue(viewport.getHeight() >= segments.getComponent(0).getPreferredSize().height,
				"Status labels remain visible above the horizontal scrollbar at " + panel.getWidth() + ": viewport="
						+ viewport.getHeight() + ", button=" + segments.getComponent(0).getPreferredSize().height
						+ ", scroll=" + viewport.getParent().getHeight() + ", pref="
						+ viewport.getParent().getPreferredSize() + ", holder="
						+ field(panel, "segmentHolder", JPanel.class).getSize() + "/"
						+ field(panel, "segmentHolder", JPanel.class).getPreferredSize() + ", toolbar="
						+ toolbar.getSize() + "/" + toolbar.getPreferredSize());
		int top = segments.getComponent(0).getY();
		for (Component segment : segments.getComponents())
			assertEquals(top, segment.getY(), "Status segments must never wrap");
	}
	private static void assertToolbarHeights(ReplenishmentPanel panel) throws Exception {
		int scale40 = com.formdev.flatlaf.util.UIScale.scale(40);
		int scale44 = com.formdev.flatlaf.util.UIScale.scale(44);
		JPanel segments = field(panel, "statusToggles", JPanel.class);
		for (Component segment : segments.getComponents())
			assertTrue(segment.getHeight() >= scale40 && segment.getHeight() <= scale40 + 3,
					"Status segment height: " + segment.getHeight());
		javax.swing.JTextField search = field(panel, "search", javax.swing.JTextField.class);
		javax.swing.JButton create = field(panel, "newRequestButton", javax.swing.JButton.class);
		for (Component component : new Component[]{search, create})
			assertTrue(component.getHeight() >= scale44 && component.getHeight() <= scale44 + 4,
					"Toolbar control height: " + component.getHeight());
	}
	private static void assertVisibleButtonsFit(Container root) {
		for (Component child : root.getComponents()) {
			if (!child.isVisible())
				continue;
			if (child instanceof javax.swing.JButton button && button.getWidth() > 0) {
				int needed = button.getFontMetrics(button.getFont()).stringWidth(button.getText())
						+ button.getInsets().left + button.getInsets().right;
				if (button.getIcon() != null)
					needed += button.getIcon().getIconWidth() + button.getIconTextGap();
				assertTrue(button.getWidth() >= needed, button.getText() + ": " + button.getWidth() + " < " + needed);
			}
			if (child instanceof Container)
				assertVisibleButtonsFit((Container) child);
		}
	}
	private static void verifySalesFlow(ReplenishmentPanel panel, DataLogicReplenishment data, Path screenshots,
			javax.swing.LookAndFeel theme, Locale locale) throws Exception {
		JTable table = field(panel, "table", JTable.class);
		javax.swing.JTextField code = field(panel, "formCode", javax.swing.JTextField.class);
		code.setText("990000");
		((javax.swing.JTextArea) field(panel, "formDescription", javax.swing.text.JTextComponent.class))
				.setText("Brusa del taulell");
		field(panel, "save", javax.swing.JButton.class).doClick();
		assertEquals("", code.getText(), "Saving clears the scanner field");
		assertTrue(data.list("Brusa del taulell", "ALL", "PENDING").size() == 1);
		layout(panel);
		capture(panel, screenshots, "sales-flow-new", theme, locale, 1470);
		@SuppressWarnings("unchecked")
		java.util.Map<String, javax.swing.JToggleButton> statuses = field(panel, "statusButtons", java.util.Map.class);
		statuses.get("ALL").doClick();
		javax.swing.JTextField search = field(panel, "search", javax.swing.JTextField.class);
		search.setText("600 123 456");
		search.postActionEvent();
		assertEquals(3, table.getRowCount(), "Customer phone finds the three requests");
		search.setText("Montserrat");
		search.postActionEvent();
		assertEquals(3, table.getRowCount(), "Customer name finds the three requests");
		table.setRowSelectionInterval(0, 0);
		layout(panel);
		capture(panel, screenshots, "sales-flow-find", theme, locale, 1470);
		javax.swing.JButton next = field(panel, "nextStep", javax.swing.JButton.class);
		next.doClick();
		next.doClick();
		next.doClick();
		assertEquals(1, data.list("Montserrat", "ALL", "DELIVERED").size());
		layout(panel);
		capture(panel, screenshots, "sales-flow-delivered", theme, locale, 1470);
	}
	@SuppressWarnings("unchecked")
	private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
		Field field = owner.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(owner);
	}
	private static void capture(ReplenishmentPanel panel, Path directory, String mode, javax.swing.LookAndFeel theme,
			Locale locale, int width) throws Exception {
		BufferedImage image = new BufferedImage(width, 750, BufferedImage.TYPE_INT_RGB);
		panel.paint(image.getGraphics());
		ImageIO.write(image, "png", directory.resolve(mode + "-" + (theme instanceof FlatDarkLaf ? "dark" : "light")
				+ "-" + locale.getLanguage() + "-" + width + ".png").toFile());
	}
	private static void layout(Container root) {
		root.doLayout();
		for (Component child : root.getComponents())
			if (child instanceof Container)
				layout((Container) child);
	}
}
