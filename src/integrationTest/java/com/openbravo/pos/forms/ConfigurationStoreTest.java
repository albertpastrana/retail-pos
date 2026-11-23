package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.openbravo.pos.config.JPanelConfigPayment;
import com.openbravo.pos.payment.ExternalVoucher;

import org.junit.jupiter.api.Test;

public class ConfigurationStoreTest {
	@Test
	public void unchangedExternalVoucherNameDoesNotOverwriteOtherTills() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			AppConfig loaded = new AppConfig();
			loaded.setProperty(ExternalVoucher.NAME_KEY, "iBons");
			JPanelConfigPayment panel = new JPanelConfigPayment();
			panel.loadProperties(loaded);
			AppConfig pending = loaded.copy();
			panel.saveProperties(pending);
			assertNull(pending.getProperty(ExternalVoucher.NAME_KEY));

			JTextField field = java.util.Arrays.stream(panel.getComponents()).filter(JTextField.class::isInstance)
					.map(JTextField.class::cast).findFirst().orElseThrow();
			field.setText(" New campaign ");
			assertTrue(panel.hasChanged());
			panel.saveProperties(pending);
			assertEquals("New campaign", pending.getProperty(ExternalVoucher.NAME_KEY));
		});
	}
	@Test
	public void normalizesTerminalIdAndRejectsInvalidIdentity() {
		AppConfig config = new AppConfig();
		config.load(null);
		String id = UUID.randomUUID().toString();
		config.setProperty("terminal.id", " " + id + " ");
		config.ensureTerminalId();
		assertEquals(id, config.getHost());
		config.setProperty("terminal.id", "not-a-uuid");
		assertTrue(assertThrows(IllegalStateException.class, config::ensureTerminalId).getMessage()
				.contains("terminal.id"));
	}

	@Test
	public void migratesLegacyConfigurationToCanonicalFile() throws Exception {
		String originalHome = System.getProperty("user.home");
		Path home = Files.createTempDirectory("retail-pos-config");
		try {
			System.setProperty("user.home", home.toString());
			File legacy = new File(home.toFile(), "openbravopos.properties");
			AppConfig legacyConfig = new AppConfig();
			legacyConfig.setProperty("machine.hostname", "legacy-host");
			try (FileOutputStream out = new FileOutputStream(legacy)) {
				legacyConfig.storeWithLegacySettings(out);
			}

			AppConfig loaded = ConfigurationStore.load();
			assertEquals("legacy-host", loaded.getProperty("machine.hostname"));
			assertEquals(loaded.getHost(), ConfigurationStore.load().getHost());
			assertEquals(36, loaded.getHost().length());
			loaded.setProperty("machine.printer", "legacy-printer");
			ConfigurationStore.saveForDatabaseRecovery(loaded);
			assertEquals("legacy-printer", ConfigurationStore.load().getProperty("machine.printer"));
			loaded.setProperty("machine.window.width", "1032");
			ConfigurationStore.save(loaded);
			Properties bootstrap = new Properties();
			try (FileInputStream input = new FileInputStream(new File(home.toFile(), ".retail-pos.properties"))) {
				bootstrap.load(input);
			}
			assertNull(bootstrap.getProperty("machine.printer"));
			assertEquals("1032", bootstrap.getProperty("machine.window.width"));
			assertEquals(loaded.getHost(), ConfigurationStore.reset().getHost());
			assertEquals(loaded.getHost(), ConfigurationStore.load().getHost());
			assertTrue(new File(home.toFile(), ".retail-pos.properties").isFile());
		} finally {
			System.setProperty("user.home", originalHome);
		}
	}
}
