package com.openbravo.pos.forms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.openbravo.data.loader.Session;

/**
 * Applies signed, one-shot SQL files from the local startup import directory.
 */
public final class AdHocSqlMigrationImporter {
	private static final Logger LOGGER = Logger.getLogger(AdHocSqlMigrationImporter.class.getName());
	private static final String DIRECTORY_PROPERTY = "catalog.import.directory";
	private static final String PUBLIC_KEY_PROPERTY = "catalog.import.publicKey";
	private static final String SIGNATURE_SUFFIX = ".sig";
	private static final String FILE_PREFIX = "migration-";
	private static final String FILE_SUFFIX = ".sql";

	private AdHocSqlMigrationImporter() {
	}

	public static void importAtStartup(Session session, AppProperties properties) {
		Path directory = configuredDirectory(properties);
		if (directory == null) {
			return;
		}
		Path publicKeyFile = pathProperty(properties.getProperty(PUBLIC_KEY_PROPERTY));
		if (publicKeyFile == null || !Files.isRegularFile(publicKeyFile)) {
			LOGGER.warning("event=adhoc_migration_disabled reason=missing_public_key");
			return;
		}
		try {
			Files.createDirectories(directory);
			PublicKey publicKey = readPublicKey(publicKeyFile);
			Path processed = directory.resolve("processed");
			Path rejected = directory.resolve("rejected");
			Files.createDirectories(processed);
			Files.createDirectories(rejected);
			List<Path> files = new ArrayList<Path>();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory,
					AdHocSqlMigrationImporter::isMigrationFile)) {
				stream.forEach(files::add);
			}
			files.sort(Comparator.comparing(path -> path.getFileName().toString()));
			for (Path file : files) {
				applyFile(session, file, publicKey, processed, rejected);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "event=adhoc_migration_startup_failed directory=" + safePath(directory), e);
		}
	}

	private static void applyFile(Session session, Path file, PublicKey publicKey, Path processed, Path rejected) {
		Path signature = file.resolveSibling(file.getFileName().toString() + SIGNATURE_SUFFIX);
		try {
			if (!Files.isRegularFile(signature)) {
				reject(file, signature, rejected, "missing_signature");
				return;
			}
			byte[] content = Files.readAllBytes(file);
			if (!verify(content, Files.readAllBytes(signature), publicKey)) {
				reject(file, signature, rejected, "invalid_signature");
				return;
			}
			String id = file.getFileName().toString();
			String checksum = checksum(content);
			if (alreadyApplied(session.getConnection(), id, checksum)) {
				movePair(file, signature, processed);
				LOGGER.info("event=adhoc_migration_skipped file=" + safePath(file) + " reason=already_applied");
				return;
			}
			List<String> statements = splitStatements(new String(content, StandardCharsets.UTF_8));
			if (statements.isEmpty()) {
				throw new IllegalArgumentException("file has no SQL statements");
			}
			session.begin();
			try {
				Connection connection = session.getConnection();
				try (java.sql.Statement statement = connection.createStatement()) {
					for (String sql : statements) {
						statement.execute(sql);
					}
				}
				try (PreparedStatement history = connection.prepareStatement(
						"INSERT INTO AD_HOC_MIGRATIONS (ID, CHECKSUM, APPLIED_AT, DESCRIPTION, APP_VERSION) "
								+ "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?)")) {
					history.setString(1, id);
					history.setString(2, checksum);
					history.setString(3, id.substring(FILE_PREFIX.length(), id.length() - FILE_SUFFIX.length()));
					history.setString(4, AppLocal.APP_VERSION);
					history.executeUpdate();
				}
				session.commit();
			} catch (Exception e) {
				if (session.isTransaction()) {
					session.rollback();
				}
				throw e;
			}
			movePair(file, signature, processed);
			LOGGER.info("event=adhoc_migration_success file=" + safePath(file) + " statements=" + statements.size());
		} catch (Exception e) {
			try {
				reject(file, signature, rejected, reason(e));
			} catch (IOException moveError) {
				LOGGER.log(Level.WARNING, "event=adhoc_migration_reject_failed file=" + safePath(file), moveError);
			}
			LOGGER.log(Level.WARNING, "event=adhoc_migration_failed file=" + safePath(file), e);
		}
	}

	private static boolean alreadyApplied(Connection connection, String id, String checksum) throws SQLException {
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT CHECKSUM FROM AD_HOC_MIGRATIONS WHERE ID = ?")) {
			statement.setString(1, id);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return false;
				}
				if (!checksum.equals(result.getString(1))) {
					throw new IllegalArgumentException("migration identifier was already used with different content");
				}
				return true;
			}
		}
	}

	private static List<String> splitStatements(String sql) {
		List<String> statements = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		boolean singleQuote = false;
		boolean doubleQuote = false;
		boolean lineComment = false;
		boolean blockComment = false;
		for (int i = 0; i < sql.length(); i++) {
			char character = sql.charAt(i);
			char next = i + 1 < sql.length() ? sql.charAt(i + 1) : 0;
			if (lineComment) {
				current.append(character);
				if (character == '\n' || character == '\r') {
					lineComment = false;
				}
				continue;
			}
			if (blockComment) {
				current.append(character);
				if (character == '*' && next == '/') {
					current.append(next);
					i++;
					blockComment = false;
				}
				continue;
			}
			if (!singleQuote && !doubleQuote && character == '-' && next == '-') {
				current.append(character).append(next);
				i++;
				lineComment = true;
				continue;
			}
			if (!singleQuote && !doubleQuote && character == '/' && next == '*') {
				current.append(character).append(next);
				i++;
				blockComment = true;
				continue;
			}
			if (character == '\'' && !doubleQuote) {
				if (singleQuote && next == '\'') {
					current.append(character).append(next);
					i++;
					continue;
				}
				singleQuote = !singleQuote;
			} else if (character == '"' && !singleQuote) {
				if (doubleQuote && next == '"') {
					current.append(character).append(next);
					i++;
					continue;
				}
				doubleQuote = !doubleQuote;
			}
			if (character == ';' && !singleQuote && !doubleQuote) {
				addStatement(statements, current);
			} else {
				current.append(character);
			}
		}
		addStatement(statements, current);
		return statements;
	}

	private static void addStatement(List<String> statements, StringBuilder current) {
		String statement = current.toString().trim();
		if (!statement.isEmpty() && !statement.matches("(?s)(--.*|/\\*.*\\*/)")) {
			statements.add(statement);
		}
		current.setLength(0);
	}

	private static String checksum(byte[] content) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
		StringBuilder result = new StringBuilder(digest.length * 2);
		for (byte value : digest) {
			result.append(String.format("%02x", value));
		}
		return result.toString();
	}

	private static boolean verify(byte[] content, byte[] signatureBytes, PublicKey publicKey) throws Exception {
		Signature signature = Signature.getInstance("Ed25519");
		signature.initVerify(publicKey);
		signature.update(content);
		return signature.verify(signatureBytes);
	}

	private static PublicKey readPublicKey(Path path) throws Exception {
		String pem = Files.readString(path, StandardCharsets.US_ASCII).trim();
		String encoded = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
				.replaceAll("\\s", "");
		return KeyFactory.getInstance("Ed25519")
				.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
	}

	private static Path configuredDirectory(AppProperties properties) {
		return pathProperty(properties.getProperty(DIRECTORY_PROPERTY));
	}

	private static Path pathProperty(String value) {
		return value == null || value.trim().isEmpty() ? null : Path.of(value.trim()).toAbsolutePath().normalize();
	}

	private static boolean isMigrationFile(Path path) {
		if (!Files.isRegularFile(path)) {
			return false;
		}
		String name = path.getFileName().toString();
		return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX)
				&& name.length() > FILE_PREFIX.length() + FILE_SUFFIX.length();
	}

	private static void movePair(Path file, Path signature, Path directory) throws IOException {
		if (Files.exists(directory.resolve(file.getFileName()))
				|| Files.exists(directory.resolve(signature.getFileName()))) {
			throw new IOException("file was already processed");
		}
		Files.move(file, directory.resolve(file.getFileName()), StandardCopyOption.ATOMIC_MOVE);
		Files.move(signature, directory.resolve(signature.getFileName()), StandardCopyOption.ATOMIC_MOVE);
	}

	private static void reject(Path file, Path signature, Path directory, String reason) throws IOException {
		LOGGER.warning("event=adhoc_migration_rejected file=" + safePath(file) + " reason=" + reason);
		moveIfPresent(file, directory);
		moveIfPresent(signature, directory);
	}

	private static void moveIfPresent(Path source, Path directory) throws IOException {
		if (Files.isRegularFile(source)) {
			Files.move(source, directory.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static String reason(Exception exception) {
		return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
	}

	private static String safePath(Path path) {
		return path == null ? "" : path.toString().replace(' ', '_');
	}
}
