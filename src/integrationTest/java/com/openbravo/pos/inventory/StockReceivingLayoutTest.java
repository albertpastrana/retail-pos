package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.formdev.flatlaf.FlatLightLaf;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.ticket.ProductInfoExt;
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
import org.flywaydb.core.internal.jdbc.DriverDataSource;
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
						"CREATE TABLE PRODUCTS (ID VARCHAR(255) PRIMARY KEY,NAME VARCHAR(255),CODE VARCHAR(255),REFERENCE VARCHAR(255),BRAND VARCHAR(255),PRICESELL DOUBLE,TAXCAT VARCHAR(255))");
				sql.execute(
						"INSERT INTO PRODUCTS VALUES ('p1','Cava Freixenet Cordón Negro 75 cl','123','00001-100 TIERRA/ARENA','Freixenet',10,'tax1')");
				sql.execute(
						"CREATE TABLE TAXES (CATEGORY VARCHAR(255),CUSTCATEGORY VARCHAR(255),VALIDFROM TIMESTAMP,RATE DOUBLE)");
				sql.execute("INSERT INTO TAXES VALUES ('tax1',NULL,'2020-01-01 00:00:00',0.21)");
				sql.execute("CREATE TABLE STOCKCURRENT (LOCATION VARCHAR(255),PRODUCT VARCHAR(255),UNITS DOUBLE)");
				sql.execute(
						"CREATE TABLE STOCKDIARY (ID VARCHAR(255),DATENEW TIMESTAMP,REASON INTEGER,LOCATION VARCHAR(255),PRODUCT VARCHAR(255),UNITS DOUBLE,PRICE DOUBLE)");
				sql.execute("CREATE TABLE ROLES (ID VARCHAR(255),PERMISSIONS BLOB)");
				Flyway.configure()
						.dataSource(new DriverDataSource(StockReceivingLayoutTest.class.getClassLoader(),
								"org.apache.derby.iapi.jdbc.AutoloadedDriver", url, null, null))
						.locations("classpath:db/migration").baselineOnMigrate(true).baselineVersion("49").load()
						.migrate();
				sql.execute("INSERT INTO STOCKCURRENT (LOCATION,PRODUCT,UNITS) VALUES ('0','p1',1.5)");
				StockSessionRepository repo = new StockSessionRepository(c);
				StockSessionRepository.Session session = repo.open("Comercial Vinícola Segre", "A-24/3318", "0",
						"user");
				repo.scan(session.id, "123", 12, true);
				repo.scan(session.id, "8437008291043", 3, true);
				StockSessionRepository.Line priced = repo.lines(session.id, false).stream()
						.filter(line -> "p1".equals(line.product)).findFirst().orElseThrow();
				assertEquals(12.1, priced.retailPrice, 0.001);
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
								assertEquals(AppLocal.getIntString("receiving.title"), panel.getTitle());
								assertEquals(18, panel.getInsets().left);
								field(panel, "repository", repo);
								field(panel, "session", session);
								Method refresh = StockReceivingPanel.class.getDeclaredMethod("refresh");
								refresh.setAccessible(true);
								int scanToolsWidth = -1;
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
									assertTrue(list.getWidth() >= (int) field(panel, "readableTableWidth"),
											locale + " table width=" + list.getWidth() + " required="
													+ field(panel, "readableTableWidth"));
									assertTrue(tools.getWidth() >= tools.getPreferredSize().width, locale + " tools="
											+ tools.getWidth() + " preferred=" + tools.getPreferredSize().width);
									assertEquals(!review, ((JPanel) field(panel, "topActions")).isVisible());
									assertEquals(AppLocal.getIntString("receiving.pending"),
											((javax.swing.JLabel) field(panel, "pendingBadge")).getText());
									assertEquals(AppLocal.getIntString("receiving.noteShort", session.note),
											((javax.swing.JLabel) field(panel, "noteHeading")).getText());
									int titleX = SwingUtilities
											.convertPoint((javax.swing.JLabel) field(panel, "heading"), 0, 0, panel).x;
									assertEquals(titleX, SwingUtilities.convertPoint(
											(javax.swing.JLabel) field(panel, "startedHint"), 0, 0, panel).x);
									assertEquals(titleX,
											SwingUtilities.convertPoint((javax.swing.JLabel) field(panel, "stockHint"),
													0, 0, panel).x);
									assertEquals(AppLocal.getIntString(review ? "receiving.back" : "receiving.review"),
											((JButton) field(panel, "reviewAction")).getText());
									if (review) {
										assertTrue(!((JButton) field(panel, "discardAction")).isVisible());
										assertEquals(AppLocal.getIntString("receiving.reviewTitle"),
												((javax.swing.JLabel) field(panel, "reviewTitle")).getText());
										assertTrue(((javax.swing.JLabel) field(panel, "reviewTitle")).isVisible());
										assertEquals(scanToolsWidth, tools.getWidth(),
												locale + " review tools changed width");
										JPanel reviewTools = (JPanel) field(panel, "actions");
										JPanel reviewNotes = (JPanel) ((BorderLayout) reviewTools.getLayout())
												.getLayoutComponent(BorderLayout.NORTH);
										JPanel reviewButtons = (JPanel) ((BorderLayout) reviewTools.getLayout())
												.getLayoutComponent(BorderLayout.SOUTH);
										assertEquals(AppLocal.getIntString("receiving.post"),
												((JButton) reviewButtons
														.getComponent(reviewButtons.getComponentCount() - 1))
														.getText());
										assertTrue(reviewNotes.getWidth() >= reviewNotes.getPreferredSize().width,
												locale + " review notes are clipped " + reviewNotes.getWidth() + " < "
														+ reviewNotes.getPreferredSize().width);
										assertTrue(reviewButtons.getWidth() >= reviewButtons.getPreferredSize().width,
												locale + " review buttons are clipped " + reviewButtons.getWidth()
														+ " < " + reviewButtons.getPreferredSize().width);
									} else {
										assertEquals(AppLocal.getIntString("receiving.later"),
												((JButton) field(panel, "laterAction")).getText());
										scanToolsWidth = tools.getWidth();
									}
									if (!review) {
										JButton discard = (JButton) field(panel, "discardAction");
										assertEquals(AppLocal.getIntString("receiving.discard"), discard.getText());
										assertTrue(SwingUtilities.isDescendingFrom(discard,
												(JPanel) field(panel, "topActions")));
										assertTrue(discard.getHeight() >= 48, locale + " discard=" + discard.getBounds()
												+ " parent=" + discard.getParent().getBounds());
										assertDiscardLabelFits(discard, locale, 980);
										JPanel card = (JPanel) field(panel, "detailCard");
										JPanel actionPanel = (JPanel) field(panel, "actions");
										JPanel scan = (JPanel) ((JPanel) actionPanel.getComponent(0)).getComponent(0);
										javax.swing.JLabel scanLabel = (javax.swing.JLabel) scan.getComponent(0);
										JTable table = (JTable) field(panel, "table");
										assertEquals(AppLocal.getIntString("receiving.scan"), scanLabel.getText());
										assertTrue(
												Math.abs(SwingUtilities.convertPoint(scanLabel, 0, 0, panel).y
														- SwingUtilities.convertPoint(table.getTableHeader(), 0, 0,
																panel).y) <= 6,
												locale + " tools are not aligned with table");
										int cardRight = SwingUtilities.convertPoint(card, card.getWidth(), 0,
												tools.getViewport()).x;
										assertTrue(cardRight <= tools.getViewport().getWidth(),
												locale + " details right=" + cardRight + " viewport="
														+ tools.getViewport().getWidth());
										assertTrue(SwingUtilities
												.isDescendingFrom((JButton) field(panel, "removeAction"), card));
										assertEquals(((javax.swing.JTextField) field(panel, "code")).getWidth(),
												((JButton) field(panel, "searchAction")).getWidth());
										assertEquals(((JButton) field(panel, "searchAction")).getHeight(),
												((javax.swing.JTextField) field(panel, "code")).getHeight());
										assertTrue(((JButton) field(panel, "searchAction")).getIcon() != null);
										assertTrue(((JButton) field(panel, "removeAction")).getIcon() != null);
										javax.swing.JLabel listTitle = (javax.swing.JLabel) ((JPanel) field(panel,
												"listHeader")).getComponent(0);
										assertEquals(switch (locale.getLanguage()) {
											case "ca" -> "Articles de la recepció";
											case "es" -> "Artículos de la recepción";
											default -> "Items in receipt";
										}, listTitle.getText());
										assertTrue(listTitle.getWidth() >= listTitle.getPreferredSize().width,
												locale + " list heading is clipped");
									}
									JTable table = (JTable) field(panel, "table");
									assertEquals(7, table.getColumnCount());
									assertEquals(AppLocal.getIntString("receiving.barcodeColumn"),
											table.getColumnName(2));
									assertEquals(AppLocal.getIntString("receiving.retailPriceColumn"),
											table.getColumnName(3));
									assertTrue(table.getColumnModel().getColumn(2).getPreferredWidth() > table
											.getColumnModel().getColumn(3).getPreferredWidth());
									for (int column = 1; column < table.getColumnCount(); column++)
										assertTrue(table.getColumnModel().getColumn(column).getMaxWidth() > 400,
												"Column " + column + " should be resizable");
									assertEquals(AppLocal.getIntString("receiving.units"), table.getColumnName(4));
									assertEquals(RetailPOSTheme.PLEX_MONO_REGULAR, table.getFont());
									assertEquals(SwingConstants.CENTER,
											((javax.swing.JLabel) table.getColumnModel().getColumn(2).getCellRenderer()
													.getTableCellRendererComponent(table, "123", false, false, 0, 2))
													.getHorizontalAlignment());
									assertEquals(SwingConstants.CENTER,
											((javax.swing.JLabel) table.getColumnModel().getColumn(2)
													.getHeaderRenderer().getTableCellRendererComponent(table,
															table.getColumnName(2), false, false, 0, 2))
													.getHorizontalAlignment());
									assertEquals(SwingConstants.RIGHT, ((DefaultTableCellRenderer) table
											.getColumnModel().getColumn(4).getCellRenderer()).getHorizontalAlignment());
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
									for (int column = 4; column < table.getColumnCount(); column++) {
										javax.swing.JLabel shortHeader = (javax.swing.JLabel) table.getColumnModel()
												.getColumn(column).getHeaderRenderer().getTableCellRendererComponent(
														table, table.getColumnName(column), false, false, 0, column);
										assertEquals(SwingConstants.RIGHT, shortHeader.getHorizontalAlignment());
										assertEquals(AppLocal.getIntString(column == 4
												? "receiving.unitsShort"
												: column == 5 ? "receiving.currentShort" : "receiving.afterShort"),
												shortHeader.getText());
										assertTrue(
												table.getColumnModel().getColumn(column)
														.getWidth() >= shortHeader.getFontMetrics(shortHeader.getFont())
																.stringWidth(shortHeader.getText()) + 12,
												locale + " short header " + column + " is clipped");
										assertTrue(shortHeader.getIcon() == null);
										assertEquals(table.getColumnName(column),
												shortHeader.getAccessibleContext().getAccessibleName());
										String hint = AppLocal.getIntString(column == 4
												? "receiving.unitsHint"
												: column == 5 ? "receiving.currentHint" : "receiving.afterHint");
										if (column == 4)
											assertTrue(!hint.contains("entrega") && !hint.contains("delivery"),
													locale + " receipt quantity hint still says delivery");
										java.awt.Rectangle headerRect = table.getTableHeader().getHeaderRect(column);
										java.awt.event.MouseEvent hover = new java.awt.event.MouseEvent(
												table.getTableHeader(), java.awt.event.MouseEvent.MOUSE_MOVED,
												System.currentTimeMillis(), 0, headerRect.x + 2, headerRect.y + 2, 0,
												false);
										assertEquals(hint, table.getTableHeader().getToolTipText(hover));
									}
									int pricedRow = "123".equals(table.getValueAt(0, 2)) ? 0 : 1;
									assertEquals("2", table.getValueAt(pricedRow, 5));
									assertEquals("14", table.getValueAt(pricedRow, 6));
									assertEquals(!review, table.getModel().isCellEditable(pricedRow, 4));
									assertEquals(Math.max(52, table.getFontMetrics(table.getFont()).getHeight() + 20),
											table.getRowHeight());
									int unknownRow = pricedRow == 0 ? 1 : 0;
									javax.swing.JLabel unknownCell = (javax.swing.JLabel) table
											.getCellRenderer(unknownRow, 1).getTableCellRendererComponent(table,
													table.getValueAt(unknownRow, 1), false, false, unknownRow, 1);
									assertEquals(AppLocal.getIntString("receiving.unknownTitle"),
											unknownCell.getText());
									assertEquals(AppLocal.getIntString("receiving.unknown"),
											unknownCell.getToolTipText());
									JScrollPane tableScroll = (JScrollPane) list.getComponent(1);
									assertTrue(!tableScroll.getHorizontalScrollBar().isVisible(),
											locale + " 980px table unexpectedly scrolls horizontally");
									BufferedImage image = new BufferedImage(980, 700, BufferedImage.TYPE_INT_RGB);
									java.awt.Graphics2D g = image.createGraphics();
									try {
										panel.paint(g);
									} finally {
										g.dispose();
									}
									ImageIO.write(image, "png", new File("build/receiving-" + locale.getLanguage()
											+ (review ? "-review" : "-scan") + ".png"));
									if (review) {
										javax.swing.JScrollBar horizontal = tableScroll.getHorizontalScrollBar();
										horizontal.setValue(horizontal.getMaximum());
										BufferedImage stockHeaders = new BufferedImage(980, 700,
												BufferedImage.TYPE_INT_RGB);
										java.awt.Graphics2D stockGraphics = stockHeaders.createGraphics();
										try {
											panel.paint(stockGraphics);
										} finally {
											stockGraphics.dispose();
										}
										ImageIO.write(stockHeaders, "png", new File(
												"build/receiving-" + locale.getLanguage() + "-review-stock.png"));
										horizontal.setValue(0);
									}
								}
								field(panel, "reviewing", false);
								refresh.invoke(panel);
								for (int width : new int[]{320, 600, 850, 1200}) {
									panel.setSize(width, 700);
									layout(panel);
									layout(panel);
									assertHeadingFits(panel, locale, width);
									JButton discard = (JButton) field(panel, "discardAction");
									assertDiscardLabelFits(discard, locale, width);
									if (width == 850) {
										java.awt.Font originalFont = discard.getFont();
										discard.setFont(originalFont.deriveFont(originalFont.getSize2D() * 1.6f));
										layout(panel);
										layout(panel);
										assertDiscardLabelFits(discard, locale, width);
										discard.setFont(originalFont);
										layout(panel);
										layout(panel);
									}
									JPanel resized = (JPanel) panel.getComponent(1);
									JPanel list = (JPanel) field(panel, "list");
									JScrollPane tools = (JScrollPane) field(panel, "tools");
									JPanel card = (JPanel) field(panel, "detailCard");
									boolean stacked = (boolean) field(panel, "compact");
									if (width <= 600)
										assertTrue(stacked, locale + " width=" + width);
									if (width == 1200)
										assertTrue(!stacked, locale + " width=" + width);
									assertEquals(stacked ? BorderLayout.NORTH : BorderLayout.CENTER,
											((BorderLayout) resized.getLayout()).getConstraints(list),
											locale + " width=" + width);
									assertEquals(stacked ? BorderLayout.CENTER : BorderLayout.EAST,
											((BorderLayout) resized.getLayout()).getConstraints(tools),
											locale + " width=" + width);
									assertTrue(
											SwingUtilities.convertPoint(card, card.getWidth(), 0,
													tools.getViewport()).x <= tools.getViewport().getWidth(),
											locale + " width=" + width);
									assertTrue(list.getWidth() > (width <= 600 ? 200 : width == 850 ? 460 : 800),
											locale + " width=" + width + " list=" + list.getWidth() + " tools="
													+ tools.getWidth());
									JScrollPane tableScroll = (JScrollPane) list.getComponent(1);
									assertEquals(width == 320, tableScroll.getHorizontalScrollBar().isVisible(),
											locale + " width=" + width + " unexpected horizontal scrollbar");
									if (width == 320
											|| (width == 850 || width == 600) && "ca".equals(locale.getLanguage())) {
										BufferedImage narrow = new BufferedImage(width, 700,
												BufferedImage.TYPE_INT_RGB);
										java.awt.Graphics2D graphics = narrow.createGraphics();
										try {
											panel.paint(graphics);
										} finally {
											graphics.dispose();
										}
										ImageIO.write(narrow, "png", new File(
												"build/receiving-" + locale.getLanguage() + "-" + width + ".png"));
									}
									int scanWidth = tools.getWidth();
									field(panel, "reviewing", true);
									refresh.invoke(panel);
									layout(panel);
									assertHeadingFits(panel, locale, width);
									assertEquals(7, ((JTable) field(panel, "table")).getColumnCount());
									assertEquals(width == 320, tableScroll.getHorizontalScrollBar().isVisible(),
											locale + " width=" + width + " review horizontal scrollbar");
									assertEquals(scanWidth, tools.getWidth(),
											locale + " width=" + width + " review tools changed width");
									if (width == 320 && "ca".equals(locale.getLanguage())) {
										BufferedImage compactReview = new BufferedImage(width, 700,
												BufferedImage.TYPE_INT_RGB);
										java.awt.Graphics2D graphics = compactReview.createGraphics();
										try {
											panel.paint(graphics);
										} finally {
											graphics.dispose();
										}
										ImageIO.write(compactReview, "png",
												new File("build/receiving-ca-review-320.png"));
									}
									field(panel, "reviewing", false);
									refresh.invoke(panel);
									layout(panel);
								}
								panel.setSize(980, 700);
								layout(panel);
								JTable table = (JTable) field(panel, "table");
								TableModel model = table.getModel();
								int knownRow = model.getValueAt(0, 2).toString().startsWith("123") ? 0 : 1;
								assertEquals("123", model.getValueAt(knownRow, 2));
								assertTrue(!"—".equals(model.getValueAt(knownRow, 3)));
								table.setRowSelectionInterval(1 - knownRow, 1 - knownRow);
								assertEquals("8437008291043",
										((javax.swing.JLabel) field(panel, "detailBarcode")).getText());
								assertEquals("—", ((javax.swing.JTextArea) field(panel, "detailReference")).getText());
								assertTrue(!((JPanel) field(panel, "brandRow")).isVisible());
								layout(panel);
								javax.swing.JLabel barcode = (javax.swing.JLabel) field(panel, "detailBarcode");
								assertTrue(barcode.getPreferredSize().width <= barcode.getWidth(),
										locale + " barcode clipped in product details");
								if ("ca".equals(locale.getLanguage())) {
									BufferedImage unknownImage = new BufferedImage(980, 700,
											BufferedImage.TYPE_INT_RGB);
									java.awt.Graphics2D graphics = unknownImage.createGraphics();
									try {
										panel.paint(graphics);
									} finally {
										graphics.dispose();
									}
									ImageIO.write(unknownImage, "png", new File("build/receiving-ca-unknown.png"));
								}
								table.setRowSelectionInterval(knownRow, knownRow);
								assertEquals("123", ((javax.swing.JLabel) field(panel, "detailBarcode")).getText());
								assertEquals("00001-100 TIERRA/ARENA",
										((javax.swing.JTextArea) field(panel, "detailReference")).getText());
								assertEquals("Freixenet", ((javax.swing.JLabel) field(panel, "detailBrand")).getText());
								assertTrue(((JPanel) field(panel, "brandRow")).isVisible());
								assertTrue(((JPanel) field(panel, "priceRow")).isVisible());
								assertTrue(!((javax.swing.JLabel) field(panel, "detailPrice")).getText().isBlank());
								assertEquals("Cava Freixenet Cordón Negro 75 cl",
										((javax.swing.JTextArea) field(panel, "selectedName")).getText());
								JButton remove = (JButton) field(panel, "removeAction");
								assertEquals(AppLocal.getIntString("receiving.remove"), remove.getText());
								JButton tick = (JButton) field(panel, "tickAction");
								assertTrue(tick.isVisible());
								layout(panel);
								assertEquals(((JButton) field(panel, "searchAction")).getHeight(), tick.getHeight());
								assertEquals(tick.getHeight(), remove.getHeight());
								assertTrue(SwingUtilities.convertPoint(remove, 0, 0, panel).y > SwingUtilities
										.convertPoint(tick, 0, 0, panel).y);
								JScrollPane tools = (JScrollPane) field(panel, "tools");
								int bottom = SwingUtilities.convertPoint(remove, 0, remove.getHeight(),
										tools.getViewport()).y;
								if (bottom > tools.getViewport().getHeight()) {
									assertTrue(tools.getVerticalScrollBar().isVisible(),
											locale + " remove action is below the viewport with no way to scroll");
									int previous = tools.getVerticalScrollBar().getValue();
									tools.getVerticalScrollBar().setValue(tools.getVerticalScrollBar().getMaximum());
									layout(panel);
									assertTrue(
											SwingUtilities.convertPoint(remove, 0, remove.getHeight(),
													tools.getViewport()).y <= tools.getViewport().getHeight(),
											locale + " remove action cannot be reached by scrolling");
									tools.getVerticalScrollBar().setValue(previous);
									layout(panel);
								}
								JPanel card = (JPanel) field(panel, "detailCard");
								javax.swing.JLabel price = (javax.swing.JLabel) field(panel, "detailPrice");
								javax.swing.JLabel brand = (javax.swing.JLabel) field(panel, "detailBrand");
								javax.swing.JTextArea reference = (javax.swing.JTextArea) field(panel,
										"detailReference");
								assertTrue(SwingUtilities.convertPoint(reference, 0, 0, card).y
										- SwingUtilities.convertPoint(
												(javax.swing.JLabel) field(panel, "detailBarcode"), 0, 0, card).y < 40);
								assertTrue(SwingUtilities.convertPoint(price, 0, 0, card).y
										- SwingUtilities.convertPoint(brand, 0, 0, card).y < 40);
								assertEquals(AppLocal.getIntString("receiving.checkLine"), tick.getText());
								tick.doClick();
								assertEquals(true, model.getValueAt(knownRow, 0));
								assertEquals(AppLocal.getIntString("receiving.uncheckLine"), tick.getText());
								tick.doClick();
								assertEquals(false, model.getValueAt(knownRow, 0));
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
								model.setValueAt("7", knownRow, 4);
								assertEquals("7", model.getValueAt(knownRow, 4));
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
								assertEquals("7", model.getValueAt(table.getSelectedRow(), 4));
								assertTrue(SwingUtilities.isDescendingFrom((JComboBox<?>) field(panel, "sort"),
										(JPanel) field(panel, "list")));
								javax.swing.JTextField code = (javax.swing.JTextField) field(panel, "code");
								code.setText("123");
								code.postActionEvent();
								assertEquals("", code.getText());
								assertTrue(table.getSelectedRow() >= 0);
								assertEquals("123", model.getValueAt(table.getSelectedRow(), 2));
								assertEquals("8", model.getValueAt(table.getSelectedRow(), 4));
								table.setRowSelectionInterval(1 - table.getSelectedRow(), 1 - table.getSelectedRow());
								ProductInfoExt chosen = new ProductInfoExt();
								chosen.setID("p1");
								chosen.setCode("123");
								panel.addFoundProduct(chosen);
								assertEquals("123", model.getValueAt(table.getSelectedRow(), 2));
								assertEquals("9", model.getValueAt(table.getSelectedRow(), 4));
								code.setText("987654321");
								code.postActionEvent();
								assertEquals("987654321", model.getValueAt(table.getSelectedRow(), 2));
								assertEquals("987654321",
										((javax.swing.JLabel) field(panel, "detailBarcode")).getText());
								StockSessionRepository.Line scannedUnknown = repo.lines(session.id, false).stream()
										.filter(line -> "987654321".equals(line.code)).findFirst().orElseThrow();
								repo.remove(session.id, scannedUnknown.id);
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

	private static void assertHeadingFits(StockReceivingPanel panel, Locale locale, int width) throws Exception {
		JPanel titleBlock = (JPanel) field(panel, "titleBlock");
		for (String name : new String[]{"heading", "noteHeading", "pendingBadge", "startedHint", "stockHint"}) {
			javax.swing.JLabel label = (javax.swing.JLabel) field(panel, name);
			int right = SwingUtilities.convertPoint(label, label.getWidth(), 0, titleBlock).x;
			assertTrue(right <= titleBlock.getWidth(),
					locale + " width=" + width + " clipped " + name + ": " + right + " > " + titleBlock.getWidth());
		}
	}

	private static void assertDiscardLabelFits(JButton button, Locale locale, int width) {
		int textWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
		assertTrue(button.getWidth() - button.getInsets().left - button.getInsets().right >= textWidth,
				locale + " width=" + width + " truncated discard button: " + button.getBounds() + " textWidth="
						+ textWidth + " insets=" + button.getInsets());
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
