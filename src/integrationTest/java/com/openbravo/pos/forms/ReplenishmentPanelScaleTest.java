package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.util.UIScale;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.inventory.DataLogicReplenishment;
import com.openbravo.pos.inventory.ReplenishmentEntry;
import com.openbravo.pos.inventory.ReplenishmentPanel;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.UUID;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class ReplenishmentPanelScaleTest {
	@Test
	void buttonTextAndIconFitAfterScaling() throws Exception {
		String previousScale = System.getProperty("flatlaf.uiScale");
		Locale previousLocale = Locale.getDefault();
		javax.swing.LookAndFeel previousTheme = UIManager.getLookAndFeel();
		String url = "jdbc:derby:memory:replenishmentScale" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			DataLogicReplenishment data = new DataLogicReplenishment();
			data.init(session);
			ReplenishmentEntry entry = new ReplenishmentEntry();
			entry.manualDescription = "Samarreta interior de cotó";
			entry.createdBy = "2";
			data.add(entry);
			AppUser user = new AppUser("2", "Marta", null, null, "2", null);
			user.setAdministrationMode(true);
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
			System.setProperty("flatlaf.uiScale", "1.0");
			RetailPOSTheme.registerDefaultsSource();
			UIManager.setLookAndFeel(new FlatLightLaf());
			RetailPOSTheme.applyFonts();
			AppLocal.setLocale(new Locale("ca"));
			SwingUtilities.invokeAndWait(() -> {
				try {
					ReplenishmentPanel panel = new ReplenishmentPanel();
					panel.init(app);
					panel.activate();
					panel.setSize(980, 750);
					layout(panel);
					layout(panel);
					layout(panel);
					System.setProperty("flatlaf.uiScale", "2.0");
					java.lang.reflect.Method resetScale = UIScale.class.getDeclaredMethod("tests_uninitialize");
					resetScale.setAccessible(true);
					resetScale.invoke(null);
					UIManager.setLookAndFeel(new FlatDarkLaf());
					RetailPOSTheme.applyFonts();
					SwingUtilities.updateComponentTreeUI(panel);
					layout(panel);
					layout(panel);
					layout(panel);
					AbstractButton action = namedButton(panel, "replenishment.newRequest");
					assertTrue(action != null);
					int needed = action.getFontMetrics(action.getFont()).stringWidth(action.getText())
							+ action.getInsets().left + action.getInsets().right + action.getIcon().getIconWidth()
							+ action.getIconTextGap();
					assertEquals(2f, UIScale.getUserScaleFactor());
					assertTrue(action.getWidth() >= needed,
							"Scaled new-request action clips: " + action.getWidth() + " < " + needed);
				} catch (Exception failure) {
					throw new AssertionError(failure);
				}
			});
			for (String language : new String[]{"en", "es", "ca"})
				for (String scale : new String[]{"1.0", "1.25", "1.5", "2.0"}) {
					System.setProperty("flatlaf.uiScale", scale);
					java.lang.reflect.Method resetScale = UIScale.class.getDeclaredMethod("tests_uninitialize");
					resetScale.setAccessible(true);
					resetScale.invoke(null);
					UIManager.setLookAndFeel("2.0".equals(scale) ? new FlatDarkLaf() : new FlatLightLaf());
					RetailPOSTheme.applyFonts();
					AppLocal.setLocale(new Locale(language));
					SwingUtilities.invokeAndWait(() -> {
						try {
							ReplenishmentPanel panel = new ReplenishmentPanel();
							panel.init(app);
							panel.activate();
							for (int width : new int[]{980, 1200, 1440}) {
								panel.setSize(width, 750);
								layout(panel);
								layout(panel);
								AbstractButton action = namedButton(panel, "replenishment.newRequest");
								int needed = action.getFontMetrics(action.getFont()).stringWidth(action.getText())
										+ action.getInsets().left + action.getInsets().right
										+ action.getIcon().getIconWidth() + action.getIconTextGap();
								assertTrue(action.getWidth() >= needed, language + " " + scale + " " + width
										+ ": new-request button " + action.getWidth() + " < " + needed);
							}
						} catch (Exception failure) {
							throw new AssertionError(failure);
						}
					});
				}
		} finally {
			if (previousScale == null)
				System.clearProperty("flatlaf.uiScale");
			else
				System.setProperty("flatlaf.uiScale", previousScale);
			java.lang.reflect.Method resetScale = UIScale.class.getDeclaredMethod("tests_uninitialize");
			resetScale.setAccessible(true);
			resetScale.invoke(null);
			AppLocal.setLocale(previousLocale);
			UIManager.setLookAndFeel(previousTheme);
			RetailPOSTheme.applyFonts();
			session.close();
		}
	}
	private static AbstractButton namedButton(Container root, String name) {
		for (Component child : root.getComponents()) {
			if (child instanceof AbstractButton && name.equals(child.getName()))
				return (AbstractButton) child;
			if (child instanceof Container) {
				AbstractButton result = namedButton((Container) child, name);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	private static void layout(Container root) {
		root.doLayout();
		for (Component child : root.getComponents())
			if (child instanceof Container)
				layout((Container) child);
	}
}
