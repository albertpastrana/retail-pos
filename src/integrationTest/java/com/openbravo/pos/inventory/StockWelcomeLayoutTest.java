package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.HashMap;
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
					JPanel entries = (JPanel) page.getComponent(6);
					assertEquals(1, jobs.getComponentCount());
					assertEquals(1, entries.getComponentCount());
					for (Component component : new Component[]{jobs.getComponent(0), entries.getComponent(0)}) {
						JButton button = (JButton) component;
						assertTrue(button.getPreferredSize().width <= button.getWidth(),
								locale + ": " + button.getText());
						assertTrue(button.getHeight() >= 48);
					}
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
					view.setSize(760, 700);
					layout(view);
					JPanel page = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
					JPanel jobs = (JPanel) page.getComponent(2);
					JPanel entries = (JPanel) page.getComponent(6);
					assertEquals(6, jobs.getComponentCount());
					assertEquals(6, entries.getComponentCount());
					for (JPanel grid : new JPanel[]{jobs, entries}) {
						for (Component component : grid.getComponents()) {
							JButton button = (JButton) component;
							assertTrue(button.getPreferredSize().width <= button.getWidth(),
									locale + ": " + button.getText() + " " + button.getWidth());
							assertTrue(button.getHeight() >= 48);
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
	void lightAndDarkThemesPaintAtMinimumWindowSize() throws Exception {
		RetailPOSTheme.registerDefaultsSource();
		try {
			for (boolean dark : new boolean[]{false, true}) {
				UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
				SwingUtilities.invokeAndWait(() -> {
					AppUserView user = (AppUserView) Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class[]{AppUserView.class}, (proxy, method, args) -> null);
					AppView app = (AppView) Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class[]{AppView.class},
							(proxy, method, args) -> "getAppUserView".equals(method.getName()) ? user : null);
					JPanelStockWelcome view = new JPanelStockWelcome(app, new MenuDefinition("Menu.StockManagement"));
					view.getComponent();
					view.setSize(760, 700);
					layout(view);
					assertEquals(RetailPOSColors.surface0(), view.getBackground());
					JPanel page = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
					assertEquals(RetailPOSColors.surface200(), page.getComponent(0).getBackground());
					BufferedImage image = new BufferedImage(760, 700, BufferedImage.TYPE_INT_RGB);
					java.awt.Graphics2D graphics = image.createGraphics();
					try {
						view.paint(graphics);
					} finally {
						graphics.dispose();
					}
					assertEquals(RetailPOSColors.surface0().getRGB(), image.getRGB(3, 3));
				});
			}
		} finally {
			UIManager.setLookAndFeel(new FlatLightLaf());
		}
	}
}
