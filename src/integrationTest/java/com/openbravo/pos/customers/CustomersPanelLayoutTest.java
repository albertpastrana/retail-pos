package com.openbravo.pos.customers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatClientProperties;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppUser;
import com.openbravo.pos.forms.AppUserView;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.inventory.ReplenishmentEntry;
import com.openbravo.pos.sales.JPanelTicketEdits;
import com.openbravo.pos.ticket.FindTicketsInfo;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class CustomersPanelLayoutTest {
	@Test
	void purchasesOpenExistingTicketViewByKeyboardAndDoubleClick() throws Exception {
		RetailPOSTheme.registerDefaultsSource();
		UIManager.setLookAndFeel(new FlatLightLaf());
		RetailPOSTheme.applyFonts();
		SwingUtilities.invokeAndWait(() -> {
			CustomersPanel panel = new CustomersPanel();
			panel.setSize(1200, 800);
			String[] openedTask = {null};
			AppUser user = new AppUser("seller", "Seller", null, null, AppUser.ROLE_SELLER, null);
			AppUserView view = (AppUserView) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[]{AppUserView.class}, (proxy, method, args) -> switch (method.getName()) {
						case "getUser" -> user;
						case "getTaskAction" -> new AbstractAction() {
							public void actionPerformed(ActionEvent event) {
							}
						};
						case "showTask" -> {
							openedTask[0] = (String) args[0];
							yield null;
						}
						default -> null;
					});
			AppView app = (AppView) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AppView.class},
					(proxy, method, args) -> "getAppUserView".equals(method.getName()) ? view : null);
			try {
				Field appField = CustomersPanel.class.getDeclaredField("app");
				appField.setAccessible(true);
				appField.set(panel, app);
				CustomerInfoExt customer = new CustomerInfoExt("anna");
				customer.setName("Anna");
				customer.setVisible(true);
				FindTicketsInfo ticket = new FindTicketsInfo(10482, 0, new Date(), "cash", 1, 27.40);
				@SuppressWarnings("unchecked")
				Map<String, List<FindTicketsInfo>> histories = field(panel, "ticketHistory", Map.class);
				histories.put(customer.getId(), List.of(ticket));
				Method show = CustomersPanel.class.getDeclaredMethod("showCustomer", CustomerInfoExt.class);
				show.setAccessible(true);
				show.invoke(panel, customer);
				JTabbedPane tabs = field(panel, "tabs", JTabbedPane.class);
				tabs.setSelectedIndex(1);
				layoutTree(panel);
				JTable purchases = field(panel, "ticketsTable", JTable.class);
				assertEquals(1, purchases.getRowCount());
				assertEquals(0, purchases.getSelectedRow());
				assertEquals("openTicket",
						purchases.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER")));
				JPanel purchaseTab = (JPanel) tabs.getComponentAt(1);
				JScrollPane scroll = (JScrollPane) ((java.awt.BorderLayout) purchaseTab.getLayout())
						.getLayoutComponent(java.awt.BorderLayout.CENTER);
				assertEquals(null, scroll.getBorder());
				assertFalse(purchases.getShowVerticalLines());
				assertTrue(purchases.getShowHorizontalLines());
				for (int column = 0; column < purchases.getColumnCount(); column++) {
					JLabel cell = (JLabel) purchases.getCellRenderer(0, column).getTableCellRendererComponent(purchases,
							purchases.getValueAt(0, column), false, false, 0, column);
					JLabel header = (JLabel) purchases.getColumnModel().getColumn(column).getHeaderRenderer()
							.getTableCellRendererComponent(purchases, purchases.getColumnName(column), false, false, -1,
									column);
					assertEquals(column == 3 ? JLabel.RIGHT : JLabel.LEFT, cell.getHorizontalAlignment());
					assertEquals(cell.getHorizontalAlignment(), header.getHorizontalAlignment());
					assertEquals(cell.getBorder().getBorderInsets(cell).left,
							header.getBorder().getBorderInsets(header).left);
					assertEquals(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f), header.getFont());
					assertEquals(RetailPOSColors.inkMuted(), header.getForeground());
					if (column != 2)
						assertEquals(RetailPOSTheme.PLEX_MONO_REGULAR, cell.getFont());
				}
				assertNotNull(findLabel(purchaseTab,
						AppLocal.getIntString("customers.screen.purchaseHistory").toUpperCase(Locale.getDefault())));
				assertNotNull(findLabel(purchaseTab, AppLocal.getIntString("customers.screen.openTicketHint")));
				if (scroll.getViewport().getWidth() >= purchases.getPreferredSize().width)
					assertEquals(scroll.getViewport().getWidth(), purchases.getColumnModel().getTotalColumnWidth());
				Method consume = JPanelTicketEdits.class.getDeclaredMethod("consumeReceiptBarcode");
				consume.setAccessible(true);
				purchases.getActionMap().get("openTicket")
						.actionPerformed(new ActionEvent(purchases, ActionEvent.ACTION_PERFORMED, "openTicket"));
				assertEquals(JPanelTicketEdits.class.getName(), openedTask[0]);
				assertEquals("T10482", consume.invoke(null));
				openedTask[0] = null;
				Rectangle row = purchases.getCellRect(0, 0, true);
				purchases.dispatchEvent(new MouseEvent(purchases, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
						0, row.x + 2, row.y + 2, 2, false));
				assertEquals(JPanelTicketEdits.class.getName(), openedTask[0]);
				assertEquals("T10482", consume.invoke(null));
				CustomerInfoExt withoutPurchases = new CustomerInfoExt("empty");
				withoutPurchases.setName("Joana");
				withoutPurchases.setVisible(true);
				show.invoke(panel, withoutPurchases);
				assertEquals(0, purchases.getRowCount());
				assertEquals(-1, purchases.getSelectedRow());
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	void switchingCustomersReplacesOrderCardsInOpenTab() throws Exception {
		RetailPOSTheme.registerDefaultsSource();
		UIManager.setLookAndFeel(new FlatLightLaf());
		RetailPOSTheme.applyFonts();
		SwingUtilities.invokeAndWait(() -> {
			CustomersPanel panel = new CustomersPanel();
			panel.setSize(1200, 800);
			CustomerInfoExt first = new CustomerInfoExt("first");
			first.setName("Anna");
			first.setVisible(true);
			CustomerInfoExt second = new CustomerInfoExt("second");
			second.setName("Joana");
			second.setVisible(true);
			try {
				Map<?, ?> history = field(panel, "orderHistory", Map.class);
				@SuppressWarnings("unchecked")
				Map<String, List<ReplenishmentEntry>> orders = (Map<String, List<ReplenishmentEntry>>) history;
				orders.put(first.getId(), List.of(order("First order"), order("Another first order")));
				orders.put(second.getId(), List.of(order("Second order")));
				JTable table = field(panel, "table", JTable.class);
				Method setCustomers = table.getModel().getClass().getDeclaredMethod("setCustomers", List.class);
				setCustomers.setAccessible(true);
				setCustomers.invoke(table.getModel(), List.of(first, second));
				JTabbedPane tabs = field(panel, "tabs", JTabbedPane.class);
				tabs.setSelectedIndex(2);
				table.setRowSelectionInterval(0, 0);
				layoutTree(panel);
				JPanel cards = field(panel, "ordersPanel", JPanel.class);
				assertNotNull(findLabel(cards, "First order"));
				assertNotNull(findLabel(cards, "Another first order"));
				table.setRowSelectionInterval(1, 1);
				layoutTree(panel);
				assertNotNull(findLabel(cards, "Second order"));
				assertEquals(null, findLabel(cards, "First order"));
				assertEquals(null, findLabel(cards, "Another first order"));
				assertEquals(Long.class, table.getModel().getColumnClass(2));
				assertEquals(Date.class, table.getModel().getColumnClass(3));
				assertEquals(2L, table.getModel().getValueAt(0, 2));
				table.getRowSorter()
						.setSortKeys(List.of(new javax.swing.RowSorter.SortKey(2, javax.swing.SortOrder.ASCENDING)));
				assertEquals("Joana", table.getValueAt(0, 0));
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static ReplenishmentEntry order(String name) {
		ReplenishmentEntry order = new ReplenishmentEntry();
		order.manualDescription = name;
		order.status = "PENDING";
		return order;
	}

	@Test
	void listAndDetailAreAccessibleInEachLanguage() throws Exception {
		RetailPOSTheme.registerDefaultsSource();
		Locale previous = Locale.getDefault();
		try {
			UIManager.setLookAndFeel(new FlatLightLaf());
			RetailPOSTheme.applyFonts();
			for (String language : new String[]{"ca", "es", "en"}) {
				AppLocal.setLocale(Locale.forLanguageTag(language));
				SwingUtilities.invokeAndWait(() -> {
					CustomersPanel panel = new CustomersPanel();
					panel.setSize(1200, 800);
					layoutTree(panel);
					try {
						JTable table = field(panel, "table", JTable.class);
						assertEquals(5, table.getColumnCount());
						for (int column = 0; column < 5; column++) {
							JLabel cell = (JLabel) table.getColumnModel().getColumn(column).getCellRenderer()
									.getTableCellRendererComponent(table, column == 4 ? 0.0 : "123", false, false, 0,
											column);
							JLabel header = (JLabel) table.getColumnModel().getColumn(column).getHeaderRenderer()
									.getTableCellRendererComponent(table, table.getColumnName(column), false, false, -1,
											column);
							assertEquals(cell.getHorizontalAlignment(), header.getHorizontalAlignment());
							assertEquals(cell.getBorder().getBorderInsets(cell).left,
									header.getBorder().getBorderInsets(header).left);
							assertEquals(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f), header.getFont());
							assertEquals(RetailPOSColors.inkMuted(), header.getForeground());
						}
						JTabbedPane tabs = field(panel, "tabs", JTabbedPane.class);
						assertEquals(4, tabs.getTabCount());
						JPanel purchasesTab = (JPanel) tabs.getComponentAt(1);
						assertNotNull(findLabel(purchasesTab, AppLocal.getIntString("customers.screen.purchaseHistory")
								.toUpperCase(Locale.getDefault())));
						JLabel purchaseHint = findLabel(purchasesTab,
								AppLocal.getIntString("customers.screen.openTicketHint"));
						assertNotNull(purchaseHint);
						assertEquals(RetailPOSColors.inkMuted(), purchaseHint.getForeground());
						CustomerInfoExt customer = new CustomerInfoExt("customer");
						customer.setName("Anna");
						customer.setVisible(true);
						Method show = CustomersPanel.class.getDeclaredMethod("showCustomer", CustomerInfoExt.class);
						show.setAccessible(true);
						show.invoke(panel, customer);
						layoutTree(panel);
						JTextField name = field(panel, "name", JTextField.class);
						assertEquals("Anna", name.getText());
						assertTrue(name.isEditable());
						JTextField search = field(panel, "search", JTextField.class);
						assertEquals(AppLocal.getIntString("customers.screen.search"),
								search.getClientProperty(FlatClientProperties.PLACEHOLDER_TEXT));
						assertEquals("focusWidth: 1", search.getClientProperty(FlatClientProperties.STYLE));
						assertEquals(null, findLabel(panel, AppLocal.getIntString("customers.screen.title")));
						assertNotNull(field(panel, "debtOnly", JToggleButton.class));
						assertNotNull(field(panel, "orderOnly", JToggleButton.class));
						assertNotNull(field(panel, "inactive", JToggleButton.class));
						assertEquals(null, field(panel, "debtOnly", JToggleButton.class)
								.getClientProperty(FlatClientProperties.BUTTON_TYPE));
						assertTrue(field(panel, "debtOnly", JToggleButton.class)
								.getClientProperty(FlatClientProperties.STYLE).toString().contains("arc: 10"));
						JLabel last = field(panel, "lastValue", JLabel.class);
						assertEquals(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(18f), last.getFont());
						JLabel debt = field(panel, "debtValue", JLabel.class);
						JLabel pending = field(panel, "orderValue", JLabel.class);
						assertEquals(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(18f), debt.getFont());
						assertEquals(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(18f), pending.getFont());
						JScrollPane dataScroll = (JScrollPane) tabs.getComponentAt(3);
						for (String key : new String[]{"name", "phone", "allowedDebt", "notes"}) {
							JLabel caption = findLabel(dataScroll, AppLocal.getIntString("customers.screen." + key));
							assertNotNull(caption, key);
							assertEquals(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f), caption.getFont());
							assertEquals(RetailPOSColors.inkMuted(), caption.getForeground());
						}
						JTextField phone = field(panel, "phone", JTextField.class);
						JTextField maxDebt = field(panel, "maxDebt", JTextField.class);
						assertEquals(RetailPOSTheme.PLEX_MONO_REGULAR, phone.getFont());
						assertEquals(phone.getFont(), maxDebt.getFont());
						tabs.setSelectedIndex(3);
						layoutTree(panel);
						Rectangle phoneBounds = SwingUtilities.convertRectangle(phone.getParent(), phone.getBounds(),
								panel);
						Rectangle maxDebtBounds = SwingUtilities.convertRectangle(maxDebt.getParent(),
								maxDebt.getBounds(), panel);
						assertEquals(phoneBounds.y, maxDebtBounds.y);
						assertEquals(phoneBounds.width, maxDebtBounds.width);
						assertTrue(phoneBounds.x + phoneBounds.width < maxDebtBounds.x);
						JButton save = field(panel, "save", JButton.class);
						JButton cancel = field(panel, "cancel", JButton.class);
						JButton archiveButton = field(panel, "archive", JButton.class);
						Rectangle nameBounds = SwingUtilities.convertRectangle(name.getParent(), name.getBounds(),
								panel);
						Rectangle saveBounds = SwingUtilities.convertRectangle(save.getParent(), save.getBounds(),
								panel);
						Rectangle cancelBounds = SwingUtilities.convertRectangle(cancel.getParent(), cancel.getBounds(),
								panel);
						Rectangle archiveBounds = SwingUtilities.convertRectangle(archiveButton.getParent(),
								archiveButton.getBounds(), panel);
						assertEquals(nameBounds.x, saveBounds.x);
						assertEquals(saveBounds.y, cancelBounds.y);
						assertEquals(nameBounds.x + nameBounds.width, archiveBounds.x + archiveBounds.width);
						assertFalse(save.isEnabled());
						phone.setText("600 000 000");
						assertTrue(save.isEnabled());
						assertEquals(RetailPOSColors.brand(), save.getBackground());
						assertEquals(RetailPOSColors.surface100(), cancel.getBackground());
						phone.setText("");
						assertFalse(save.isEnabled());
						tabs.setSelectedIndex(0);
						Rectangle debtValueBounds = SwingUtilities.convertRectangle(debt.getParent(), debt.getBounds(),
								panel);
						Rectangle pendingValueBounds = SwingUtilities.convertRectangle(pending.getParent(),
								pending.getBounds(), panel);
						Rectangle lastValueBounds = SwingUtilities.convertRectangle(last.getParent(), last.getBounds(),
								panel);
						assertEquals(debtValueBounds.y, pendingValueBounds.y);
						assertEquals(debtValueBounds.y, lastValueBounds.y);
						JPanel summary = field(panel, "summary", JPanel.class);
						JLabel recentTitle = findLabel(summary, AppLocal
								.getIntString("customers.screen.recentPurchases").toUpperCase(Locale.getDefault()));
						JLabel noPurchases = findLabel(summary, AppLocal.getIntString("customers.screen.noPurchases"));
						assertNotNull(recentTitle);
						assertNotNull(noPurchases);
						assertEquals(null, ((javax.swing.JScrollPane) tabs.getComponentAt(0)).getBorder());
						JPanel orderTab = (JPanel) tabs.getComponentAt(2);
						assertEquals(null, ((javax.swing.JScrollPane) ((java.awt.BorderLayout) orderTab.getLayout())
								.getLayoutComponent(java.awt.BorderLayout.CENTER)).getBorder());
						Rectangle recentTitleBounds = SwingUtilities.convertRectangle(recentTitle.getParent(),
								recentTitle.getBounds(), summary);
						Rectangle noPurchasesBounds = SwingUtilities.convertRectangle(noPurchases.getParent(),
								noPurchases.getBounds(), summary);
						assertTrue(noPurchasesBounds.y >= recentTitleBounds.y + recentTitleBounds.height);
						assertTrue(noPurchasesBounds.y - recentTitleBounds.y - recentTitleBounds.height < 15);
						JSplitPane split = find(panel, JSplitPane.class);
						assertNotNull(split);
						assertTrue(split.getLeftComponent().getWidth() > 0);
						assertTrue(split.getRightComponent().getWidth() > 0);
						assertTrue(split.getLeftComponent().getWidth() > split.getRightComponent().getWidth(),
								"left=" + split.getLeftComponent().getWidth() + " right="
										+ split.getRightComponent().getWidth());
						assertTrue(split.getRightComponent().getWidth() >= com.formdev.flatlaf.util.UIScale.scale(440));
						JViewport viewport = (JViewport) table.getParent();
						assertTrue(viewport.getWidth() > 0);
						assertEquals(table.getColumnModel().getColumn(1).getMinWidth(),
								table.getColumnModel().getColumn(1).getMaxWidth());
						assertEquals(table.getColumnModel().getColumn(2).getMinWidth(),
								table.getColumnModel().getColumn(2).getMaxWidth());
						assertEquals(table.getColumnModel().getColumn(3).getMinWidth(),
								table.getColumnModel().getColumn(3).getMaxWidth());
						assertEquals(table.getColumnModel().getColumn(4).getMinWidth(),
								table.getColumnModel().getColumn(4).getMaxWidth());
						int preferredColumnsWidth = 0;
						for (int column = 0; column < table.getColumnCount(); column++)
							preferredColumnsWidth += table.getColumnModel().getColumn(column).getPreferredWidth();
						if (viewport.getWidth() >= preferredColumnsWidth) {
							assertEquals(viewport.getWidth(), table.getWidth());
							assertEquals(viewport.getWidth(), table.getColumnModel().getTotalColumnWidth());
						}
						ReplenishmentEntry order = new ReplenishmentEntry();
						order.productId = "product";
						order.name = "Samarreta [SAM-1]";
						order.reference = "SAM-1";
						order.status = "ORDERED";
						Method cards = CustomersPanel.class.getDeclaredMethod("orderCards", java.util.List.class,
								boolean.class);
						cards.setAccessible(true);
						Component orders = (Component) cards.invoke(panel, java.util.List.of(order), false);
						assertNotNull(findLabel(orders, "Samarreta"));
						assertEquals(null, findLabel(orders, "Bestreta i data estimada no disponibles"));
						JButton archive = field(panel, "archive", JButton.class);
						Rectangle bounds = SwingUtilities.convertRectangle(archive.getParent(), archive.getBounds(),
								panel);
						assertTrue(bounds.x + bounds.width <= panel.getWidth());
						assertFalse(archive.getText().isEmpty());
						assertEquals(RetailPOSColors.surface100(), table.getBackground());
						CustomerInfoExt second = new CustomerInfoExt("second");
						second.setName("Joana Cabrera i Fernández");
						second.setVisible(true);
						Method setCustomers = table.getModel().getClass().getDeclaredMethod("setCustomers",
								java.util.List.class);
						setCustomers.setAccessible(true);
						setCustomers.invoke(table.getModel(), java.util.List.of(customer, second));
						Method widths = CustomersPanel.class.getDeclaredMethod("updateColumnWidths");
						widths.setAccessible(true);
						widths.invoke(panel);
						assertTrue(table.getColumnModel().getColumn(0).getPreferredWidth() >= table
								.getFontMetrics(table.getFont()).stringWidth(second.getName()) + 8);
						assertEquals(RetailPOSColors.surface100(),
								table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0).getBackground());
						assertEquals(RetailPOSColors.surface100(),
								table.prepareRenderer(table.getCellRenderer(1, 0), 1, 0).getBackground());
						JLabel footer = field(panel, "listCount", JLabel.class);
						Rectangle footerBounds = SwingUtilities.convertRectangle(footer.getParent(), footer.getBounds(),
								panel);
						assertTrue(footerBounds.y + footerBounds.height < panel.getHeight() - 8);
						assertTrue(split.getRightComponent().getY()
								+ split.getRightComponent().getHeight() < panel.getHeight() - 8);
						for (int width : new int[]{900, 1024, 1600}) {
							panel.setSize(width, 800);
							layoutTree(panel);
							JLabel pendingMetric = findLabel(panel,
									AppLocal.getIntString("customers.screen.pendingMetric"));
							assertNotNull(pendingMetric);
							assertTrue(
									pendingMetric.getWidth() >= pendingMetric.getFontMetrics(pendingMetric.getFont())
											.stringWidth(pendingMetric.getText()),
									"screen=" + width + " metric=" + pendingMetric.getText() + " available="
											+ pendingMetric.getWidth());
							int comfortableWidth = width >= 1200 ? com.formdev.flatlaf.util.UIScale.scale(440) : 440;
							assertTrue(split.getRightComponent().getWidth() >= comfortableWidth,
									"screen=" + width + " right=" + split.getRightComponent().getWidth());
							if (width >= 1200)
								assertTrue(split.getLeftComponent().getWidth() > split.getRightComponent().getWidth(),
										"screen=" + width + " left=" + split.getLeftComponent().getWidth() + " right="
												+ split.getRightComponent().getWidth());
						}
						assertEquals("new", panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
								.get(KeyStroke.getKeyStroke("control N")));
						assertEquals("edit",
								table.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER")));
						field(panel, "newCustomer", JButton.class).doClick();
						assertEquals(3, tabs.getSelectedIndex());
						assertTrue(name.isEnabled());
						assertFalse(save.isEnabled());
						assertTrue(cancel.isEnabled());
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				});
			}
		} finally {
			AppLocal.setLocale(previous);
		}
	}

	private static <T> T field(CustomersPanel panel, String name, Class<T> type) throws ReflectiveOperationException {
		Field field = CustomersPanel.class.getDeclaredField(name);
		field.setAccessible(true);
		return type.cast(field.get(panel));
	}

	private static <T> T find(Component component, Class<T> type) {
		if (type.isInstance(component))
			return type.cast(component);
		if (component instanceof Container container)
			for (Component child : container.getComponents()) {
				T result = find(child, type);
				if (result != null)
					return result;
			}
		return null;
	}
	private static JLabel findLabel(Component component, String text) {
		if (component instanceof JLabel label && text.equals(label.getText()))
			return label;
		if (component instanceof Container container)
			for (Component child : container.getComponents()) {
				JLabel result = findLabel(child, text);
				if (result != null)
					return result;
			}
		return null;
	}

	private static void layoutTree(Component component) {
		if (component instanceof Container container) {
			container.doLayout();
			for (Component child : container.getComponents())
				layoutTree(child);
		}
	}
}
