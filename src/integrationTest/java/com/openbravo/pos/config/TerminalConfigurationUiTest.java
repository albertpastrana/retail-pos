package com.openbravo.pos.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.ConfigurationStore;
import com.openbravo.pos.payment.ExternalVoucher;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class TerminalConfigurationUiTest {
	@Test
	void recoverySavesRetainUneditedExternalVoucherName() throws Exception {
		String override = System.getenv(ConfigurationStore.CONFIG_ENVIRONMENT_VARIABLE);
		assumeTrue(override == null || override.isBlank(), "Do not write to the configured application file");
		String originalHome = System.getProperty("user.home");
		Path home = Files.createTempDirectory("voucher-recovery");
		try {
			System.setProperty("user.home", home.toString());
			AppConfig current = ConfigurationStore.load();
			current.setProperty(ExternalVoucher.NAME_KEY, "iBons");
			JPanelConfigPayment panel = new JPanelConfigPayment();
			panel.loadProperties(current);
			for (int save = 0; save < 2; save++) {
				AppConfig pending = current.copy();
				panel.saveProperties(pending);
				JPanelConfiguration.preserveRecoveryVoucherName(current, pending, panel.hasChanged());
				ConfigurationStore.saveForDatabaseRecovery(pending);
				current = ConfigurationStore.load();
				assertEquals("iBons", current.getProperty(ExternalVoucher.NAME_KEY));
				panel.loadProperties(current);
			}
		} finally {
			System.setProperty("user.home", originalHome);
		}
	}
	@Test
	void backupControlsAndConnectionFailureReadInAllLocalesAtTillWidth() throws Exception {
		Locale previous = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.forLanguageTag("es"),
					Locale.forLanguageTag("ca")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					JPanelConfigDatabase panel = new JPanelConfigDatabase();
					panel.setSize(900, panel.getPreferredSize().height);
					layout(panel);
					JPanel backup = field(panel, "jPanelBackup", JPanel.class);
					for (String name : new String[]{"jLabelBackupHour", "jLabelBackupRetention", "jLabelBackupDir"}) {
						JLabel label = field(panel, name, JLabel.class);
						assertTrue(label.getWidth() >= label.getPreferredSize().width, locale + ": " + name + " width="
								+ label.getWidth() + " preferred=" + label.getPreferredSize().width);
						assertTrue(label.getX() + label.getWidth() <= backup.getWidth(), locale + ": " + name);
					}
					assertTrue(AppLocal.getIntString("message.startupdatabase", "Connection refused")
							.contains("Connection refused"));
					assertTrue(AppLocal.getIntString("message.backupowner").length() > 20);
				});
			}
		} finally {
			AppLocal.setLocale(previous);
		}
	}

	private static void layout(Container container) {
		container.doLayout();
		for (Component component : container.getComponents()) {
			if (component instanceof Container) {
				layout((Container) component);
			}
		}
	}

	private static <T> T field(Object owner, String name, Class<T> type) {
		try {
			Field field = owner.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return type.cast(field.get(owner));
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
