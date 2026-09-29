package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppUserView;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.MenuDefinition;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class StockWelcomeLayoutTest {
	@Test
	void permittedEntriesAndJobsFitAtMinimumWidthInThreeLanguages() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
				AppLocal.setLocale(locale);
				assertEquals(switch (locale.getLanguage()) {
					case "es" -> "Catálogo y precios";
					case "ca" -> "Catàleg i preus";
					default -> "Catalogue and prices";
				}, AppLocal.getIntString("Menu.StockManagement"));
				SwingUtilities.invokeAndWait(() -> {
					String products = "com.openbravo.pos.inventory.ProductsPanel";
					String taxes = "com.openbravo.pos.inventory.TaxPanel";
					Action productAction = new AbstractAction(AppLocal.getIntString("Menu.Products")) {
						@Override
						public void actionPerformed(ActionEvent e) {
						}
					};
					productAction.putValue(AppUserView.ACTION_TASKNAME, products);
					Action taxAction = new AbstractAction(AppLocal.getIntString("Menu.Taxes")) {
						@Override
						public void actionPerformed(ActionEvent e) {
						}
					};
					taxAction.putValue(AppUserView.ACTION_TASKNAME, taxes);
					MenuDefinition menu = new MenuDefinition("Menu.StockManagement");
					menu.addMenuItem(productAction);
					menu.addMenuItem(taxAction);
					AppUserView user = (AppUserView) Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class[]{AppUserView.class},
							(proxy, method, args) -> "getTaskAction".equals(method.getName())
									&& products.equals(args[0]) ? productAction : null);
					AppView app = (AppView) Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class[]{AppView.class},
							(proxy, method, args) -> "getAppUserView".equals(method.getName()) ? user : null);
					JPanelStockWelcome view = new JPanelStockWelcome(app, menu);
					view.getComponent();
					view.setSize(760, 700); // 1024px window minus full sidebar and its border
					layout(view);
					JPanel header = (JPanel) view.getComponent(0);
					assertTrue(header.getComponent(1) instanceof JButton);
					assertTrue(header.getComponent(1).getX() + header.getComponent(1).getWidth() <= header.getWidth());
					JPanel page = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
					JPanel jobs = (JPanel) page.getComponent(2);
					JPanel entries = (JPanel) page.getComponent(7);
					JButton firstJob = (JButton) jobs.getComponent(0);
					JPanel words = (JPanel) firstJob.getComponent(1);
					assertTrue(words.getComponent(1).getHeight() > 0, "Hint: card=" + firstJob.getBounds() + " words="
							+ words.getBounds() + " hint=" + words.getComponent(1).getBounds());
					assertTrue(words.getComponent(1).getY() + words.getComponent(1).getHeight() <= words.getHeight(),
							"Hint clipped: words=" + words.getBounds() + " hint=" + words.getComponent(1).getBounds());
					assertEquals(0, jobs.getX(), locale.toString());
					assertEquals(0, entries.getX(), locale.toString());
					assertEquals(page.getWidth(), jobs.getWidth(), locale.toString());
					assertEquals(page.getWidth(), entries.getWidth(), locale.toString());
					assertEquals(1, jobs.getComponentCount());
					assertEquals(1, entries.getComponentCount());
					for (Component component : new Component[]{jobs.getComponent(0), entries.getComponent(0)}) {
						JButton button = (JButton) component;
						assertTrue(button.getWidth() > 0, locale.toString());
						assertTrue(button.getHeight() >= 48);
					}
					StockWelcomeRepository.Product product = new StockWelcomeRepository.Product();
					product.name = "Milk";
					product.code = "12345678";
					product.reference = "MILK";
					product.category = "Dairy";
					product.tax = "Reduced";
					product.taxRate = 0.10;
					view.renderMatches(product.code, List.of(product), false);
					layout(view);
					JPanel facets = (JPanel) ((JPanel) ((JPanel) page.getComponent(4)).getComponent(0)).getComponent(1);
					assertEquals(5, facets.getComponentCount());
					assertTrue(facets.getComponent(0) instanceof JButton);
					for (int i = 1; i < 5; i++)
						assertFalse(facets.getComponent(i) instanceof JButton, locale + ": inaccessible action " + i);
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}

	@Test
	void sixExistingEntriesStayVisibleAndTouchSizedInEachLocale() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					MenuDefinition menu = new MenuDefinition("Menu.StockManagement");
					Map<String, Action> actions = new HashMap<>();
					String[] names = {"Products", "PriceRules", "SaleMark", "Categories", "Taxes", "StockDiary"};
					String[] classes = {"ProductsPanel", "PriceRulesPanel", "SaleMarkPanel", "CategoriesPanel",
							"TaxPanel", "StockDiaryPanel"};
					for (int i = 0; i < names.length; i++) {
						String task = "com.openbravo.pos.inventory." + classes[i];
						Action action = new AbstractAction(AppLocal.getIntString("Menu." + names[i])) {
							@Override
							public void actionPerformed(ActionEvent e) {
							}
						};
						action.putValue(AppUserView.ACTION_TASKNAME, task);
						actions.put(task, action);
						menu.addMenuItem(action);
					}
					AppUserView user = (AppUserView) Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class[]{AppUserView.class}, (proxy, method,
									args) -> "getTaskAction".equals(method.getName()) ? actions.get(args[0]) : null);
					AppView app = (AppView) Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class[]{AppView.class},
							(proxy, method, args) -> "getAppUserView".equals(method.getName()) ? user : null);
					JPanelStockWelcome view = new JPanelStockWelcome(app, menu);
					view.getComponent();
					StockWelcomeRepository.Queue narrowQueue = new StockWelcomeRepository.Queue();
					narrowQueue.pending = 7;
					narrowQueue.ordered = 4;
					narrowQueue.customers = 3;
					view.renderQueue(narrowQueue);
					view.setSize(760, 700);
					layout(view);
					JPanel page = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
					JPanel jobs = (JPanel) page.getComponent(2);
					JPanel entries = (JPanel) page.getComponent(7);
					assertEquals(0, jobs.getX(), locale.toString());
					assertEquals(0, entries.getX(), locale.toString());
					assertEquals(page.getWidth(), jobs.getWidth(), locale.toString());
					assertEquals(page.getWidth(), entries.getWidth(), locale.toString());
					assertEquals(5, jobs.getComponentCount());
					assertEquals(6, entries.getComponentCount());
					JPanel queue = (JPanel) page.getComponent(5);
					JPanel queueCard = (JPanel) queue.getComponent(1);
					JPanel metrics = (JPanel) queueCard.getComponent(0);
					for (Component component : metrics.getComponents()) {
						JPanel details = (JPanel) ((JButton) component).getComponent(1);
						Component hint = details.getComponent(1);
						assertTrue(hint.getHeight() > 0 && hint.getY() + hint.getHeight() <= details.getHeight(),
								locale + ": queue hint=" + hint.getBounds() + " parent=" + details.getBounds());
					}
					for (JPanel grid : new JPanel[]{jobs, entries}) {
						for (Component component : grid.getComponents()) {
							JButton button = (JButton) component;
							assertTrue(button.getWidth() > 0, locale.toString());
							assertTrue(button.getHeight() >= 48);
							if (grid == jobs) {
								JPanel words = (JPanel) button.getComponent(1);
								for (Component line : words.getComponents()) {
									assertTrue(
											line.getHeight() > 0 && line.getY() + line.getHeight() <= words.getHeight(),
											locale + ": " + ((javax.swing.JTextArea) line).getText());
								}
							} else {
								javax.swing.JLabel caption = (javax.swing.JLabel) button.getComponent(0);
								assertTrue(caption.getPreferredSize().width <= caption.getWidth(),
										locale + ": " + caption.getText());
							}
						}
					}
					if ("ca".equals(locale.getLanguage())) {
						BufferedImage narrow = new BufferedImage(760, 700, BufferedImage.TYPE_INT_RGB);
						java.awt.Graphics2D narrowGraphics = narrow.createGraphics();
						try {
							view.paint(narrowGraphics);
						} finally {
							narrowGraphics.dispose();
						}
						try {
							ImageIO.write(narrow, "png", new File("build/stock-welcome-ca-narrow.png"));
						} catch (java.io.IOException e) {
							throw new AssertionError(e);
						}
						JPanelStockWelcome wide = new JPanelStockWelcome(app, menu);
						wide.getComponent();
						StockWelcomeRepository.Queue waiting = new StockWelcomeRepository.Queue();
						waiting.pending = 7;
						waiting.ordered = 4;
						waiting.customers = 3;
						wide.renderQueue(waiting);
						wide.setSize(1800, 1050);
						layout(wide);
						JPanel widePage = (JPanel) ((JScrollPane) wide.getComponent(1)).getViewport().getView();
						assertEquals(3,
								((java.awt.GridLayout) ((JPanel) widePage.getComponent(2)).getLayout()).getColumns());
						assertEquals(6,
								((java.awt.GridLayout) ((JPanel) widePage.getComponent(7)).getLayout()).getColumns());
						BufferedImage image = new BufferedImage(1800, 1050, BufferedImage.TYPE_INT_RGB);
						java.awt.Graphics2D graphics = image.createGraphics();
						try {
							wide.paint(graphics);
						} finally {
							graphics.dispose();
						}
						Component tile = ((JPanel) widePage.getComponent(2)).getComponent(0);
						Point corner = SwingUtilities.convertPoint(tile, 0, 0, wide);
						assertEquals(RetailPOSColors.surface0().getRGB(), image.getRGB(corner.x, corner.y),
								"Job card corner must stay round");
						assertEquals(RetailPOSColors.surface100().getRGB(), image.getRGB(corner.x + 12, corner.y + 12),
								"Job card must use the raised surface");
						try {
							ImageIO.write(image, "png", new File("build/stock-welcome-wide.png"));
						} catch (java.io.IOException e) {
							throw new AssertionError(e);
						}
					}
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}

	private static void layout(Component component) {
		component.doLayout();
		if (component instanceof JScrollPane) {
			layout(((JScrollPane) component).getViewport());
		} else if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents())
				layout(child);
		}
	}

	@Test
	void searchResultAndUnknownCodeStayAlignedWithTheSearchField() throws Exception {
		Locale original = Locale.getDefault();
		try {
			AppLocal.setLocale(new Locale("ca", "ES"));
			SwingUtilities.invokeAndWait(() -> {
				MenuDefinition menu = new MenuDefinition("Menu.StockManagement");
				Map<String, Action> actions = new HashMap<>();
				for (String className : new String[]{"ProductsPanel", "PriceRulesPanel", "SaleMarkPanel",
						"CategoriesPanel", "TaxPanel", "StockDiaryPanel", "ReplenishmentPanel"}) {
					String task = "com.openbravo.pos.inventory." + className;
					String label = switch (className) {
						case "ProductsPanel" -> "Menu.Products";
						case "PriceRulesPanel" -> "Menu.PriceRules";
						case "SaleMarkPanel" -> "Menu.SaleMark";
						case "CategoriesPanel" -> "Menu.Categories";
						case "TaxPanel" -> "Menu.Taxes";
						case "StockDiaryPanel" -> "Menu.StockDiary";
						default -> "Menu.Replenishment";
					};
					Action destination = new AbstractAction(AppLocal.getIntString(label)) {
						@Override
						public void actionPerformed(ActionEvent event) {
						}
					};
					destination.putValue(AppUserView.ACTION_TASKNAME, task);
					if (!"ReplenishmentPanel".equals(className))
						menu.addMenuItem(destination);
					actions.put(task, destination);
				}
				AppUserView user = (AppUserView) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class[]{AppUserView.class}, (proxy, method,
								args) -> "getTaskAction".equals(method.getName()) ? actions.get(args[0]) : null);
				AppView app = (AppView) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AppView.class},
						(proxy, method, args) -> "getAppUserView".equals(method.getName()) ? user : null);
				JPanelStockWelcome view = new JPanelStockWelcome(app, menu);
				view.getComponent();
				StockWelcomeRepository.Product product = new StockWelcomeRepository.Product();
				product.name = "Cafè en gra torrefacte 1 kg";
				product.code = "8412345678905";
				product.reference = "1188";
				product.price = 10.53;
				product.salePercent = 15;
				product.category = "Esmorzars";
				product.categoryParent = "Alimentació";
				product.tax = "Reduït";
				product.taxRate = 0.10;
				product.units = 12;
				product.lastMovement = java.sql.Timestamp.valueOf("2026-09-24 19:04:00");
				product.brand = "Boncafè";
				product.ruleMarkup = 38.0;
				product.ruleRounding = "ALWAYS_95";
				product.status = "PENDING";
				product.replenishmentCreated = java.sql.Timestamp.valueOf("2026-09-22 11:00:00");
				view.renderMatches(product.code, List.of(product), false);
				view.setSize(760, 700);
				layout(view);
				JPanel page = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
				JPanel results = (JPanel) page.getComponent(4);
				assertEquals(page.getWidth(), results.getWidth());
				assertEquals(0, results.getX());
				assertTrue(results.getComponent(0).getHeight() >= 48);
				JPanel resultCard = (JPanel) results.getComponent(0);
				JPanel facets = (JPanel) resultCard.getComponent(1);
				assertEquals(5, facets.getComponentCount());
				assertEquals(2, ((java.awt.GridLayout) facets.getLayout()).getColumns());
				assertCompactFacetsFit(facets);
				assertEquals(java.text.NumberFormat.getCurrencyInstance().format(product.price * 0.85),
						((javax.swing.JTextArea) ((JButton) facets.getComponent(0)).getComponent(1)).getText());
				assertEquals(java.text.NumberFormat.getPercentInstance().format(0.10),
						((javax.swing.JTextArea) ((JButton) facets.getComponent(3)).getComponent(1)).getText());
				assertEquals(4, resultCard.getComponentCount()); // header, facets, open request, brand rule
				writePreview(view, 760, 700, "build/stock-welcome-product.png");
				view.setSize(1800, 1000);
				view.renderMatches(product.code, List.of(product), false);
				layout(view);
				resultCard = (JPanel) results.getComponent(0);
				facets = (JPanel) resultCard.getComponent(1);
				assertEquals(5, ((java.awt.GridLayout) facets.getLayout()).getColumns());
				assertCompactFacetsFit(facets);
				assertTrue(facets.getComponent(0).getHeight() <= 190,
						"Wide facet oversized: tile=" + facets.getComponent(0).getBounds() + " tile preferred="
								+ facets.getComponent(0).getPreferredSize() + " grid=" + facets.getBounds()
								+ " grid preferred=" + facets.getPreferredSize() + " card=" + resultCard.getBounds());
				writePreview(view, 1800, 1000, "build/stock-welcome-product-wide.png");
				StockWelcomeRepository.Product bag = new StockWelcomeRepository.Product();
				bag.name = "Bossa gran";
				bag.reference = "2000000000039";
				bag.code = "2000000000039";
				bag.price = 0.12;
				bag.category = "Bosses";
				bag.tax = "IVA 21%";
				bag.taxRate = 0.21;
				bag.units = -2;
				bag.lastMovement = java.sql.Timestamp.valueOf("2026-09-24 19:04:00");
				view.setSize(1800, 900);
				view.renderMatches(bag.code, List.of(bag), false);
				layout(view);
				JPanel bagFacets = (JPanel) ((JPanel) results.getComponent(0)).getComponent(1);
				assertCompactFacetsFit(bagFacets);
				assertEquals("Sense rebaixa",
						((javax.swing.JTextArea) ((JButton) bagFacets.getComponent(1)).getComponent(1)).getText());
				assertEquals(java.text.NumberFormat.getPercentInstance().format(0.21),
						((javax.swing.JTextArea) ((JButton) bagFacets.getComponent(3)).getComponent(1)).getText());
				assertEquals(2, ((JPanel) results.getComponent(0)).getComponentCount());
				writePreview(view, 1800, 900, "build/stock-welcome-product-negative.png");
				view.setSize(760, 700);
				view.renderMatches("9876543210000", List.of(), false);
				layout(view);
				assertEquals(page.getWidth(), results.getWidth());
				writePreview(view, 760, 700, "build/stock-welcome-unknown.png");
			});
		} finally {
			AppLocal.setLocale(original);
		}
	}

	private static void writePreview(Component view, int width, int height, String path) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D graphics = image.createGraphics();
		try {
			view.paint(graphics);
		} finally {
			graphics.dispose();
		}
		try {
			ImageIO.write(image, "png", new File(path));
		} catch (java.io.IOException e) {
			throw new AssertionError(e);
		}
	}

	private static void assertCompactFacetsFit(JPanel facets) {
		for (Component component : facets.getComponents()) {
			assertTrue(component.getHeight() >= 48 && component.getHeight() <= 132,
					"Fact tile height: " + component.getBounds());
			JButton tile = (JButton) component;
			for (Component child : new Component[]{tile.getComponent(0), tile.getComponent(1),
					tile.getComponent(tile.getComponentCount() - 1)}) {
				assertTrue(child.getHeight() > 0 && child.getY() + child.getHeight() <= tile.getHeight(),
						"Clipped fact: " + child.getBounds() + " in " + tile.getBounds());
			}
		}
	}

	@Test
	void lightAndDarkThemesPaintAtMinimumWindowSize() throws Exception {
		RetailPOSTheme.registerDefaultsSource();
		try {
			for (boolean dark : new boolean[]{false, true}) {
				UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
				SwingUtilities.invokeAndWait(() -> {
					MenuDefinition menu = new MenuDefinition("Menu.StockManagement");
					Map<String, Action> actions = new HashMap<>();
					String[] classes = {"ProductsPanel", "PriceRulesPanel", "SaleMarkPanel", "CategoriesPanel",
							"TaxPanel", "StockDiaryPanel"};
					for (String name : classes) {
						String task = "com.openbravo.pos.inventory." + name;
						Action action = new AbstractAction(name) {
							@Override
							public void actionPerformed(ActionEvent e) {
							}
						};
						action.putValue(AppUserView.ACTION_TASKNAME, task);
						menu.addMenuItem(action);
						actions.put(task, action);
					}
					AppUserView user = (AppUserView) Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class[]{AppUserView.class}, (proxy, method,
									args) -> "getTaskAction".equals(method.getName()) ? actions.get(args[0]) : null);
					AppView app = (AppView) Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class[]{AppView.class},
							(proxy, method, args) -> "getAppUserView".equals(method.getName()) ? user : null);
					JPanelStockWelcome view = new JPanelStockWelcome(app, menu);
					view.getComponent();
					StockWelcomeRepository.Queue waiting = new StockWelcomeRepository.Queue();
					waiting.pending = 7;
					waiting.ordered = 4;
					waiting.customers = 3;
					view.renderQueue(waiting);
					view.setSize(760, 700);
					layout(view);
					assertEquals(RetailPOSColors.surface0(), view.getBackground());
					JPanel page = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
					JPanel scanBlock = (JPanel) page.getComponent(0);
					assertEquals(RetailPOSColors.surface200(), scanBlock.getComponent(0).getBackground());
					BufferedImage image = new BufferedImage(760, 700, BufferedImage.TYPE_INT_RGB);
					java.awt.Graphics2D graphics = image.createGraphics();
					try {
						view.paint(graphics);
					} finally {
						graphics.dispose();
					}
					assertEquals(RetailPOSColors.surface0().getRGB(), image.getRGB(3, 3));
					JPanel jobs = (JPanel) page.getComponent(2);
					Component firstCard = jobs.getComponent(0);
					Point corner = SwingUtilities.convertPoint(firstCard, 0, 0, view);
					assertEquals(RetailPOSColors.surface0().getRGB(), image.getRGB(corner.x, corner.y));
					assertEquals(RetailPOSColors.surface100().getRGB(), image.getRGB(corner.x + 12, corner.y + 12));
					try {
						ImageIO.write(image, "png",
								new File("build/stock-welcome-" + (dark ? "dark" : "light") + "-narrow.png"));
					} catch (java.io.IOException e) {
						throw new AssertionError(e);
					}
					if (dark) {
						StockWelcomeRepository.Product product = new StockWelcomeRepository.Product();
						product.name = "Coffee";
						product.reference = "COF";
						product.code = "12345678";
						product.price = 10.53;
						product.salePercent = 15;
						product.category = "Food";
						product.tax = "Reduced";
						product.taxRate = 0.10;
						product.units = -2;
						view.setSize(1800, 900);
						view.renderMatches(product.code, List.of(product), false);
						layout(view);
						writePreview(view, 1800, 900, "build/stock-welcome-product-dark.png");
					}
				});
			}
		} finally {
			UIManager.setLookAndFeel(new FlatLightLaf());
		}
	}
}
