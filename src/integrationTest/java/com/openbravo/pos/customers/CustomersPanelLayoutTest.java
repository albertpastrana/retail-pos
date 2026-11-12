package com.openbravo.pos.customers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import org.junit.jupiter.api.Test;

class CustomersPanelLayoutTest {
	@Test
	void customerFieldsFitAtTillSizeInBothThemes() throws Exception {
		RetailPOSTheme.registerDefaultsSource();
		Locale previous = Locale.getDefault();
		try {
			for (boolean dark : new boolean[]{false, true}) {
				UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
				RetailPOSTheme.applyFonts();
				for (Locale locale : new Locale[]{Locale.forLanguageTag("ca"), Locale.forLanguageTag("es"),
						Locale.forLanguageTag("en")}) {
					Locale.setDefault(locale);
					for (int[] size : new int[][]{{700, 650}, {800, 650}, {900, 720}, {1024, 768}, {1600, 900}}) {
						SwingUtilities.invokeAndWait(() -> {
							CustomersPanel panel = new CustomersPanel();
							panel.setSize(size[0], size[1]);
							layoutTree(panel);
							try {
								JTextField name = field(panel, "name", JTextField.class);
								JTextField maxDebt = field(panel, "maxDebt", JTextField.class);
								JTextArea notes = field(panel, "notes", JTextArea.class);
								notes.setEnabled(true);
								name.setEnabled(true);
								maxDebt.setEnabled(true);
								JScrollPane notesScroll = (JScrollPane) notes.getParent().getParent();
								JScrollPane detailScroll = (JScrollPane) notesScroll.getParent().getParent()
										.getParent();
								assertEquals(name.getHeight(), maxDebt.getHeight());
								assertTrue(maxDebt.getY() - notesScroll.getY() - notesScroll.getHeight() < 55);
								assertTrue(
										notesScroll.getHeight() >= notes.getFontMetrics(notes.getFont()).getHeight()
												* 3,
										size[0] + "x" + size[1] + " notes=" + notesScroll.getSize() + " content="
												+ notesScroll.getParent().getSize() + " pref="
												+ notesScroll.getParent().getPreferredSize() + " viewport="
												+ detailScroll.getViewport().getSize());
								if (size[0] >= 900)
									assertFalse(notesScroll.getVerticalScrollBar().isVisible());
								assertEquals(RetailPOSColors.surface200(), notes.getBackground());
								assertTrue(name.isEditable());
								for (String text : new String[]{"Nom", "Telèfon", "Notes", "Deute permès (€)",
										"Deute acumulat"}) {
									JLabel label = findLabel(detailScroll, text);
									assertTrue(label != null, text);
									Rectangle bounds = SwingUtilities.convertRectangle(label.getParent(),
											label.getBounds(), detailScroll.getViewport());
									assertTrue(bounds.y >= 0
											&& bounds.y + bounds.height <= detailScroll.getViewport().getHeight(),
											text);
									assertTrue(
											bounds.x >= 0
													&& bounds.x + bounds.width <= detailScroll.getViewport().getWidth(),
											text);
								}
								JTextArea help = findTextArea(detailScroll,
										"El deute acumulat es calcula a partir dels albarans.");
								assertTrue(help != null);
								assertTrue(help.getHeight() >= help.getFontMetrics(help.getFont()).getHeight() * 2);
								Rectangle helpBounds = SwingUtilities.convertRectangle(help.getParent(),
										help.getBounds(), detailScroll.getViewport());
								assertTrue(helpBounds.x + helpBounds.width <= detailScroll.getViewport().getWidth());
								JSplitPane split = findSplit(panel);
								assertTrue(split.getRightComponent().getWidth() < panel.getWidth());
								assertTrue(split.getLeftComponent().getWidth() > 0);
								if (size[0] >= 900)
									assertTrue(
											split.getRightComponent().getWidth() < split.getLeftComponent().getWidth(),
											size[0] + " right=" + split.getRightComponent().getWidth() + " left="
													+ split.getLeftComponent().getWidth());
								JTextField search = field(panel, "search", JTextField.class);
								JButton create = field(panel, "newCustomer", JButton.class);
								Rectangle searchBounds = SwingUtilities.convertRectangle(search.getParent(),
										search.getBounds(), split.getLeftComponent());
								Rectangle createBounds = SwingUtilities.convertRectangle(create.getParent(),
										create.getBounds(), split.getLeftComponent());
								assertTrue(searchBounds.width >= search.getMinimumSize().width);
								assertTrue(createBounds.x + createBounds.width <= split.getLeftComponent().getWidth());
								JLabel shortcut = findLabel(split.getLeftComponent(),
										"↑↓ moure · Enter editar · Ctrl+N nova");
								Rectangle shortcutBounds = SwingUtilities.convertRectangle(shortcut.getParent(),
										shortcut.getBounds(), split.getLeftComponent());
								assertTrue(shortcutBounds.x >= 0);
								assertTrue(
										shortcutBounds.x + shortcutBounds.width <= split.getLeftComponent().getWidth());
								Rectangle detailBounds = SwingUtilities.convertRectangle(
										split.getRightComponent().getParent(), split.getRightComponent().getBounds(),
										panel);
								assertTrue(detailBounds.x + detailBounds.width <= panel.getWidth());
								JButton save = field(panel, "save", JButton.class);
								JButton cancel = field(panel, "cancel", JButton.class);
								JButton archive = field(panel, "archive", JButton.class);
								CustomerInfoExt customer = new CustomerInfoExt("customer");
								customer.setName("Clienta");
								customer.setVisible(true);
								Method show = CustomersPanel.class.getDeclaredMethod("showCustomer",
										CustomerInfoExt.class);
								show.setAccessible(true);
								show.invoke(panel, customer);
								layoutTree(panel);
								assertEquals(save.getY(), cancel.getY());
								assertEquals(SwingUtilities.convertPoint(save.getParent(), 0, save.getY(), panel).y,
										SwingUtilities.convertPoint(archive.getParent(), 0, archive.getY(), panel).y);
								assertTrue(save.getWidth() >= save.getPreferredSize().width);
								assertTrue(archive.getWidth() >= archive.getPreferredSize().width);
								Rectangle cancelBounds = SwingUtilities.convertRectangle(cancel.getParent(),
										cancel.getBounds(), panel);
								Rectangle archiveBounds = SwingUtilities.convertRectangle(archive.getParent(),
										archive.getBounds(), panel);
								assertTrue(cancelBounds.x + cancelBounds.width < archiveBounds.x);
								assertTrue(archiveBounds.x + archiveBounds.width <= panel.getWidth());
							} catch (ReflectiveOperationException e) {
								throw new RuntimeException(e);
							}
						});
					}
				}
			}
		} finally {
			Locale.setDefault(previous);
		}
	}

