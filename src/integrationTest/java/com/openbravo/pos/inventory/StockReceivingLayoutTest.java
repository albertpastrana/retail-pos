package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.formdev.flatlaf.FlatLightLaf;
import com.openbravo.beans.JNumberKeys;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JComboBox;
import javax.swing.table.TableModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class StockReceivingLayoutTest {
	@Test
	void scanningAndReviewRemainUsableAtCounterWidthInAllLanguages() throws Exception {
		javax.swing.LookAndFeel originalTheme = UIManager.getLookAndFeel();
		UIManager.setLookAndFeel(new FlatLightLaf());
		try {
			String url = "jdbc:derby:memory:receivingLayout" + UUID.randomUUID() + ";create=true";
			try (Connection c = DriverManager.getConnection(url); Statement sql = c.createStatement()) {
				sql.execute("CREATE TABLE LOCATIONS (ID VARCHAR(255) PRIMARY KEY)");
				sql.execute("INSERT INTO LOCATIONS VALUES ('0')");
				sql.execute(
						"CREATE TABLE PRODUCTS (ID VARCHAR(255) PRIMARY KEY,NAME VARCHAR(255),CODE VARCHAR(255),REFERENCE VARCHAR(255))");
				sql.execute("INSERT INTO PRODUCTS VALUES ('p1','Cava Freixenet Cordón Negro 75 cl','123','REF1')");
				sql.execute("CREATE TABLE STOCKCURRENT (LOCATION VARCHAR(255),PRODUCT VARCHAR(255),UNITS DOUBLE)");
				sql.execute(
						"CREATE TABLE STOCKDIARY (ID VARCHAR(255),DATENEW TIMESTAMP,REASON INTEGER,LOCATION VARCHAR(255),PRODUCT VARCHAR(255),UNITS DOUBLE,PRICE DOUBLE)");
				sql.execute("CREATE TABLE ROLES (ID VARCHAR(255),PERMISSIONS BLOB)");
				Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration")
						.baselineOnMigrate(true).baselineVersion("49").load().migrate();
				StockSessionRepository repo = new StockSessionRepository(c);
				StockSessionRepository.Session session = repo.open("Comercial Vinícola Segre", "A-24/3318", "0",
						"user");
				repo.scan(session.id, "123", 12, true);
				repo.scan(session.id, "8437008291043", 3, true);
				Locale original = Locale.getDefault();
				try {
					for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
						StockSessionRepository.Line known = repo.lines(session.id, false).stream()
								.filter(line -> line.product != null).findFirst().orElseThrow();
						repo.quantity(session.id, known.id, 12);
						repo.tick(session.id, known.id, false);
						AppLocal.setLocale(locale);
						SwingUtilities.invokeAndWait(() -> {
							try {
								StockReceivingPanel panel = new StockReceivingPanel();
								field(panel, "repository", repo);
								field(panel, "session", session);
								Method refresh = StockReceivingPanel.class.getDeclaredMethod("refresh");
								refresh.setAccessible(true);
								for (boolean review : new boolean[]{false, true}) {
									field(panel, "reviewing", review);
									refresh.invoke(panel);
									panel.setSize(980, 700); // Stock area after the application sidebar.
									layout(panel);
									JPanel workspace = (JPanel) panel.getComponent(1);
									JPanel list = (JPanel) field(panel, "list");
									JScrollPane tools = (JScrollPane) field(panel, "tools");
									assertEquals(2, workspace.getComponentCount());
									assertEquals(BorderLayout.CENTER,
											((BorderLayout) workspace.getLayout()).getConstraints(list));
									assertEquals(BorderLayout.EAST,
											((BorderLayout) workspace.getLayout()).getConstraints(tools));
									assertTrue(list.getWidth() > (review ? 500 : 600),
											locale + " left=" + list.getWidth());
									assertTrue(tools.getWidth() > 240, locale.toString());
									if (!review) {
										JNumberKeys keys = (JNumberKeys) field(panel, "numberKeys");
										int keysRight = SwingUtilities.convertPoint(keys, keys.getWidth(), 0,
												tools.getViewport()).x;
										assertTrue(keysRight <= tools.getViewport().getWidth(),
												locale + " keypad right=" + keysRight + " viewport="
														+ tools.getViewport().getWidth());
										assertTrue(SwingUtilities
												.isDescendingFrom((JButton) field(panel, "removeAction"), list));
										assertEquals(((javax.swing.JTextField) field(panel, "code")).getWidth(),
												((JButton) field(panel, "searchAction")).getWidth());
										assertTrue(((JButton) field(panel, "searchAction")).getIcon() != null);
									}
									JTable table = (JTable) field(panel, "table");
									assertEquals(review ? 6 : 4, table.getColumnCount());
									assertEquals(AppLocal.getIntString("receiving.units"), table.getColumnName(3));
									assertEquals(RetailPOSTheme.PLEX_MONO_REGULAR, table.getFont());
									assertEquals(SwingConstants.RIGHT, ((DefaultTableCellRenderer) table
											.getColumnModel().getColumn(3).getCellRenderer()).getHorizontalAlignment());
									assertTrue(table.getColumnModel().getColumn(0).getHeaderRenderer()
											.getTableCellRendererComponent(table, "", false, false, 0,
													0) instanceof JCheckBox);
									assertEquals(SwingConstants.LEFT,
											((javax.swing.JLabel) table.getColumnModel().getColumn(1)
													.getHeaderRenderer()
													.getTableCellRendererComponent(table, "", false, false, 0, 1))
													.getHorizontalAlignment());
									assertEquals(SwingConstants.RIGHT,
											((javax.swing.JLabel) table.getColumnModel().getColumn(3)
													.getHeaderRenderer()
													.getTableCellRendererComponent(table, "", false, false, 0, 3))
													.getHorizontalAlignment());
									BufferedImage image = new BufferedImage(980, 700, BufferedImage.TYPE_INT_RGB);
									java.awt.Graphics2D g = image.createGraphics();
									try {
										panel.paint(g);
									} finally {
										g.dispose();
									}
									ImageIO.write(image, "png", new File("build/receiving-" + locale.getLanguage()
											+ (review ? "-review" : "-scan") + ".png"));
								}
								field(panel, "reviewing", false);
								refresh.invoke(panel);
								for (int width : new int[]{320, 600, 850, 1200}) {
									panel.setSize(width, 700);
									layout(panel);
									layout(panel);
									JPanel resized = (JPanel) panel.getComponent(1);
									JPanel list = (JPanel) field(panel, "list");
									JScrollPane tools = (JScrollPane) field(panel, "tools");
									JNumberKeys keys = (JNumberKeys) field(panel, "numberKeys");
									assertEquals(width < 850 ? BorderLayout.NORTH : BorderLayout.CENTER,
											((BorderLayout) resized.getLayout()).getConstraints(list),
											locale + " width=" + width);
									assertEquals(width < 850 ? BorderLayout.CENTER : BorderLayout.EAST,
											((BorderLayout) resized.getLayout()).getConstraints(tools),
											locale + " width=" + width);
									assertTrue(
											SwingUtilities.convertPoint(keys, keys.getWidth(), 0,
													tools.getViewport()).x <= tools.getViewport().getWidth(),
											locale + " width=" + width);
									assertTrue(list.getWidth() > (width <= 600 ? 200 : width == 850 ? 460 : 800),
											locale + " width=" + width + " list=" + list.getWidth() + " tools="
													+ tools.getWidth());
									if ((width == 850 || width == 600 || width == 320)
											&& "ca".equals(locale.getLanguage())) {
										BufferedImage narrow = new BufferedImage(width, 700,
												BufferedImage.TYPE_INT_RGB);
										java.awt.Graphics2D graphics = narrow.createGraphics();
										try {
											panel.paint(graphics);
										} finally {
											graphics.dispose();
										}
										ImageIO.write(narrow, "png", new File("build/receiving-ca-" + width + ".png"));
									}
								}
								panel.setSize(980, 700);
								layout(panel);
								JTable table = (JTable) field(panel, "table");
								TableModel model = table.getModel();
								int knownRow = model.getValueAt(0, 2).toString().startsWith("123") ? 0 : 1;
								table.setRowSelectionInterval(knownRow, knownRow);
								assertEquals("12",
										((javax.swing.JTextField) field(panel, "selectedQuantity")).getText());
								panel.setSize(980, 700);
								layout(panel);
								BufferedImage selectedImage = new BufferedImage(980, 700, BufferedImage.TYPE_INT_RGB);
								java.awt.Graphics2D selectedGraphics = selectedImage.createGraphics();
								try {
									panel.paint(selectedGraphics);
								} finally {
									selectedGraphics.dispose();
								}
								ImageIO.write(selectedImage, "png",
										new File("build/receiving-" + locale.getLanguage() + "-selected.png"));
								Method numberKey = StockReceivingPanel.class.getDeclaredMethod("numberKey", char.class);
								numberKey.setAccessible(true);
								numberKey.invoke(panel, '6');
								assertEquals("6",
										((javax.swing.JTextField) field(panel, "selectedQuantity")).getText());
								numberKey.invoke(panel, '4');
								assertEquals("64",
										((javax.swing.JTextField) field(panel, "selectedQuantity")).getText());
								numberKey.invoke(panel, '\u007f');
								assertEquals("", ((javax.swing.JTextField) field(panel, "selectedQuantity")).getText());
								assertEquals("12", model.getValueAt(knownRow, 3)); // Keypad entry needs confirmation.
								model.setValueAt("7", knownRow, 3);
								assertEquals("7", model.getValueAt(knownRow, 3));
								assertEquals(knownRow, table.getSelectedRow());
								java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(table.getTableHeader(),
										java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 8, 8, 1,
										false);
								table.getTableHeader().dispatchEvent(click);
								assertEquals(true, model.getValueAt(0, 0));
								assertEquals(true, model.getValueAt(1, 0));
								table.getTableHeader().dispatchEvent(click);
								assertEquals(false, model.getValueAt(0, 0));
								assertEquals(false, model.getValueAt(1, 0));
								model.setValueAt(true, knownRow, 0);
								assertEquals(true, model.getValueAt(knownRow, 0));
								((JComboBox<?>) field(panel, "sort")).setSelectedIndex(1);
								assertEquals("7", model.getValueAt(table.getSelectedRow(), 3));
								assertTrue(SwingUtilities.isDescendingFrom((JComboBox<?>) field(panel, "sort"),
										(JPanel) field(panel, "list")));
								javax.swing.JTextField code = (javax.swing.JTextField) field(panel, "code");
								code.setText("123");
								code.postActionEvent();
								assertEquals("", code.getText());
								assertEquals(-1, table.getSelectedRow());
								assertEquals("8", model.getValueAt(0, 3));
							} catch (Exception ex) {
								throw new AssertionError(ex);
							}
						});
					}
				} finally {
					AppLocal.setLocale(original);
				}
			}
		} finally {
			UIManager.setLookAndFeel(originalTheme);
		}
	}

	private static Object field(Object object, String name) throws Exception {
		Field field = object.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(object);
	}

	private static void field(Object object, String name, Object value) throws Exception {
		Field field = object.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(object, value);
	}

	private static void layout(Component component) {
		if (component instanceof Container container) {
			container.doLayout();
			for (Component child : container.getComponents())
				layout(child);
		}
	}
}
