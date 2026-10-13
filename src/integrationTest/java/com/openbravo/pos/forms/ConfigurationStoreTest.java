package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class ConfigurationStoreTest {
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