	@Test
	void tableHeadersMatchCellsAndPhoneUsesDesignSystemMono() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			CustomersPanel panel = new CustomersPanel();
			try {
				JTable table = field(panel, "table", JTable.class);
				for (int column = 0; column < 3; column++) {
					TableCellRenderer cell = table.getColumnModel().getColumn(column).getCellRenderer();
					if (cell == null)
						cell = table.getDefaultRenderer(String.class);
					JLabel body = (JLabel) cell.getTableCellRendererComponent(table, column == 2 ? 123.0 : "123", false,
							false, 0, column);
					JLabel header = (JLabel) table.getColumnModel().getColumn(column).getHeaderRenderer()
							.getTableCellRendererComponent(table, table.getColumnName(column), false, false, -1,
									column);
					assertEquals(body.getHorizontalAlignment(), header.getHorizontalAlignment());
					assertEquals(body.getBorder().getBorderInsets(body).left,
							header.getBorder().getBorderInsets(header).left);
					assertEquals(body.getBorder().getBorderInsets(body).right,
							header.getBorder().getBorderInsets(header).right);
					if (column == 1)
						assertEquals(RetailPOSTheme.PLEX_MONO_REGULAR, body.getFont());
				}
				assertTrue(findButton(panel, "×") == null);
				assertEquals("new",
						panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("control N")));
				assertEquals("cancel",
						panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("ESCAPE")));
				assertEquals("edit", table.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER")));
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static JLabel findLabel(Component component, String text) {
		if (component instanceof JLabel label && label.getText().replaceAll("<[^>]*>", "").equals(text))
			return label;
		if (component instanceof Container container)
			for (Component child : container.getComponents()) {
				JLabel label = findLabel(child, text);
				if (label != null)
					return label;
			}
		return null;
	}

	private static JTextArea findTextArea(Component component, String text) {
		if (component instanceof JTextArea area && text.equals(area.getText()))
			return area;
		if (component instanceof Container container)
			for (Component child : container.getComponents()) {
				JTextArea area = findTextArea(child, text);
				if (area != null)
					return area;
			}
		return null;
	}

	private static JSplitPane findSplit(Component component) {
		if (component instanceof JSplitPane split)
			return split;
		if (component instanceof Container container)
			for (Component child : container.getComponents()) {
				JSplitPane split = findSplit(child);
				if (split != null)
					return split;
			}
		return null;
	}

	private static JButton findButton(Component component, String text) {
		if (component instanceof JButton button && text.equals(button.getText()))
			return button;
		if (component instanceof Container container)
			for (Component child : container.getComponents()) {
				JButton button = findButton(child, text);
				if (button != null)
					return button;
			}
		return null;
	}

	private static <T> T field(CustomersPanel panel, String name, Class<T> type) throws ReflectiveOperationException {
		Field field = CustomersPanel.class.getDeclaredField(name);
		field.setAccessible(true);
		return type.cast(field.get(panel));
	}

	private static void layoutTree(Component component) {
		if (component instanceof Container container) {
			container.doLayout();
			for (Component child : container.getComponents()) {
				layoutTree(child);
			}
		}
	}
}
