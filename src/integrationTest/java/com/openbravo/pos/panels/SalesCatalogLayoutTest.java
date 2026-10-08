package com.openbravo.pos.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.pos.catalog.JCatalog;
import com.openbravo.pos.catalog.JCatalogTab;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.sales.JPanelTicket;
import com.openbravo.pos.sales.JPanelTicketSales;
import com.openbravo.pos.ticket.CategoryInfo;
import com.openbravo.data.loader.Session;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SalesCatalogLayoutTest {
	@Test
	void subcategoryQueriesPreserveParentIdsForFullBreadcrumbs() throws Exception {
		Session session = new Session("jdbc:derby:memory:catalogPath" + UUID.randomUUID() + ";create=true", null, null);
		try {
			try (Statement sql = session.getConnection().createStatement()) {
				sql.execute(
						"CREATE TABLE CATEGORIES (ID VARCHAR(64), NAME VARCHAR(64), PARENTID VARCHAR(64), IMAGE BLOB)");
				sql.execute("INSERT INTO CATEGORIES VALUES ('root', 'Women', NULL, NULL)");
				sql.execute("INSERT INTO CATEGORIES VALUES ('middle', 'Clothing', 'root', NULL)");
				sql.execute("INSERT INTO CATEGORIES VALUES ('leaf', 'Classic', 'middle', NULL)");
			}
			DataLogicSales sales = new DataLogicSales();
			sales.init(session);
			assertEquals("root", sales.getSubcategories("root").get(0).getParentID());
			assertEquals("middle", sales.getSubcategories("middle").get(0).getParentID());
		} finally {
			session.close();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void nestedCategoryBreadcrumbShowsEveryAncestorAndNavigatesBackToEachLevel() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			try {
				JCatalog catalog = new JCatalog(null);
				CategoryInfo root = new CategoryInfo("root", "Women", null);
				CategoryInfo middle = new CategoryInfo("middle", "Clothing", "root", null);
				CategoryInfo leaf = new CategoryInfo("leaf", "Classic", "middle", null);
				Map<String, CategoryInfo> categories = (Map<String, CategoryInfo>) catalogField(catalog,
						"categoriesById");
				for (CategoryInfo category : new CategoryInfo[]{root, middle, leaf}) {
					categories.put(category.getID(), category);
					((Set<String>) catalogField(catalog, "m_categoriesset")).add(category.getID());
					((JPanel) catalogField(catalog, "m_jProducts")).add(new JPanel(), category.getID());
				}
				JList<CategoryInfo> roots = (JList<CategoryInfo>) catalogField(catalog, "m_jListCategories");
				DefaultListModel<CategoryInfo> model = new DefaultListModel<CategoryInfo>();
				model.addElement(root);
				roots.setModel(model);
				roots.setSelectedIndex(0);
				Method open = JCatalog.class.getDeclaredMethod("showSubcategoryPanel", CategoryInfo.class);
				open.setAccessible(true);
				open.invoke(catalog, leaf);
				JPanel trail = (JPanel) catalogField(catalog, "breadcrumbs");
				Component header = ((BorderLayout) catalog.getLayout()).getLayoutComponent(BorderLayout.NORTH);
				assertTrue(header.isVisible());
				assertEquals("Women", ((JButton) trail.getComponent(0)).getText());
				assertEquals("Clothing", ((JButton) trail.getComponent(2)).getText());
				assertEquals("Classic", ((JLabel) trail.getComponent(4)).getText());
				Method detail = JCatalog.class.getDeclaredMethod("showBreadcrumb", CategoryInfo.class, String.class);
				detail.setAccessible(true);
				detail.invoke(catalog, leaf, "Blue shirt");
				assertEquals("Blue shirt", ((JLabel) trail.getComponent(6)).getText());
				((JButton) trail.getComponent(2)).doClick();
				assertEquals(3, trail.getComponentCount());
				assertEquals("Clothing", ((JLabel) trail.getComponent(2)).getText());
				((JButton) trail.getComponent(0)).doClick();
				assertEquals(1, trail.getComponentCount());
				assertEquals("Women", ((JLabel) trail.getComponent(0)).getText());
				assertTrue(((JPanel) catalogField(catalog, "m_jCategories")).isVisible());
				assertFalse(header.isVisible());
				catalog.setSize(900, 312);
				catalog.doLayout();
				assertEquals(0, ((BorderLayout) catalog.getLayout()).getLayoutComponent(BorderLayout.CENTER).getY());
				assertEquals(312,
						((BorderLayout) catalog.getLayout()).getLayoutComponent(BorderLayout.CENTER).getHeight());
				detail.invoke(catalog, root, "Blue shirt");
				assertTrue(header.isVisible());
			} catch (Exception ex) {
				throw new AssertionError(ex);
			}
		});
	}

	private Object catalogField(JCatalog target, String name) throws Exception {
		Field field = JCatalog.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	@Test
	void salesSummaryKeepsACountBesideItsLabelAndAllAmountsOnOneBaseline() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					try {
						JPanelTicketSales sales = new JPanelTicketSales();
						JPanel summary = (JPanel) field(sales, "m_jPanTotals");
						JLabel countLabel = (JLabel) field(sales, "m_jLblProductCount");
						JLabel count = (JLabel) field(sales, "m_jProductCount");
						JLabel taxes = (JLabel) field(sales, "m_jTaxesEuros");
						JLabel subtotal = (JLabel) field(sales, "m_jSubtotalEuros");
						JLabel total = (JLabel) field(sales, "m_jTotalEuros");
						taxes.setText("€ 5,03");
						subtotal.setText("€ 23,97");
						total.setText("€ 29,00");
						summary.setSize(950, summary.getPreferredSize().height);
						summary.doLayout();
						assertEquals(AppLocal.getIntString("label.sales.products") + ":", countLabel.getText());
						assertTrue(count.getX() - (countLabel.getX() + countLabel.getWidth()) <= 4);
						int baseline = count.getY() + count.getBaseline(count.getWidth(), count.getHeight());
						for (JLabel value : new JLabel[]{taxes, subtotal, total}) {
							assertEquals(baseline,
									value.getY() + value.getBaseline(value.getWidth(), value.getHeight()));
						}
						int x = total.getX();
						total.setText("€ 123,45");
						summary.doLayout();
						assertEquals(x, total.getX());
					} catch (Exception ex) {
						throw new AssertionError(ex);
					}
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}

	private Object field(Object target, String name) throws Exception {
		Field field = JPanelTicket.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	@Test
	void compactSubcategoryUsesFullWidthAndLongProductNamesStayInsideTouchTiles() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					try {
						JCatalog catalog = new JCatalog(null);
						Method open = JCatalog.class.getDeclaredMethod("selectIndicatorPanel", String.class);
						open.setAccessible(true);
						open.invoke(catalog, "Clàssics");
						catalog.setSize(900, 312);
						catalog.doLayout();
						BorderLayout layout = (BorderLayout) catalog.getLayout();
						assertFalse(layout.getLayoutComponent(BorderLayout.LINE_START).isVisible());
						Component header = layout.getLayoutComponent(BorderLayout.NORTH);
						assertEquals(900, header.getWidth());
						assertTrue(layout.getLayoutComponent(BorderLayout.CENTER).getHeight() >= 245);
						assertTrue(header.getHeight() >= 48);

						JCatalogTab tab = new JCatalogTab();
						String name = "MUJER — AZUL / 3XL [L766231] MODELO LARGO ".repeat(5);
						tab.addButton(new String[]{name, "29,00 €"}, null, 144, 100, false, e -> {
						});
						JScrollPane scroll = (JScrollPane) tab.getComponent(0);
						Container tiles = (Container) scroll.getViewport().getView();
						JButton tile = (JButton) tiles.getComponent(0);
						assertTrue(tile.getPreferredSize().width >= 144);
						assertTrue(tile.getPreferredSize().height >= 100);
						assertTrue(tile.getText().contains("29,00 €"));
						assertTrue(tile.getText().contains("…"));
						assertTrue(tile.getToolTipText().contains(name));
						tab.addButton(new String[]{name, "29,00 €"},
								new ImageIcon(new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB)), 144, 100, false,
								e -> {
								});
						JButton imageTile = (JButton) tiles.getComponent(1);
						assertTrue(imageTile.getText().contains("29,00 €"));
						assertEquals(tile.getPreferredSize(), imageTile.getPreferredSize());
					} catch (Exception ex) {
						throw new AssertionError(ex);
					}
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}
}
