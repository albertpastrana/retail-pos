package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
				legacyConfig.store(out);
			}

			AppConfig loaded = ConfigurationStore.load();
			assertEquals("legacy-host", loaded.getProperty("machine.hostname"));
			assertTrue(new File(home.toFile(), ".retail-pos.properties").isFile());
		} finally {
			System.setProperty("user.home", originalHome);
		}
	}
}
