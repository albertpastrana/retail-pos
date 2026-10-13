package com.openbravo.pos.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.pos.forms.AppLocal;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class TerminalConfigurationUiTest {
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
