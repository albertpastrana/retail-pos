package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

public class DemoModeTest {
	@Test
	public void activeConfigurationUsesDemoConnection() throws Exception {
		File file = File.createTempFile("retail-pos-demo", ".properties");
		assertTrue(file.delete());
		try {
			AppConfig config = new AppConfig(file);
			config.setProperty("db.URL", "jdbc:postgresql://localhost:5432/production");
			config.setProperty("db.user", "production-user");
			config.setProperty("db.password", "production-password");
			config.setProperty("production.db.URL", "jdbc:postgresql://localhost:5432/production");
			config.setProperty("production.db.user", "production-user");
			config.setProperty("production.db.password", "production-password");
			config.setProperty(DemoMode.ACTIVE_KEY, "true");
			config.setProperty(DemoMode.URL_KEY, "jdbc:postgresql://localhost:5432/demo");
			config.setProperty(DemoMode.USER_KEY, "demo-user");
			config.setProperty(DemoMode.PASSWORD_KEY, "demo-password");
			config.save();

			AppConfig loaded = new AppConfig(file);
			loaded.load();

			assertEquals("jdbc:postgresql://localhost:5432/demo", loaded.getProperty("db.URL"));
			assertEquals("demo-user", loaded.getProperty("db.user"));
			DemoMode.exit(loaded);
			assertEquals("jdbc:postgresql://localhost:5432/production", loaded.getProperty("db.URL"));
			assertEquals("production-user", loaded.getProperty("db.user"));
		} finally {
			file.delete();
		}
	}
}
