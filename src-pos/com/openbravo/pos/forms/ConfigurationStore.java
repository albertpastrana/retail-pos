package com.openbravo.pos.forms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Owns the location and persistence of the application configuration. */
public final class ConfigurationStore {
	public static final String CONFIG_ENVIRONMENT_VARIABLE = "RETAIL_POS_CONFIG";
	private static final String CONFIG_FILE_NAME = ".retail-pos.properties";
	private static final String LEGACY_CONFIG_FILE_NAME = "openbravopos.properties";

	private ConfigurationStore() {
	}

	public static AppConfig load() {
		File file = resolveFile();
		AppConfig config = new AppConfig();
		try {
			InputStream input = new FileInputStream(file);
			config.load(input);
		} catch (IOException e) {
			config.load(null);
		}
		return config;
	}

	public static AppConfig reset() {
		File file = resolveFile();
		if (file.exists() && !file.delete()) {
			return null;
		}
		return load();
	}

	public static void save(AppConfig config) throws IOException {
		File target = resolveFile();
		File parent = target.getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
			throw new IOException("Cannot create configuration directory: " + parent);
		}

		Path targetPath = target.toPath();
		Path temporary = Files.createTempFile(parent.toPath(), ".retail-pos-", ".tmp");
		try {
			try (OutputStream out = new FileOutputStream(temporary.toFile())) {
				config.store(out);
			}
			try {
				Files.setPosixFilePermissions(temporary, ConfigurationPermissions.privateFile());
			} catch (UnsupportedOperationException e) {
				// Windows does not expose POSIX permissions.
			}
			try {
				Files.move(temporary, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(temporary, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	static File resolveFile() {
		String override = System.getenv(CONFIG_ENVIRONMENT_VARIABLE);
		if (override != null && !override.trim().isEmpty()) {
			return new File(override.trim()).getAbsoluteFile();
		}

		File home = new File(System.getProperty("user.home"));
		File current = new File(home, CONFIG_FILE_NAME);
		File legacy = new File(home, LEGACY_CONFIG_FILE_NAME);
		if (!current.exists() && legacy.isFile()) {
			try {
				Files.copy(legacy.toPath(), current.toPath());
			} catch (IOException e) {
				// The normal missing-file path will use the built-in defaults.
			}
		}
		return current;
	}
}
