package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.inventory.DataLogicReplenishment;
import com.openbravo.pos.inventory.ReplenishmentEntry;
import com.openbravo.pos.inventory.ReplenishmentPanel;
import com.openbravo.pos.inventory.ReplenishmentSupplier;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class ReplenishmentPanelLayoutTest {
	@Test
	void fourContextCardsAndSupplierNavigationFitBothThemesAndThreeLanguages() throws Exception {
		String url = "jdbc:derby:memory:replenishmentLayout" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		Locale previous = Locale.getDefault();
		javax.swing.LookAndFeel previousTheme = UIManager.getLookAndFeel();
		try {
			DataLogicReplenishment data = new DataLogicReplenishment();
			data.init(session);
			try (Statement sql = session.getConnection().createStatement()) {
				sql.executeUpdate("INSERT INTO CUSTOMERS (ID, SEARCHKEY, NAME, PHONE) "
						+ "VALUES ('customer-anna', 'customer-anna', 'Anna Puig', '34600111222')");
				sql.executeUpdate("INSERT INTO CUSTOMERS (ID, SEARCHKEY, NAME) "
						+ "VALUES ('customer-joana', 'customer-joana', 'Joana Vila')");
				sql.executeUpdate("INSERT INTO PRODUCTS "
						+ "(ID, REFERENCE, CODE, NAME, PRICEBUY, PRICESELL, CATEGORY, TAXCAT, BRAND, FAMILY) "
						+ "VALUES ('group-a', 'MNA-1042', '990001', 'Samarreta interior de cotó', 1, 2, 'gift-vouchers', '000', 'Massana', 'Cotó'), "
						+ "('group-b', 'YSM-2001', '990002', 'Mitjons de llana per a dona', 1, 2, 'gift-vouchers', '000', 'Ysabel Mora', 'Mitjons')");
				sql.executeUpdate("UPDATE PEOPLE SET NAME='Marta Soler' WHERE ID='2'");
			}
			ReplenishmentSupplier supplier = new ReplenishmentSupplier();
			supplier.name = "Tèxtils Montserrat";
			supplier.contactFirstName = "Marta";
			supplier.contactLastName = "Soler";
			supplier.whatsapp = "+34 600 00 00 00";
			data.saveSupplier(supplier);
			ReplenishmentSupplier withoutPhone = new ReplenishmentSupplier();
			withoutPhone.name = "Calçats del Vallès";
			withoutPhone.contactFirstName = "Joana";
			withoutPhone.contactLastName = "Vidal";
			data.saveSupplier(withoutPhone);
			java.util.List<String> grouped = new java.util.ArrayList<>();
			String[] descriptions = {"Brusa de lli", "Jersei beix", "Bata de ratlles", "Cinturó de pell",
					"Leggings negres", "Pijama florit", "Mitjons esportius"};
			String[] missingPhoneRequest = {null};
			for (int i = 0; i < 9; i++) {
				ReplenishmentEntry request = new ReplenishmentEntry();
				request.createdBy = "2";
				if (i < 2) {
					request.productId = i == 0 ? "group-a" : "group-b";
					request.reference = i == 0 ? "MNA-1042" : "YSM-2001";
					request.name = i == 0 ? "Samarreta interior de cotó" : "Mitjons de llana per a dona";
					if (i == 1) {
						request.customerId = "customer-anna";
						request.customerName = "Anna Puig";
					}
					ReplenishmentEntry saved = data.add(request);
					if (i == 1)
						assertEquals("34600111222", data.list("Anna Puig", "ALL", "PENDING").get(0).customerPhone);
					grouped.add(saved.id);
				} else {
					request.manualDescription = descriptions[i - 2];
					if (i == 2) {
						request.customerId = "customer-joana";
						request.customerName = "Joana Vila";
					}
					ReplenishmentEntry saved = data.add(request);
					if (i == 2)
						grouped.add(saved.id);
					if (i == 3)
						missingPhoneRequest[0] = saved.id;
				}
			}
			data.assignSupplier(grouped, supplier.id, "2");
			assertEquals("34600111222", data.list("600 111 222", "ALL", "PENDING").get(0).customerPhone);
			data.assignSupplier(java.util.List.of(missingPhoneRequest[0]), withoutPhone.id, "2");
			AppUser user = new AppUser("2", "Employee", null, null, "2", null);
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
			RetailPOSTheme.registerDefaultsSource();
			for (javax.swing.LookAndFeel theme : new javax.swing.LookAndFeel[]{new FlatLightLaf(), new FlatDarkLaf()}) {
				UIManager.setLookAndFeel(theme);
				RetailPOSTheme.applyFonts();
				for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
					AppLocal.setLocale(locale);
					SwingUtilities.invokeAndWait(() -> {
						try {
							ReplenishmentPanel panel = new ReplenishmentPanel();
							panel.init(app);
							panel.activate();
							assertEquals(null, label(panel, AppLocal.getIntString("Replenishment.ScreenTitle")),
									"Application toolbar owns the screen title");
							JButton newRequest = button(panel, AppLocal.getIntString("Replenishment.NewRequest"));
							assertTrue(newRequest != null && newRequest.getIcon() != null);
							assertEquals("replenishment.searchRow", newRequest.getParent().getName());
							Method pendingLabel = ReplenishmentPanel.class.getDeclaredMethod("pendingCountLabel",
									long.class);
							pendingLabel.setAccessible(true);
							assertEquals(String.format(AppLocal.getIntString("Replenishment.PendingCountOne"), 1),
									pendingLabel.invoke(null, 1L));
							assertEquals(String.format(AppLocal.getIntString("Replenishment.PendingCount"), 3),
									pendingLabel.invoke(null, 3L));
							JTable table = first(panel, JTable.class);
							int customerRow = 0;
							while (!"Anna Puig".equals(table.getModel().getValueAt(customerRow, 2)))
								customerRow++;
							int customerView = table.convertColumnIndexToView(2);
							Component customerCell = table.getCellRenderer(customerRow, customerView)
									.getTableCellRendererComponent(table, table.getValueAt(customerRow, customerView),
											false, false, customerRow, customerView);
							JLabel phoneCaption = label((Container) customerCell, "34600111222");
							assertNotNull(phoneCaption);
							assertEquals(RetailPOSColors.inkMuted(), phoneCaption.getForeground());
							assertEquals(13f, phoneCaption.getFont().getSize2D());
							int noPhoneRow = 0;
							while (!"Joana Vila".equals(table.getModel().getValueAt(noPhoneRow, 2)))
								noPhoneRow++;
							Component noPhoneCell = table.getCellRenderer(noPhoneRow, customerView)
									.getTableCellRendererComponent(table, table.getValueAt(noPhoneRow, customerView),
											false, false, noPhoneRow, customerView);
							assertFalse(components((Container) noPhoneCell, JLabel.class).get(1).isVisible(),
									"Customer without a phone has no empty caption line");
							JPanel cards = cards(panel);
							table.setRowSelectionInterval(noPhoneRow, noPhoneRow);
							java.lang.reflect.Field detailPhone = ReplenishmentPanel.class
									.getDeclaredField("detailCustomerPhone");
							detailPhone.setAccessible(true);
							assertFalse(((JLabel) detailPhone.get(panel)).isVisible(),
									"The detail omits an empty phone line");
							table.clearSelection();
							assertTrue(cards.getComponent(0).isVisible(), "No selection shows the new request");
							JButton cancelNew = button((Container) cards.getComponent(0),
									AppLocal.getIntString("Button.Cancel"));
							assertFalse(cancelNew.isEnabled(), "Blank new request has nothing to cancel");
							first((Container) cards.getComponent(0), JTextField.class).setText("12345");
							assertTrue(cancelNew.isEnabled(), "Scanner input enables Cancel");
							cancelNew.doClick();
							assertFalse(cancelNew.isEnabled());
							assertEquals(9, table.getRowCount());
							assertEquals(5, table.getColumnCount(), "A single status hides the Status column");
							assertEquals(5, table.getColumnModel().getColumn(2).getModelIndex(),
									"Supplier follows the product request");
							assertEquals(2, table.getColumnModel().getColumn(3).getModelIndex());
							assertEquals(4, table.getColumnModel().getColumn(4).getModelIndex());
							JLabel supplierCell = (JLabel) table.getCellRenderer(0, 2)
									.getTableCellRendererComponent(table, table.getValueAt(0, 2), false, false, 0, 2);
							assertTrue(supplierCell.getFont().getFamily().contains("Manrope"));
							assertEquals(15f, supplierCell.getFont().getSize2D());
							assertTrue(toggle(panel, AppLocal.getIntString("Replenishment.Pending") + " (9)")
									.isSelected());
							assertTrue(
									label(panel, String.format(AppLocal.getIntString("Replenishment.SelectedOf"), 0, 9))
											.isVisible());
							assertTrue(label(panel,
									String.format(AppLocal.getIntString("Replenishment.ExportVisibleHint"), 9))
									.isVisible());
							assertFalse(
									button(panel, AppLocal.getIntString("Replenishment.ClearSelection")).isVisible());
							assertTrue(button(panel, AppLocal.getIntString("Replenishment.Export")).isVisible());
							JCheckBox emptyHeader = (JCheckBox) table.getTableHeader().getDefaultRenderer()
									.getTableCellRendererComponent(table, "", false, false, -1, 0);
							assertFalse(emptyHeader.isSelected());
							assertFalse(RetailPOSColors.brand()
									.equals(toggle(panel, AppLocal.getIntString("Replenishment.Pending") + " (9)")
											.getBackground()));
							for (int width : new int[]{980, 1200, 1440, 2000}) {
								panel.setSize(width, 750);
								layout(panel);
								layout(panel);
								layout(panel);
								table.doLayout();
								JTextField searchField = first(panel, JTextField.class);
								int searchBottom = SwingUtilities.convertPoint(searchField, 0, searchField.getHeight(),
										panel).y;
								int tableTop = SwingUtilities.convertPoint(tableScroll(panel), 0, 0, panel).y;
								assertTrue(
										searchField.getHeight() >= com.formdev.flatlaf.util.UIScale.scale(44)
												&& searchBottom <= tableTop,
										"Search toolbar must not be clipped at " + width + ": bottom=" + searchBottom
												+ ", table=" + tableTop + ", height=" + searchField.getHeight());
								assertTrue(tableScroll(panel).getColumnHeader().getHeight() >= 32,
										"Column headings must remain visible at " + width);
								assertEquals(width >= 980, sidebar(panel).isVisible());
								assertEquals(width >= 980, !first(panel, JComboBox.class).getParent().isVisible(),
										"Supplier combo is shown only without the sidebar");
								JPanel segments = namedPanel(panel, "replenishment.statusSegments");
								int compactWidth = 8;
								for (Component segment : segments.getComponents())
									compactWidth += segment.getPreferredSize().width;
								for (Component segment : segments.getComponents())
									assertEquals(segments.getComponent(0).getY(), segment.getY(),
											"Status tabs must stay on one row at " + width);
								if (width == 2000)
									assertTrue(Math.abs(
											SwingUtilities.convertPoint(segments, 0, segments.getHeight() / 2, panel).y
													- SwingUtilities.convertPoint(searchField, 0,
															searchField.getHeight() / 2, panel).y) <= 2,
											"Status and search share a vertically centered toolbar row");
								for (String key : new String[]{"Replenishment.Pending", "Replenishment.Ordered",
										"Replenishment.Received", "Replenishment.Delivered", "Replenishment.All"}) {
									JToggleButton segment = toggle(panel, AppLocal.getIntString(key)
											+ ("Replenishment.Pending".equals(key) || "Replenishment.All".equals(key)
													? " (9)"
													: " (0)"));
									int needed = segment.getFontMetrics(segment.getFont()).stringWidth(
											segment.getText()) + segment.getInsets().left + segment.getInsets().right;
									assertTrue(segment.getWidth() >= needed, key + " label must fit at " + width + ": "
											+ segment.getWidth() + " < " + needed);
									assertTrue(segment.isFocusable());
									assertTrue(
											segment.getPreferredSize().height >= com.formdev.flatlaf.util.UIScale
													.scale(40),
											key + " segment height: " + segment.getPreferredSize().height);
								}
								JButton exportButton = button(panel, AppLocal.getIntString("Replenishment.Export"));
								int exportRight = SwingUtilities.convertPoint(exportButton, exportButton.getWidth(), 0,
										panel).x;
								int tableRight = SwingUtilities.convertPoint(tableScroll(panel),
										tableScroll(panel).getWidth(), 0, panel).x;
								assertTrue(exportRight <= tableRight, "Export action must fit at " + width);
								assertTrue(SwingUtilities.convertPoint(exportButton, 0, 0, panel).y < 750,
										"Export action must stay visible at " + width);
								if (width == 980 && com.formdev.flatlaf.util.UIScale.getUserScaleFactor() <= 1f) {
									assertTrue(tableScroll(panel).getViewport().getHeight() >= 9 * 48,
											"Nine ordinary touch rows fit at 980px and 1x");
								}
								BufferedImage image = new BufferedImage(width, 750, BufferedImage.TYPE_INT_RGB);
								panel.paint(image.getGraphics());
								File directory = Path.of("build", "reports", "replenishment-screenshots").toFile();
								assertTrue(directory.isDirectory() || directory.mkdirs());
								ImageIO.write(image, "png",
										new File(directory, (theme instanceof FlatDarkLaf ? "dark" : "light") + "-"
												+ locale.getLanguage() + "-" + width + ".png"));
							}
							table.setRowSelectionInterval(0, 2);
							assertTrue(
									label(panel, String.format(AppLocal.getIntString("Replenishment.SelectedOf"), 3, 9))
											.isVisible());
							assertFalse(label(panel,
									String.format(AppLocal.getIntString("Replenishment.ExportVisibleHint"), 9))
									.isVisible());
							assertTrue(
									button(panel, AppLocal.getIntString("Replenishment.ClearSelection")).isVisible());
							assertTrue(button(panel,
									String.format(AppLocal.getIntString("Replenishment.ExportSelectionCount"), 3))
									.isVisible());
							JCheckBox headerCheck = (JCheckBox) table.getTableHeader().getDefaultRenderer()
									.getTableCellRendererComponent(table, "", false, false, -1, 0);
							assertEquals("indeterminate", headerCheck.getClientProperty("JButton.selectedState"));
							button(panel, AppLocal.getIntString("Replenishment.SelectAll")).doClick();
							JCheckBox fullHeader = (JCheckBox) table.getTableHeader().getDefaultRenderer()
									.getTableCellRendererComponent(table, "", false, false, -1, 0);
							assertTrue(fullHeader.isSelected());
							button(panel, AppLocal.getIntString("Replenishment.ClearSelection")).doClick();
							toggle(panel, AppLocal.getIntString("Replenishment.All") + " (9)").doClick();
							assertEquals(6, table.getColumnCount(), "All statuses restores the Status column");
							assertEquals(5, table.getColumnModel().getColumn(2).getModelIndex());
							assertEquals(2, table.getColumnModel().getColumn(3).getModelIndex());
							assertEquals(3, table.getColumnModel().getColumn(4).getModelIndex());
							assertEquals(4, table.getColumnModel().getColumn(5).getModelIndex());
							checkContext(panel, cards, table, 0, "all-status", theme, locale);
							toggle(panel, AppLocal.getIntString("Replenishment.Pending") + " (9)").doClick();
							JComboBox<?> filter = first(panel, JComboBox.class);
							selectSupplier(filter, "NONE");
							assertEquals(4, table.getColumnCount(), "Unassigned hides both Supplier and Status");
							assertTrue(cards.getComponent(0).isVisible());
							checkContext(panel, cards, table, 0, "unassigned-none", theme, locale);
							table.setRowSelectionInterval(0, 2);
							assertEquals(3, table.getSelectedRowCount());
							assertTrue(cards.getComponent(2).isVisible());
							assertEquals(null, button((Container) cards.getComponent(2),
									AppLocal.getIntString("Replenishment.NoSupplier")));
							JCheckBox selectedCheck = (JCheckBox) table.getCellRenderer(0, 0)
									.getTableCellRendererComponent(table, table.getValueAt(0, 0), true, false, 0, 0);
							assertTrue(selectedCheck.isSelected() && selectedCheck.getIcon() != null);
							checkContext(panel, cards, table, 2, "unassigned-three", theme, locale);
							button(panel, AppLocal.getIntString("Replenishment.ClearSelection")).doClick();
							selectSupplier(filter, supplier.id);
							JButton more = namedButton(panel, "replenishment.supplierMore." + supplier.id);
							assertTrue(more.isVisible(), "The active supplier exposes its actions");
							assertTrue(more.getPreferredSize().height >= com.formdev.flatlaf.util.UIScale.scale(36));
							JButton inactiveMore = namedButton(panel, "replenishment.supplierMore." + withoutPhone.id);
							assertFalse(inactiveMore.isVisible());
							JButton inactiveSupplier = button(sidebar(panel), withoutPhone.name);
							inactiveSupplier.dispatchEvent(new java.awt.event.MouseEvent(inactiveSupplier,
									java.awt.event.MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 5, 5, 0,
									false));
							assertTrue(inactiveMore.isVisible(), "Hover reveals the supplier menu action");
							inactiveSupplier.dispatchEvent(new java.awt.event.MouseEvent(inactiveSupplier,
									java.awt.event.MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -10, -10, 0,
									false));
							assertFalse(inactiveMore.isVisible());
							Method supplierActions = ReplenishmentPanel.class
									.getDeclaredMethod("createSupplierActionsMenu", ReplenishmentSupplier.class);
							supplierActions.setAccessible(true);
							javax.swing.JPopupMenu supplierMenu = (javax.swing.JPopupMenu) supplierActions.invoke(panel,
									supplier);
							assertEquals(AppLocal.getIntString("Replenishment.EditAction"),
									((javax.swing.JMenuItem) supplierMenu.getComponent(0)).getText());
							assertEquals(AppLocal.getIntString("Replenishment.Delete"),
									((javax.swing.JMenuItem) supplierMenu.getComponent(1)).getText());
							assertEquals(6, table.getRowCount(), "Three brand/family groups and three requests");
							assertEquals(4, table.getColumnCount());
							assertFalse(cards.getParent().isVisible(),
									"Admin list fills the space without a selection");
							checkContext(panel, cards, table, -1, "admin-list", theme, locale);
							captureSendDialogs(panel, supplier, data, theme, locale);
							captureSupplierDialog(panel, supplier, theme, locale);
							assertEquals("Massana · Cotó", table.getValueAt(0, 1));
							JPanel productCell = (JPanel) table.getCellRenderer(1, 1)
									.getTableCellRendererComponent(table, table.getValueAt(1, 1), false, false, 1, 1);
							assertEquals(RetailPOSColors.inkMuted(), first(productCell, JLabel.class).getForeground());
							assertEquals("Ysabel Mora · Mitjons", table.getValueAt(2, 1));
							assertEquals(AppLocal.getIntString("Replenishment.UnbrandedFamily"),
									table.getValueAt(4, 1));
							table.setRowSelectionInterval(0, 0);
							assertEquals(0, table.getSelectedRowCount(), "Group heading must not be selectable");
							table.setRowSelectionInterval(1, 1);
							assertTrue(cards.getComponent(1).isVisible(), "One request shows Detail");
							checkContext(panel, cards, table, 1, "detail", theme, locale);
							table.setRowSelectionInterval(3, 3);
							checkContext(panel, cards, table, 1, "detail-customer", theme, locale);
							table.addRowSelectionInterval(1, 1);
							assertTrue(cards.getComponent(2).isVisible(), "Multiple requests show Selection");
							checkContext(panel, cards, table, 2, "selection", theme, locale);
							button(panel, AppLocal.getIntString("Replenishment.ClearSelection")).doClick();
							assertFalse(cards.getParent().isVisible());
							newRequest.doClick();
							assertTrue(cards.getComponent(0).isVisible(), "New request always opens creation form");
							checkContext(panel, cards, table, 0, "new", theme, locale);
							table.setRowSelectionInterval(1, 1);
							JButton next = button(cards, AppLocal.getIntString("Replenishment.MarkOrdered"));
							assertTrue(next.isEnabled());
							String note = "ca".equals(locale.getLanguage())
									? "Nota per demanar"
									: "es".equals(locale.getLanguage()) ? "Nota del pedido" : "Order note";
							editableArea((Container) cards.getComponent(1)).setText(note);
							next.doClick();
							assertFalse(cards.getParent().isVisible(), "Ordered request leaves Pending");
							assertEquals(2, data.list("", "ALL", "PENDING", supplier.id).size());
							assertEquals(4, table.getRowCount(),
									"Only two pending requests remain, with group headings");
							assertTrue(toggle(panel, AppLocal.getIntString("Replenishment.Pending") + " (2)")
									.isSelected());
							assertEquals("8", sidebarCount(panel, AppLocal.getIntString("Replenishment.AllSuppliers")));
							assertEquals("2", sidebarCount(panel, supplier.name));
							assertEquals("ORDERED", data.list("", "ALL", "ORDERED", supplier.id).get(0).status);
							assertEquals(note, data.list("", "ALL", "ORDERED", supplier.id).get(0).note);
							toggle(panel, AppLocal.getIntString("Replenishment.Ordered") + " (1)").doClick();
							table.setRowSelectionInterval(1, 1);
							button(cards, AppLocal.getIntString("Replenishment.MarkReceived")).doClick();
							assertEquals("RECEIVED", data.list("", "ALL", "RECEIVED", supplier.id).get(0).status);
							assertEquals(0, table.getRowCount(), "Received request leaves Ordered");
							assertTrue(toggle(panel, AppLocal.getIntString("Replenishment.Ordered") + " (0)")
									.isSelected());
							assertEquals("0", sidebarCount(panel, AppLocal.getIntString("Replenishment.AllSuppliers")));
							assertEquals("0", sidebarCount(panel, supplier.name));
							toggle(panel, AppLocal.getIntString("Replenishment.Received") + " (1)").doClick();
							table.setRowSelectionInterval(1, 1);
							assertTrue(
									button((Container) cards.getComponent(1),
											AppLocal.getIntString("Replenishment.Customer")).isVisible(),
									"A received request without a customer offers Choose customer");
							assertEquals(AppLocal.getIntString("Replenishment.NeedCustomerForDelivery"),
									areaContaining((Container) cards.getComponent(1),
											AppLocal.getIntString("Replenishment.NeedCustomerForDelivery")).getText());
							checkContext(panel, cards, table, 1, "detail-received-blocked", theme, locale);
							data.updateDetails(grouped.get(0), "", "customer", "Customer", "2");
							panel.activate();
							toggle(panel, AppLocal.getIntString("Replenishment.Received") + " (1)").doClick();
							selectSupplier(filter, supplier.id);
							table.setRowSelectionInterval(1, 1);
							button(cards, AppLocal.getIntString("Replenishment.MarkDelivered")).doClick();
							assertEquals("DELIVERED", data.list("", "ALL", "DELIVERED", supplier.id).get(0).status);
							data.updateStatus(grouped.get(0), "PENDING", "2");
							data.updateDetails(grouped.get(0), "", null, null, "2");
							panel.activate();
							selectSupplier(filter, withoutPhone.id);
							assertFalse(cards.getParent().isVisible());
							assertTrue(
									button(panel,
											String.format(AppLocal.getIntString("Replenishment.SendOrderCount"),
													data.list("", "ALL", "PENDING", withoutPhone.id).size()))
											.isEnabled());
							checkContext(panel, cards, table, -1, "admin-no-phone", theme, locale);
							Method sendWithoutPhone = ReplenishmentPanel.class.getDeclaredMethod("createSendDialog",
									String.class);
							sendWithoutPhone.setAccessible(true);
							JDialog textDialog = (JDialog) sendWithoutPhone.invoke(panel, "TEXT");
							try {
								assertTrue(toggle(textDialog.getContentPane(),
										AppLocal.getIntString("Replenishment.TextFile")).isSelected());
								assertTrue(
										label(textDialog.getContentPane(),
												AppLocal.getIntString("Replenishment.ToPhone") + " "
														+ AppLocal.getIntString("Replenishment.MissingWhatsApp"))
												.isVisible());
								captureOverlay(panel, textDialog.getContentPane(), 980, "send-no-phone", theme, locale);
							} finally {
								textDialog.dispose();
							}
							panel.activate();
							selectSupplier(filter, supplier.id);
							table.setRowSelectionInterval(1, 1);
							Method correction = ReplenishmentPanel.class.getDeclaredMethod("applyDetailStep",
									String.class);
							correction.setAccessible(true);
							correction.invoke(panel, "RECEIVED");
							assertTrue(toggle(panel, AppLocal.getIntString("Replenishment.Pending") + " (2)")
									.isSelected());
							assertEquals(4, table.getRowCount(), "Correcting a Pending request to Received hides it");
							assertEquals("8", sidebarCount(panel, AppLocal.getIntString("Replenishment.AllSuppliers")));
							assertEquals("2", sidebarCount(panel, supplier.name));
							data.updateStatus(grouped.get(0), "PENDING", "2");
						} catch (Exception ex) {
							throw new AssertionError(ex);
						}
					});
				}
			}
			data.deleteSupplier(supplier.id, "2");
			data.deleteSupplier(withoutPhone.id, "2");
			AppLocal.setLocale(new Locale("ca"));
			SwingUtilities.invokeAndWait(() -> {
				try {
					ReplenishmentPanel empty = new ReplenishmentPanel();
					empty.init(app);
					empty.activate();
					empty.setSize(1440, 750);
					layout(empty);
					assertTrue(label(empty, AppLocal.getIntString("Replenishment.NoSuppliersYet")).isVisible());
					assertTrue(button(empty, AppLocal.getIntString("Replenishment.NewSupplier")).isVisible());
				} catch (Exception ex) {
					throw new AssertionError(ex);
				}
			});
		} finally {
			AppLocal.setLocale(previous);
			UIManager.setLookAndFeel(previousTheme);
			session.close();
		}
	}
	private static void checkContext(ReplenishmentPanel panel, JPanel cards, JTable table, int index, String mode,
			javax.swing.LookAndFeel theme, Locale locale) throws Exception {
		for (int width : new int[]{980, 1200, 1440}) {
			panel.setSize(width, 750);
			layout(panel);
			layout(panel);
			table.doLayout();
			assertTrue(tableScroll(panel).getColumnHeader().getHeight() >= 32,
					"Column headings must remain visible in " + mode + " at " + width);
			if (index < 0)
				assertFalse(cards.getParent().isVisible(), mode + " at " + width);
			else
				assertTrue(cards.getComponent(index).isVisible(), mode + " at " + width);
			if ("selection".equals(mode) && width == 980) {
				JLabel selectedCount = label(panel,
						String.format(AppLocal.getIntString("Replenishment.SelectedOf"), 2, 3));
				int needed = selectedCount.getFontMetrics(selectedCount.getFont()).stringWidth(selectedCount.getText());
				assertTrue(selectedCount.getWidth() >= needed,
						"Selection count clipping: width=" + selectedCount.getWidth() + " required=" + needed
								+ " preferred=" + selectedCount.getPreferredSize().width + " parent="
								+ selectedCount.getParent().getWidth());
			}
			if ("unassigned-three".equals(mode)) {
				for (String key : new String[]{"Replenishment.SelectAll", "Replenishment.ClearSelection"}) {
					JButton ghost = button(panel, AppLocal.getIntString(key));
					assertEquals(15f, ghost.getFont().getSize2D());
					assertTrue(ghost.getPreferredSize().height >= com.formdev.flatlaf.util.UIScale.scale(44));
				}
				JButton export = button(panel,
						String.format(AppLocal.getIntString("Replenishment.ExportSelectionCount"), 3));
				assertTrue(export.getWidth() >= export.getFontMetrics(export.getFont()).stringWidth(export.getText())
						+ export.getInsets().left + export.getInsets().right, "Export label fits at " + width);
				assertTrue(
						SwingUtilities.convertPoint(export, export.getWidth(), 0, panel).x <= SwingUtilities
								.convertPoint(tableScroll(panel), tableScroll(panel).getWidth(), 0, panel).x,
						"Selection export beyond table: "
								+ SwingUtilities.convertPoint(export, export.getWidth(), 0, panel).x + " > "
								+ SwingUtilities.convertPoint(tableScroll(panel), tableScroll(panel).getWidth(), 0,
										panel).x);
			}
			if (width >= 1200 && "unassigned-none".equals(mode)) {
				JButton unassigned = button(sidebar(panel), AppLocal.getIntString("Replenishment.NoSupplier"));
				assertEquals(17f, unassigned.getFont().getSize2D());
				assertTrue(unassigned.isFocusable());
				assertEquals(Boolean.TRUE,
						((JPanel) unassigned.getParent()).getClientProperty("replenishment.highlighted"));
				JButton manage = button(panel, AppLocal.getIntString("Replenishment.NewSupplier"));
				assertEquals(15f, manage.getFont().getSize2D());
				assertTrue(manage.getHeight() >= com.formdev.flatlaf.util.UIScale.scale(44),
						"Manage supplier touch height: " + manage.getHeight());
				int required = manage.getFontMetrics(manage.getFont()).stringWidth(manage.getText())
						+ manage.getIcon().getIconWidth() + manage.getIconTextGap() + manage.getInsets().left
						+ manage.getInsets().right;
				assertTrue(manage.getWidth() >= required,
						"Manage suppliers label fits at " + width + ": actual=" + manage.getWidth() + " required="
								+ required + " x=" + SwingUtilities.convertPoint(manage, 0, 0, panel).x + " sidebar="
								+ sidebar(panel).getWidth());
			}
			BufferedImage image = new BufferedImage(width, 750, BufferedImage.TYPE_INT_RGB);
			panel.paint(image.getGraphics());
			ImageIO.write(image, "png",
					new File(Path.of("build", "reports", "replenishment-screenshots").toFile(),
							(theme instanceof FlatDarkLaf ? "dark" : "light") + "-" + locale.getLanguage() + "-" + mode
									+ "-" + width + ".png"));
		}
	}
	private static void captureSendDialogs(ReplenishmentPanel panel, ReplenishmentSupplier supplier,
			DataLogicReplenishment data, javax.swing.LookAndFeel theme, Locale locale) throws Exception {
		Method sendMethod = ReplenishmentPanel.class.getDeclaredMethod("createSendDialog", String.class);
		sendMethod.setAccessible(true);
		Method confirmMethod = ReplenishmentPanel.class.getDeclaredMethod("createWhatsAppConfirmationDialog",
				ReplenishmentSupplier.class, java.util.List.class);
		confirmMethod.setAccessible(true);
		for (int width : new int[]{980, 1440}) {
			panel.setSize(width, 750);
			layout(panel);
			JDialog send = (JDialog) sendMethod.invoke(panel, "WHATSAPP");
			try {
				JButton action = button(send.getContentPane(), AppLocal.getIntString("Replenishment.OpenWhatsApp"));
				assertTrue(
						action.getWidth() >= action.getFontMetrics(action.getFont()).stringWidth(action.getText())
								+ action.getInsets().left + action.getInsets().right,
						"WhatsApp confirm label fits at " + width + ": actual=" + action.getWidth() + " preferred="
								+ action.getPreferredSize().width + " text="
								+ action.getFontMetrics(action.getFont()).stringWidth(action.getText()));
				captureOverlay(panel, send.getContentPane(), width, "send", theme, locale);
			} finally {
				send.dispose();
			}
			JDialog confirm = (JDialog) confirmMethod.invoke(panel, supplier,
					data.listForExport("", "ALL", "PENDING", supplier.id));
			try {
				JButton primary = confirm.getRootPane().getDefaultButton();
				JButton secondary = button(confirm.getContentPane(), AppLocal.getIntString("Replenishment.NotYet"));
				assertEquals(AppLocal.getIntString("Replenishment.YesMarkOrdered"), primary.getText());
				assertEquals(RetailPOSColors.brand(), primary.getBackground());
				assertTrue(primary.getPreferredSize().height >= 48);
				assertTrue(secondary.getPreferredSize().height >= 48);
				assertTrue(SwingUtilities.convertPoint(secondary, 0, 0, confirm).x < SwingUtilities
						.convertPoint(primary, 0, 0, confirm).x);
				captureOverlay(panel, confirm.getContentPane(), width, "confirm", theme, locale);
			} finally {
				confirm.dispose();
			}
		}
	}
	private static void captureSupplierDialog(ReplenishmentPanel panel, ReplenishmentSupplier supplier,
			javax.swing.LookAndFeel theme, Locale locale) throws Exception {
		Method method = ReplenishmentPanel.class.getDeclaredMethod("createSupplierFormDialog", String.class,
				java.util.function.Consumer.class);
		method.setAccessible(true);
		for (int width : new int[]{980, 1440}) {
			panel.setSize(width, 750);
			layout(panel);
			for (String id : new String[]{supplier.id, null}) {
				JDialog dialog = (JDialog) method.invoke(panel, id, null);
				try {
					layout(dialog.getContentPane());
					assertEquals(
							AppLocal.getIntString(
									id == null ? "Replenishment.NewSupplier" : "Replenishment.EditSupplier"),
							dialog.getTitle());
					assertEquals(null, first(dialog.getContentPane(), JList.class), "Form has no supplier list");
					assertEquals(null,
							button(dialog.getContentPane(), AppLocal.getIntString("Replenishment.DeleteSupplier")));
					for (String label : new String[]{AppLocal.getIntString("Button.Cancel"),
							AppLocal.getIntString("Replenishment.Save")}) {
						JButton action = button(dialog.getContentPane(), label);
						assertTrue(action.getParent().getLayout() instanceof FlowLayout);
						assertTrue(action.getHeight() <= com.formdev.flatlaf.util.UIScale.scale(44) + 4);
					}
					assertEquals(5, components(dialog.getContentPane(), JTextField.class).size());
					for (JTextField field : components(dialog.getContentPane(), JTextField.class))
						assertEquals(RetailPOSColors.surface100(), field.getBackground());
					captureOverlay(panel, dialog.getContentPane(), width, id == null ? "supplier-new" : "supplier-edit",
							theme, locale);
				} finally {
					dialog.dispose();
				}
			}
		}
	}
	private static void captureOverlay(ReplenishmentPanel panel, Container content, int width, String name,
			javax.swing.LookAndFeel theme, Locale locale) throws Exception {
		java.awt.Dimension size = content.getPreferredSize();
		content.setSize(size);
		layout(content);
		BufferedImage image = new BufferedImage(width, 750, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D graphics = image.createGraphics();
		panel.paint(graphics);
		if ("send".equals(name) || name.startsWith("supplier-")) {
			java.awt.Color ink = RetailPOSColors.ink();
			graphics.setColor(new java.awt.Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 110));
			graphics.fillRect(0, 0, width, 750);
		}
		int x = (width - size.width) / 2;
		int y = (750 - size.height) / 2;
		graphics.translate(x, y);
		content.paint(graphics);
		graphics.setColor(RetailPOSColors.border());
		graphics.drawRect(0, 0, size.width - 1, size.height - 1);
		graphics.dispose();
		ImageIO.write(image, "png",
				new File(Path.of("build", "reports", "replenishment-screenshots").toFile(),
						(theme instanceof FlatDarkLaf ? "dark" : "light") + "-" + locale.getLanguage() + "-" + name
								+ "-" + width + ".png"));
	}
	private static void layout(Container container) {
		container.doLayout();
		for (Component child : container.getComponents())
			if (child instanceof Container)
				layout((Container) child);
	}
	private static <T extends Component> T first(Container root, Class<T> type) {
		for (Component child : root.getComponents()) {
			if (type.isInstance(child))
				return type.cast(child);
			if (child instanceof Container) {
				T result = first((Container) child, type);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static <T extends Component> java.util.List<T> components(Container root, Class<T> type) {
		java.util.List<T> found = new java.util.ArrayList<>();
		for (Component child : root.getComponents()) {
			if (type.isInstance(child))
				found.add(type.cast(child));
			if (child instanceof Container)
				found.addAll(components((Container) child, type));
		}
		return found;
	}
	private static JPanel cards(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JPanel && ((JPanel) child).getLayout() instanceof CardLayout)
				return (JPanel) child;
			if (child instanceof Container) {
				JPanel result = cards((Container) child);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static JButton button(Container root, String text) {
		for (Component child : root.getComponents()) {
			if (child instanceof JButton && text.equals(((JButton) child).getText()))
				return (JButton) child;
			if (child instanceof Container) {
				JButton result = button((Container) child, text);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static JButton namedButton(Container root, String name) {
		for (Component child : root.getComponents()) {
			if (child instanceof JButton && name.equals(child.getName()))
				return (JButton) child;
			if (child instanceof Container) {
				JButton result = namedButton((Container) child, name);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static JLabel label(Container root, String text) {
		for (Component child : root.getComponents()) {
			if (child instanceof JLabel && text.equals(((JLabel) child).getText()))
				return (JLabel) child;
			if (child instanceof Container) {
				JLabel result = label((Container) child, text);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static JToggleButton toggle(Container root, String text) {
		for (Component child : root.getComponents()) {
			if (child instanceof JToggleButton && text.equals(((JToggleButton) child).getText()))
				return (JToggleButton) child;
			if (child instanceof Container) {
				JToggleButton result = toggle((Container) child, text);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static JScrollPane tableScroll(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JScrollPane && ((JScrollPane) child).getViewport().getView() instanceof JTable)
				return (JScrollPane) child;
			if (child instanceof Container) {
				JScrollPane result = tableScroll((Container) child);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static JPanel sidebar(ReplenishmentPanel panel) {
		for (Component child : panel.getComponents())
			if (child instanceof JPanel && "replenishment.supplierSidebar".equals(child.getName()))
				return (JPanel) child;
		return null;
	}
	private static JPanel namedPanel(Container root, String name) {
		for (Component child : root.getComponents()) {
			if (child instanceof JPanel && name.equals(child.getName()))
				return (JPanel) child;
			if (child instanceof Container) {
				JPanel result = namedPanel((Container) child, name);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static String sidebarCount(ReplenishmentPanel panel, String supplierName) {
		JButton supplierButton = button(sidebar(panel), supplierName);
		assertTrue(supplierButton != null, "Missing supplier sidebar item " + supplierName);
		return first(supplierButton.getParent(), JLabel.class).getText();
	}
	private static void selectSupplier(JComboBox<?> filter, String id) {
		for (int i = 0; i < filter.getItemCount(); i++)
			if (id.equals(((ReplenishmentSupplier) filter.getItemAt(i)).id)) {
				filter.setSelectedIndex(i);
				return;
			}
		throw new AssertionError("Supplier missing from filter: " + id);
	}
	private static JTextArea editableArea(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JTextArea && ((JTextArea) child).isEditable())
				return (JTextArea) child;
			if (child instanceof Container) {
				JTextArea result = editableArea((Container) child);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static JTextArea areaContaining(Container root, String text) {
		for (Component child : root.getComponents()) {
			if (child instanceof JTextArea && ((JTextArea) child).getText().contains(text))
				return (JTextArea) child;
			if (child instanceof Container) {
				JTextArea result = areaContaining((Container) child, text);
				if (result != null)
					return result;
			}
		}
		return null;
	}
}
