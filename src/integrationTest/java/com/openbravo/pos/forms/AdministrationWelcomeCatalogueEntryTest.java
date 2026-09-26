package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class AdministrationWelcomeCatalogueEntryTest {
	private static final String STOCK_WELCOME = "com.openbravo.pos.forms.MenuStockManagement";
	private static final String PRODUCTS = "com.openbravo.pos.inventory.ProductsPanel";

	@Test
	void commonCatalogueCardOpensStockWelcomeAndDirectProductEntryRemains() throws Exception {
		for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es", "ES"), new Locale("ca", "ES")})
			assertWelcomeRoutes(locale, true, true);
	}

	@Test
	void catalogueCardIsOmittedWhenStockWelcomePermissionIsMissing() throws Exception {
		for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es", "ES"), new Locale("ca", "ES")})
			assertWelcomeRoutes(locale, false, false);
	}

	private void assertWelcomeRoutes(Locale locale, boolean allowStockWelcome, boolean expectStockWelcome)
			throws Exception {
		Locale original = Locale.getDefault();
		try {
			AppLocal.setLocale(locale);
			SwingUtilities.invokeAndWait(() -> {
				AtomicInteger stockOpens = new AtomicInteger();
				Map<String, Action> actions = new HashMap<>();
				actions.put(STOCK_WELCOME,
						action(AppLocal.getIntString("Workflow.CataloguePrices"), STOCK_WELCOME, stockOpens));
				actions.put(PRODUCTS, action(AppLocal.getIntString("Menu.Products"), PRODUCTS, new AtomicInteger()));
				AppUser user = new AppUser("admin", "Alex Shopkeeper", "", "", AppUser.ROLE_ADMINISTRATOR, null);
				AppUserView userView = (AppUserView) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class[]{AppUserView.class}, (proxy, method, args) -> {
							if ("getTaskAction".equals(method.getName())) {
								if (STOCK_WELCOME.equals(args[0]) && !allowStockWelcome)
									return null;
								return actions.get(args[0]);
							}
							if ("getUser".equals(method.getName()))
								return user;
							return null;
						});
				AppView app = (AppView) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AppView.class},
						(proxy, method, args) -> "getAppUserView".equals(method.getName()) ? userView : null);
				JPanelWelcome welcome = new JPanelWelcome();
				try {
					welcome.init(app);
				} catch (BeanFactoryException e) {
					throw new AssertionError(e);
				}

				Set<String> labels = new HashSet<>();
				collectLabels(welcome, labels);
				assertEquals(expectStockWelcome, labels.contains(AppLocal.getIntString("Workflow.CataloguePrices")),
						locale.toString());
				assertTrue(labels.stream().anyMatch(label -> label.endsWith(AppLocal.getIntString("Menu.Products"))),
						"Direct product entry stays in the catalogue: " + labels);
				JButton stockButton = findTaskButton(welcome, STOCK_WELCOME);
				if (expectStockWelcome) {
					assertTrue(stockButton != null);
					stockButton.doClick();
					assertEquals(1, stockOpens.get());
				} else {
					assertFalse(stockButton != null);
					assertTrue(findTaskButton(welcome, PRODUCTS) != null);
				}
			});
		} finally {
			AppLocal.setLocale(original);
		}
	}

	private static Action action(String name, String task, AtomicInteger opens) {
		Action action = new AbstractAction(name) {
			@Override
			public void actionPerformed(ActionEvent event) {
				opens.incrementAndGet();
			}
		};
		action.putValue(AppUserView.ACTION_TASKNAME, task);
		return action;
	}

	private static void collectLabels(Component component, Set<String> labels) {
		if (component instanceof JLabel label && label.getText() != null)
			labels.add(label.getText());
		if (component instanceof JButton button && button.getText() != null)
			labels.add(button.getText());
		if (component instanceof Container container)
			for (Component child : container.getComponents())
				collectLabels(child, labels);
	}

	private static JButton findTaskButton(Component component, String task) {
		if (component instanceof JButton button && button.getAction() != null
				&& task.equals(button.getAction().getValue(AppUserView.ACTION_TASKNAME)))
			return button;
		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				JButton found = findTaskButton(child, task);
				if (found != null)
					return found;
			}
		}
		return null;
	}
}
