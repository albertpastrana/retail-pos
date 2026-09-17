package com.openbravo.pos.forms;

import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/** Checks the configured release endpoint without blocking the POS startup. */
public final class UpdateChecker {

	private static final Logger logger = Logger.getLogger("com.openbravo.pos.forms.UpdateChecker");
	private static final String DEFAULT_URL = "https://api.github.com/repos/albertpastrana/retail-pos/releases/latest";
	private static final Pattern TAG_PATTERN = Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
	private static final Pattern PAGE_PATTERN = Pattern.compile("\\\"html_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

	private UpdateChecker() {
	}

	public static void checkAsync(final AppConfig config, final Component parent) {
		if ("false".equalsIgnoreCase(config.getProperty("update.check"))) {
			logger.info("Update check disabled by configuration");
			return;
		}

		final String endpoint = configuredUrl(config);
		logger.info("Starting update check; endpoint=" + endpoint + ", local directory="
				+ config.getProperty("update.dir"));
		Thread checker = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Release release = findLocal(config.getProperty("update.dir"));
					if (release == null) {
						release = fetch(endpoint);
					}
					if (release != null && hasNewerVersion(release)) {
						logger.info("New application version available: " + release.version);
						final Release availableRelease = release;
						java.awt.EventQueue.invokeLater(new Runnable() {
							@Override
							public void run() {
								showUpdate(parent, availableRelease);
							}
						});
					} else {
						logger.info("No newer application version found; current=" + AppLocal.APP_VERSION);
					}
				} catch (Exception e) {
					logger.log(Level.WARNING, "Unexpected error while checking for application updates", e);
				}
			}
		}, "retail-pos-update-check");
		checker.setDaemon(true);
		checker.start();
	}

	private static Release findLocal(String directory) {
		if (directory == null || directory.trim().isEmpty()) {
			logger.info("No local update directory configured");
			return null;
		}
		File updateDirectory = new File(directory);
		File[] files = updateDirectory.listFiles();
		if (files == null) {
			logger.warning("Cannot read local update directory: " + updateDirectory.getAbsolutePath());
			return null;
		}
		String platform = platformName();
		Pattern packagePattern = Pattern.compile("RetailPOS-(.+)-" + platform + "-x86_64\\.(tar\\.gz|zip)",
				Pattern.CASE_INSENSITIVE);
		Release newest = null;
		for (File file : files) {
			Matcher matcher = packagePattern.matcher(file.getName());
			if (matcher.matches() && verifyChecksum(file)) {
				Release candidate = new Release(matcher.group(1), file.toURI().toString(), file);
				if (!hasValidVersion(candidate.version)) {
					continue;
				}
				if (newest == null || isNewer(candidate.version, newest.version)) {
					newest = candidate;
				}
			}
		}
		if (newest != null) {
			logger.info("Found verified local update package: " + newest.packageFile.getAbsolutePath());
		}
		return newest;
	}

	private static String platformName() {
		String os = System.getProperty("os.name", "").toLowerCase();
		if (os.contains("win")) {
			return "windows";
		}
		if (os.contains("mac")) {
			return "macos";
		}
		return "linux";
	}

	private static boolean verifyChecksum(File packageFile) {
		File checksumFile = new File(packageFile.getPath() + ".sha256");
		if (!checksumFile.isFile()) {
			return false;
		}
		try {
			String expected = new String(Files.readAllBytes(checksumFile.toPath()), StandardCharsets.UTF_8).trim()
					.split("\\s+")[0];
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream input = Files.newInputStream(packageFile.toPath())) {
				byte[] buffer = new byte[8192];
				int read;
				while ((read = input.read(buffer)) != -1) {
					digest.update(buffer, 0, read);
				}
			}
			StringBuilder actual = new StringBuilder();
			for (byte value : digest.digest()) {
				actual.append(String.format("%02x", value));
			}
			return expected.equalsIgnoreCase(actual.toString());
		} catch (Exception e) {
			logger.log(Level.FINE, "Could not verify local update package", e);
			return false;
		}
	}

	private static String configuredUrl(AppConfig config) {
		String url = config.getProperty("update.url");
		return url == null || url.trim().isEmpty() ? DEFAULT_URL : url.trim();
	}

	private static Release fetch(String endpoint) {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(endpoint).openConnection();
			connection.setConnectTimeout(3000);
			connection.setReadTimeout(3000);
			connection.setRequestProperty("Accept", "application/vnd.github+json");
			connection.setRequestProperty("User-Agent", AppLocal.APP_ID + "-update-checker");
			int responseCode = connection.getResponseCode();
			logger.info("Update endpoint response: " + responseCode);
			if (responseCode != HttpURLConnection.HTTP_OK) {
				return null;
			}

			StringBuilder body = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					body.append(line);
				}
			}

			Matcher tag = TAG_PATTERN.matcher(body);
			Matcher page = PAGE_PATTERN.matcher(body);
			if (!tag.find() || !page.find()) {
				return null;
			}
			Release release = new Release(tag.group(1), page.group(1));
			logger.info("Update endpoint release: " + release.version);
			return release;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not check for application updates", e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static boolean hasNewerVersion(Release release) {
		try {
			return isNewer(release.version, AppLocal.APP_VERSION);
		} catch (NumberFormatException e) {
			logger.log(Level.FINE, "Ignoring malformed release version: " + release.version);
			return false;
		}
	}

	private static boolean hasValidVersion(String version) {
		try {
			numericParts(version);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isNewer(String available, String current) {
		String[] availableParts = numericParts(available);
		String[] currentParts = numericParts(current);
		int length = Math.max(availableParts.length, currentParts.length);
		for (int i = 0; i < length; i++) {
			int availablePart = i < availableParts.length ? Integer.parseInt(availableParts[i]) : 0;
			int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
			if (availablePart != currentPart) {
				return availablePart > currentPart;
			}
		}
		return false;
	}

	private static String[] numericParts(String version) {
		String normalized = version.toLowerCase().startsWith("v") ? version.substring(1) : version;
		String numeric = normalized.split("[-+]")[0];
		String[] parts = numeric.split("\\.");
		for (String part : parts) {
			Integer.parseInt(part);
		}
		return parts;
	}

	private static void showUpdate(Component parent, Release release) {
		String message = AppLocal.getIntString("update.available", release.version, AppLocal.APP_VERSION);
		if (release.packageFile != null) {
			int choice = JOptionPane.showOptionDialog(parent, message, AppLocal.getIntString("update.title"),
					JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
					new Object[]{AppLocal.getIntString("update.install"), AppLocal.getIntString("update.later")},
					AppLocal.getIntString("update.install"));
			logger.info("Update dialog choice for local package: " + choice);
			if (choice == 0) {
				installAndRestart(parent, release.packageFile);
			}
			return;
		}
		int choice = JOptionPane.showOptionDialog(parent, message, AppLocal.getIntString("update.title"),
				JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
				new Object[]{AppLocal.getIntString("update.download"), AppLocal.getIntString("update.later")},
				AppLocal.getIntString("update.download"));
		logger.info("Update dialog choice for remote release: " + choice);
		if (choice == 0) {
			try {
				if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					Desktop.getDesktop().browse(new URI(release.pageUrl));
				}
			} catch (Exception e) {
				logger.log(Level.FINE, "Could not open the release page", e);
			}
		}
	}

	private static void installAndRestart(Component parent, File packageFile) {
		try {
			File installDir = new File(System.getProperty("dirname.path")).getCanonicalFile();
			String platform = platformName();
			File updater = new File(installDir, platform.equals("windows") ? "update.bat" : "update.sh");
			logger.info("Starting updater; package=" + packageFile.getAbsolutePath() + ", install="
					+ installDir.getAbsolutePath() + ", updater=" + updater.getAbsolutePath() + ", pid=" + processId());
			if (!updater.isFile()) {
				throw new IOException("Updater is not present in the application package");
			}
			if (!platform.equals("windows") && !updater.canExecute()) {
				throw new IOException("Updater is not executable: " + updater.getAbsolutePath());
			}
			if (platform.equals("windows")) {
				new ProcessBuilder("cmd", "/c", "start", "", updater.getAbsolutePath(), packageFile.getAbsolutePath(),
						installDir.getAbsolutePath(), processId()).start();
			} else {
				new ProcessBuilder(updater.getAbsolutePath(), packageFile.getAbsolutePath(),
						installDir.getAbsolutePath(), processId()).start();
			}
			logger.info("Updater process started; exiting application");
			System.exit(0);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not start the application updater", e);
			JOptionPane.showMessageDialog(parent, AppLocal.getIntString("update.startError"),
					AppLocal.getIntString("update.title"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private static String processId() {
		return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	}

	private static final class Release {
		private final String version;
		private final String pageUrl;
		private final File packageFile;

		private Release(String version, String pageUrl) {
			this(version, pageUrl, null);
		}

		private Release(String version, String pageUrl, File packageFile) {
			this.version = version;
			this.pageUrl = pageUrl;
			this.packageFile = packageFile;
		}
	}
}
