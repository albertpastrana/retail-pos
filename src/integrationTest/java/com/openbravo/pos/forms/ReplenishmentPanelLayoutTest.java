package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.inventory.DataLogicReplenishment;
import com.openbravo.pos.inventory.ReplenishmentEntry;
import com.openbravo.pos.inventory.ReplenishmentPanel;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ReplenishmentPanelLayoutTest {
	@Test
	void selectedStatusAndNarrowFormStayReadableInAllLanguages() throws Exception {
		String url = "jdbc:derby:memory:replenishmentLayout" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		DataLogicReplenishment data = new DataLogicReplenishment();
		data.init(session);
		ReplenishmentEntry request = new ReplenishmentEntry();
		request.manualDescription = "Extra long product description that should wrap instead of clipping";
		request.createdBy = "2";
		request.customerId = "customer";
		request.customerName = "A customer with a long name that should wrap";
		data.add(request);
		data.updateStatus(request.id, "RECEIVED", "2");
		AppUser user = new AppUser("2", "Employee", null, null, "2", null);
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
		Locale previous = Locale.getDefault();
		javax.swing.LookAndFeel previousTheme = UIManager.getLookAndFeel();
		try {
			RetailPOSTheme.registerDefaultsSource();
			for (javax.swing.LookAndFeel theme : new javax.swing.LookAndFeel[]{new FlatLightLaf(), new FlatDarkLaf()}) {
				UIManager.setLookAndFeel(theme);
				RetailPOSTheme.applyFonts();
				for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
					data.updateStatus(request.id, "RECEIVED", "2");
					AppLocal.setLocale(locale);
					SwingUtilities.invokeAndWait(() -> {
						try {
							ReplenishmentPanel panel = new ReplenishmentPanel();
							panel.init(app);
							panel.activate();
							for (int width : new int[]{980, 1200}) {
								panel.setSize(width, 750);
								layout(panel);
								first(panel, JTable.class).doLayout();
								JScrollPane formScroll = formScroll(panel);
								assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
										formScroll.getHorizontalScrollBarPolicy());
								assertTrue(formScroll.getViewport().getView().getWidth() <= formScroll.getViewport()
										.getWidth());
								JTable sizedTable = first(panel, JTable.class);
								JButton footer = button(panel, AppLocal.getIntString("Replenishment.FormPending"));
								int footerY = SwingUtilities.convertPoint(footer, 0, 0, panel).y;
								JLabel footerLabel = label(panel, AppLocal.getIntString("Replenishment.MarkAs"));
								int lastRight = SwingUtilities.convertPoint(footerLabel, footerLabel.getWidth(), 0,
										panel).x;
								for (String key : new String[]{"Replenishment.FormPending", "Replenishment.FormOrdered",
										"Replenishment.FormReceived", "Replenishment.FormDelivered",
										"Replenishment.Delete", "Replenishment.Export"}) {
									JButton action = button(panel, AppLocal.getIntString(key));
									assertEquals(footer.getHeight(), action.getHeight(), key);
									assertEquals(footerY, SwingUtilities.convertPoint(action, 0, 0, panel).y, key);
									int left = SwingUtilities.convertPoint(action, 0, 0, panel).x;
									assertTrue(left >= lastRight, key + " must not overlap the previous action");
									lastRight = left + action.getWidth();
									assertTrue(action.getWidth() >= action.getFontMetrics(action.getFont()).stringWidth(
											action.getText()) + action.getInsets().left + action.getInsets().right,
											key + " must show its entire label");
								}
								assertFalse(sizedTable.getValueAt(0, 5).toString().contains(" - "));
								assertTrue(sizedTable.getRowHeight(0) >= 48);
								assertTrue(sizedTable.getColumnModel().getColumn(2).getWidth() > sizedTable
										.getColumnModel().getColumn(4).getWidth());
								assertEquals(6, sizedTable.getColumnCount());
								assertTrue(sizedTable.getColumnModel().getColumn(3).getWidth() >= 155);
								int dateWidth = sizedTable.getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f))
										.stringWidth(sizedTable.getValueAt(0, 5).toString()) + 8;
								assertTrue(sizedTable.getColumnModel().getColumn(5).getWidth() >= dateWidth);
								JLabel heading = (JLabel) sizedTable.getTableHeader().getDefaultRenderer()
										.getTableCellRendererComponent(sizedTable, sizedTable.getColumnName(1), false,
												false, -1, 1);
								assertEquals(JLabel.LEFT, heading.getHorizontalAlignment());
								assertEquals(8, heading.getBorder().getBorderInsets(heading).left);
							}
							JTable table = first(panel, JTable.class);
							table.setRowSelectionInterval(0, 0);
							layout(panel);
							assertTrue(
									hasLabel(panel, AppLocal.getIntString("Replenishment.CreatedBy") + ": Employee"));
							assertEquals(RetailPOSColors.ink(), table.getCellRenderer(0, 1)
									.getTableCellRendererComponent(table, table.getValueAt(0, 1), true, false, 0, 1)
									.getForeground());
							assertEquals(RetailPOSColors.surface200(), table.getCellRenderer(0, 1)
									.getTableCellRendererComponent(table, table.getValueAt(0, 1), true, false, 0, 1)
									.getBackground());
							JCheckBox selectedCheck = (JCheckBox) table.getCellRenderer(0, 0)
									.getTableCellRendererComponent(table, table.getValueAt(0, 0), true, false, 0, 0);
							assertTrue(selectedCheck.isOpaque());
							assertEquals(RetailPOSColors.surface200(), selectedCheck.getBackground());
							assertEquals(1, selectedCheck.getBorder().getBorderInsets(selectedCheck).top);
							assertTrue(table.getRowHeight() >= 48);
							assertEquals(AppLocal.getIntString("Replenishment.Order"), table.getValueAt(0, 1));
							assertEquals(AppLocal.getIntString("Replenishment.FormReceived"), table.getValueAt(0, 4));
							assertTrue(toggle(panel, AppLocal.getIntString("Replenishment.Received") + " (1)")
									.isVisible());
							assertFalse(RetailPOSColors.brand().equals(
									button(panel, AppLocal.getIntString("Replenishment.Save")).getBackground()));
							assertTrue(button(panel, AppLocal.getIntString("Replenishment.Save")).isVisible());
							assertFalse(button(panel, AppLocal.getIntString("Replenishment.Save")).isEnabled());
							assertEquals(RetailPOSColors.surface100(),
									button(panel, AppLocal.getIntString("Replenishment.Delete")).getBackground());
							assertFalse(RetailPOSColors.brand()
									.equals(button(panel, "+ " + AppLocal.getIntString("Replenishment.NewRequest"))
											.getBackground()));
							assertEquals(RetailPOSColors.ink(), table.getCellRenderer(0, 1)
									.getTableCellRendererComponent(table, table.getValueAt(0, 1), false, false, 0, 1)
									.getForeground());
							assertEquals(RetailPOSColors.surface100(), table.getCellRenderer(0, 0)
									.getTableCellRendererComponent(table, table.getValueAt(0, 0), false, false, 0, 0)
									.getBackground());
							JScrollPane formScroll = formScroll(panel);
							Container form = (Container) formScroll.getViewport().getView();
							JTextField barcode = first(form, JTextField.class);
							assertTrue(barcode.getFont().getFamily().contains("IBM Plex Mono"));
							assertEquals(RetailPOSColors.surface200(), barcode.getBackground());
							JTextField search = first(panel, JTextField.class);
							assertEquals(AppLocal.getIntString("Replenishment.SearchPlaceholder"),
									search.getClientProperty("JTextField.placeholderText"));
							assertEquals("", search.getText());
							assertFalse(barcode.isVisible());
							assertFalse(hasVisibleLabel(form, AppLocal.getIntString("Replenishment.Scan")));
							assertFalse(hasVisibleLabel(form, AppLocal.getIntString("Replenishment.WithoutCode")));
							assertFalse(hasVisibleLabel(form, AppLocal.getIntString("Replenishment.Description")));
							assertFalse(button(panel, AppLocal.getIntString("Replenishment.SearchProduct")).getParent()
									.isVisible());
							JToggleButton formDelivery = toggle(form,
									AppLocal.getIntString("Replenishment.FormDelivered"));
							assertTrue(formDelivery.isEnabled());
							assertTrue(toggle(form, AppLocal.getIntString("Replenishment.FormReceived")).isSelected());
							JTextArea note = editableArea(form);
							note.setText("Updated note");
							search.setText("Extra long");
							search.postActionEvent();
							assertEquals(0, table.getSelectedRow());
							assertEquals("Updated note", note.getText(), "Search must preserve unsaved edits");
							search.setText("");
							search.postActionEvent();
							assertEquals("Updated note", note.getText());
							assertTrue(button(panel, AppLocal.getIntString("Replenishment.Save")).isEnabled());
							assertEquals(RetailPOSColors.brand(),
									button(panel, AppLocal.getIntString("Replenishment.Save")).getBackground());
							note.setText("");
							assertFalse(button(panel, AppLocal.getIntString("Replenishment.Save")).isEnabled());
							for (int width : new int[]{980, 1200}) {
								panel.setSize(width, 750);
								layout(panel);
								first(panel, JTable.class).doLayout();
								assertTrue(table.getRowHeight(0) >= 48 && table.getRowHeight(0) <= 80);
								if (width == 980) {
									Component cell = table.getCellRenderer(0, 2).getTableCellRendererComponent(table,
											table.getValueAt(0, 2), true, false, 0, 2);
									assertTrue(first((Container) cell, JTextArea.class).getText().contains("…"));
									java.awt.Rectangle bounds = table.getCellRect(0, 2, true);
									java.awt.event.MouseEvent hover = new java.awt.event.MouseEvent(table,
											java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, bounds.x + 8, bounds.y + 8, 0,
											false);
									assertEquals(request.displayName(), table.getToolTipText(hover));
								}
								BufferedImage image = new BufferedImage(width, 750, BufferedImage.TYPE_INT_RGB);
								panel.paint(image.getGraphics());
								layout(panel);
								table.doLayout();
								image = new BufferedImage(width, 750, BufferedImage.TYPE_INT_RGB);
								panel.paint(image.getGraphics());
								File directory = new File("build/reports/replenishment-screenshots");
								assertTrue(directory.isDirectory() || directory.mkdirs());
								ImageIO.write(image, "png",
										new File(directory, (theme instanceof FlatDarkLaf ? "dark" : "light") + "-"
												+ locale.getLanguage() + "-" + width + ".png"));
							}
							formDelivery.doClick();
							assertTrue(button(panel, AppLocal.getIntString("Replenishment.Save")).isEnabled());
							button(panel, AppLocal.getIntString("Replenishment.Save")).doClick();
							assertEquals(0, table.getSelectedRow());
							assertEquals(AppLocal.getIntString("Replenishment.FormDelivered"), table.getValueAt(0, 4));
							assertTrue(formDelivery.isSelected());
							button(panel, "+ " + AppLocal.getIntString("Replenishment.NewRequest")).doClick();
							assertTrue(button(panel, AppLocal.getIntString("Replenishment.SearchProduct")).getParent()
									.isVisible());
							assertTrue(hasLabel(form, AppLocal.getIntString("Replenishment.Scan")));
							assertTrue(barcode.getPreferredSize().height >= 48);
							assertFalse(toggle(form, AppLocal.getIntString("Replenishment.FormDelivered")).isVisible());
							assertEquals(0,
									visibleHintCount(panel, AppLocal.getIntString("Replenishment.DeliveryHint")));
							assertTrue(hasVisibleLabel(form, AppLocal.getIntString("Replenishment.WithoutCode")));
							assertTrue(hasVisibleLabel(form, AppLocal.getIntString("Replenishment.Description")));
							for (int width : new int[]{980, 1200}) {
								panel.setSize(width, 750);
								layout(panel);
								table.doLayout();
								assertTrue(barcode.getHeight() >= 48);
								BufferedImage newRequestImage = new BufferedImage(width, 750,
										BufferedImage.TYPE_INT_RGB);
								panel.paint(newRequestImage.getGraphics());
								layout(panel);
								table.doLayout();
								newRequestImage = new BufferedImage(width, 750, BufferedImage.TYPE_INT_RGB);
								panel.paint(newRequestImage.getGraphics());
								ImageIO.write(newRequestImage, "png",
										new File("build/reports/replenishment-screenshots",
												(theme instanceof FlatDarkLaf ? "dark" : "light") + "-"
														+ locale.getLanguage() + "-new-" + width + ".png"));
							}
							data.updateStatus(request.id, "PENDING", "2");
							data.updateDetails(request.id, "", null, null, "2");
							data.updateStatus(request.id, "RECEIVED", "2");
							panel.activate();
							table.setRowSelectionInterval(0, 0);
							layout(panel);
							table.doLayout();
							assertTrue(table.getRowHeight(0) >= 48 && table.getRowHeight(0) <= 80);
							assertFalse(
									button(panel, AppLocal.getIntString("Replenishment.FormDelivered")).isEnabled());
							assertFalse(toggle(form, AppLocal.getIntString("Replenishment.FormDelivered")).isEnabled());
							assertEquals(1,
									visibleHintCount(panel, AppLocal.getIntString("Replenishment.DeliveryHint")));
							if (theme instanceof FlatLightLaf && "ca".equals(locale.getLanguage())) {
								panel.setSize(980, 750);
								layout(panel);
								BufferedImage blockedImage = new BufferedImage(980, 750, BufferedImage.TYPE_INT_RGB);
								panel.paint(blockedImage.getGraphics());
								ImageIO.write(blockedImage, "png", new File("build/reports/replenishment-screenshots",
										"light-ca-blocked-980.png"));
							}
							data.updateDetails(request.id, "", "customer",
									"A customer with a long name that should wrap", "2");
							java.util.List<String> extraIds = new java.util.ArrayList<>();
							for (int i = 0; i < 8; i++) {
								ReplenishmentEntry shortRequest = new ReplenishmentEntry();
								shortRequest.productId = "layout-product-" + i;
								shortRequest.reference = "31UUUL63A0" + i + (i == 1 ? " M/48" : "");
								shortRequest.name = i == 0
										? "Item 0 [31UUUL63A00]"
										: i == 1 ? "Item 1 — M/48" : "Item " + i;
								shortRequest.createdBy = "2";
								data.add(shortRequest);
								extraIds.add(shortRequest.id);
							}
							panel.activate();
							panel.setSize(980, 750);
							layout(panel);
							table.doLayout();
							assertEquals(9, table.getRowCount());
							table.setRowSelectionInterval(0, 1);
							assertEquals(2, table.getSelectedRowCount());
							assertEquals(Boolean.TRUE, table.getValueAt(0, 0));
							assertEquals(Boolean.TRUE, table.getValueAt(1, 0));
							assertTrue(button(panel, AppLocal.getIntString("Replenishment.FormReceived") + " (2)")
									.isVisible());
							search.setText("Item");
							search.postActionEvent();
							assertEquals(0, table.getSelectedRowCount());
							search.setText("");
							search.postActionEvent();
							JPanel productCell = (JPanel) table.getCellRenderer(0, 2)
									.getTableCellRendererComponent(table, table.getValueAt(0, 2), false, false, 0, 2);
							productCell.setSize(table.getColumnModel().getColumn(2).getWidth(), table.getRowHeight(0));
							productCell.doLayout();
							JLabel reference = first(productCell, JLabel.class);
							JTextArea productName = first(productCell, JTextArea.class);
							assertTrue(reference.isVisible());
							assertEquals(reference.getX() + reference.getInsets().left,
									productName.getX() + productName.getInsets().left);
							for (int row = 0; row < table.getRowCount(); row++) {
								String raw = String.valueOf(table.getValueAt(row, 2));
								if (raw.contains("[31UUUL63A00]") || raw.contains(" — M/48")) {
									JPanel cell = (JPanel) table.getCellRenderer(row, 2).getTableCellRendererComponent(
											table, table.getValueAt(row, 2), false, false, row, 2);
									assertEquals(raw.contains("[31UUUL63A00]") ? "31UUUL63A00" : "31UUUL63A01",
											first(cell, JLabel.class).getText());
									assertFalse(first(cell, JTextArea.class).getText().contains("[31UUUL63A00]"));
									if (raw.contains(" — M/48"))
										assertTrue(first(cell, JTextArea.class).getText().contains("M/48"));
								}
							}
							for (int row = 0; row < table.getRowCount(); row++)
								assertTrue(table.getRowHeight(row) >= 48 && table.getRowHeight(row) <= 80);
							for (int row = 0; row < table.getRowCount(); row++)
								if (AppLocal.getIntString("Replenishment.Replenishment")
										.equals(table.getValueAt(row, 1))
										&& !String.valueOf(table.getValueAt(row, 2)).contains(" — M/48"))
									assertTrue(table.getRowHeight(row) <= 64);
							int contentHeight = 0;
							for (int row = 0; row < table.getRowCount(); row++)
								contentHeight += table.getRowHeight(row);
							assertTrue(tableScroll(panel).getViewport().getHeight() >= contentHeight,
									"Nine typical rows should be visible without scrolling");
							if (theme instanceof FlatLightLaf
									&& (Locale.ENGLISH.equals(locale) || "ca".equals(locale.getLanguage()))) {
								BufferedImage nineRows = new BufferedImage(980, 750, BufferedImage.TYPE_INT_RGB);
								panel.paint(nineRows.getGraphics());
								ImageIO.write(nineRows, "png", new File("build/reports/replenishment-screenshots",
										"light-" + locale.getLanguage() + "-nine-980.png"));
							}
							for (String id : extraIds)
								data.delete(id);
						} catch (Exception ex) {
							throw new AssertionError(ex);
						}
					});
				}
			}
		} finally {
			AppLocal.setLocale(previous);
			UIManager.setLookAndFeel(previousTheme);
			session.close();
		}
	}
	private static void layout(Container root) {
		root.doLayout();
		for (Component child : root.getComponents())
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
	private static JScrollPane formScroll(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JScrollPane && ((JScrollPane) child).getViewport().getView() != null
					&& "FormFields".equals(((JScrollPane) child).getViewport().getView().getClass().getSimpleName()))
				return (JScrollPane) child;
			if (child instanceof Container) {
				JScrollPane result = formScroll((Container) child);
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
	private static boolean hasLabel(Container root, String text) {
		for (Component child : root.getComponents()) {
			if (child instanceof JLabel && text.equals(((JLabel) child).getText()))
				return true;
			if (child instanceof Container && hasLabel((Container) child, text))
				return true;
		}
		return false;
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
	private static boolean hasVisibleLabel(Container root, String text) {
		for (Component child : root.getComponents()) {
			if (child instanceof JLabel && child.isVisible() && text.equals(((JLabel) child).getText()))
				return true;
			if (child instanceof Container && child.isVisible() && hasVisibleLabel((Container) child, text))
				return true;
		}
		return false;
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
	private static JTextArea editableArea(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JTextArea && ((JTextArea) child).isEditable() && child.isVisible())
				return (JTextArea) child;
			if (child instanceof Container) {
				JTextArea result = editableArea((Container) child);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static int visibleHintCount(Container root, String text) {
		int count = 0;
		for (Component child : root.getComponents()) {
			if (child.isVisible() && child instanceof JTextArea && text.equals(((JTextArea) child).getText()))
				count++;
			if (child.isVisible() && child instanceof Container)
				count += visibleHintCount((Container) child, text);
		}
		return count;
	}
}
