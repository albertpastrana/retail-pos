package com.openbravo.pos.forms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.openbravo.data.loader.Session;

/** Imports signed catalog files found in the local startup directory. */
public final class CatalogFileImporter {

	private static final Logger LOGGER = Logger.getLogger(CatalogFileImporter.class.getName());
	private static final String DIRECTORY_PROPERTY = "catalog.import.directory";
	private static final String PUBLIC_KEY_PROPERTY = "catalog.import.publicKey";
	private static final String SIGNATURE_SUFFIX = ".sig";
	private static final String[] DATA_SUFFIXES = {".csv", ".tsv"};

	private CatalogFileImporter() {
	}

	public static void importAtStartup(Session session, AppProperties properties) {
		Path directory = configuredDirectory(properties);
		if (directory == null) {
			return;
		}
		Path publicKeyFile = pathProperty(properties.getProperty(PUBLIC_KEY_PROPERTY));
		if (publicKeyFile == null) {
			LOGGER.warning("event=catalog_import_disabled reason=missing_public_key");
			return;
		}
		try {
			Files.createDirectories(directory);
			PublicKey publicKey = readPublicKey(publicKeyFile);
			Path processed = directory.resolve("processed");
			Path rejected = directory.resolve("rejected");
			Files.createDirectories(processed);
			Files.createDirectories(rejected);
			try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, CatalogFileImporter::isDataFile)) {
				for (Path file : files) {
					importFile(session, file, publicKey, processed, rejected);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "event=catalog_import_startup_failed directory=" + safePath(directory), e);
		}
	}

	private static void importFile(Session session, Path file, PublicKey publicKey, Path processed, Path rejected) {
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
			String type = importType(file.getFileName().toString());
			List<Row> rows = parse(content, delimiter(content), type);
			if (rows.isEmpty()) {
				throw new IllegalArgumentException("file has no data rows");
			}
			ImportSummary summary = apply(session, type, rows);
			movePair(file, signature, processed);
			LOGGER.info("event=catalog_import_success file=" + safePath(file) + " type=" + type + " rows="
					+ summary.rows + " inserted=" + summary.inserted + " updated=" + summary.updated);
		} catch (Exception e) {
			try {
				reject(file, signature, rejected,
						e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
			} catch (IOException moveError) {
				LOGGER.log(Level.WARNING, "event=catalog_import_reject_failed file=" + safePath(file), moveError);
			}
			LOGGER.log(Level.WARNING, "event=catalog_import_failed file=" + safePath(file), e);
		}
	}

	private static ImportSummary apply(Session session, String type, List<Row> rows) throws Exception {
		validateUnique(rows, "barcode");
		session.begin();
		try {
			Connection connection = session.getConnection();
			ImportSummary summary = "products".equals(type)
					? upsertProducts(connection, rows)
					: upsertFallback(connection, rows);
			session.commit();
			return summary;
		} catch (Exception e) {
			session.rollback();
			throw e;
		}
	}

	private static ImportSummary upsertProducts(Connection connection, List<Row> rows) throws SQLException {
		int inserted = 0;
		int updated = 0;
		try (PreparedStatement findCategory = connection.prepareStatement("SELECT ID FROM CATEGORIES WHERE ID = ?");
				PreparedStatement find = connection.prepareStatement("SELECT ID FROM PRODUCTS WHERE CODE = ?");
				PreparedStatement update = connection.prepareStatement("UPDATE PRODUCTS SET REFERENCE = ?, NAME = ?, "
						+ "PRICEBUY = ?, PRICESELL = ?, CATEGORY = ?, BRAND = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE ID = ?");
				PreparedStatement insert = connection.prepareStatement("INSERT INTO PRODUCTS (ID, REFERENCE, CODE, "
						+ "CODETYPE, NAME, PRICEBUY, PRICESELL, CATEGORY, TAXCAT, ISCOM, BRAND, CREATED_AT, "
						+ "UPDATED_AT) VALUES (?, ?, ?, 'EAN13', ?, ?, ?, ?, '001', 0, ?, CURRENT_TIMESTAMP, "
						+ "CURRENT_TIMESTAMP)");
				PreparedStatement productCategory = connection
						.prepareStatement("INSERT INTO PRODUCTS_CAT (PRODUCT, CATORDER) VALUES (?, NULL)")) {
			for (Row row : rows) {
				String category = required(row, "category");
				if (!exists(findCategory, category)) {
					throw new IllegalArgumentException("category does not exist: " + category);
				}
				String id = findId(find, required(row, "barcode"));
				if (id == null) {
					id = UUID.randomUUID().toString();
					bindProduct(insert, id, row, category);
					insert.executeUpdate();
					productCategory.setString(1, id);
					productCategory.executeUpdate();
					inserted++;
				} else {
					bindProductUpdate(update, row, category, id);
					update.executeUpdate();
					updated++;
				}
			}
		}
		return new ImportSummary(rows.size(), inserted, updated);
	}

	private static ImportSummary upsertFallback(Connection connection, List<Row> rows) throws SQLException {
		int inserted = 0;
		int updated = 0;
		try (PreparedStatement find = connection
				.prepareStatement("SELECT ID FROM CATALOG_FALLBACK_PRODUCTS WHERE BARCODE = ?");
				PreparedStatement update = connection
						.prepareStatement("UPDATE CATALOG_FALLBACK_PRODUCTS SET REFERENCE = ?, "
								+ "NAME = ?, CATEGORY_ID = ?, CATEGORY_NAME = ?, PRICE_BUY = ?, PRICE_SELL = ?, BRAND = ?, FAMILY = ? "
								+ "WHERE ID = ?");
				PreparedStatement insert = connection
						.prepareStatement("INSERT INTO CATALOG_FALLBACK_PRODUCTS (ID, BARCODE, "
								+ "REFERENCE, NAME, CATEGORY_ID, CATEGORY_NAME, PRICE_BUY, PRICE_SELL, BRAND, FAMILY) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				PreparedStatement price = connection
						.prepareStatement("INSERT INTO CATALOG_FALLBACK_PRICES (LOOKUP_CODE, "
								+ "REFERENCE, PRICE_BUY, PRICE_SELL, BRAND, SOURCE) VALUES (?, ?, ?, ?, ?, ?)")) {
			for (Row row : rows) {
				String barcode = required(row, "barcode");
				String id = findId(find, barcode);
				if (id == null) {
					bindFallback(insert, UUID.randomUUID().toString(), barcode, row);
					insert.executeUpdate();
					inserted++;
				} else {
					bindFallbackUpdate(update, row, id);
					update.executeUpdate();
					updated++;
				}
				if (row.has("price_buy") || row.has("price_sell")) {
					upsertFallbackPrice(connection, price, barcode, row);
				}
			}
		}
		return new ImportSummary(rows.size(), inserted, updated);
	}

	private static void upsertFallbackPrice(Connection connection, PreparedStatement insert, String barcode, Row row)
			throws SQLException {
		try (PreparedStatement update = connection.prepareStatement("UPDATE CATALOG_FALLBACK_PRICES SET REFERENCE = ?, "
				+ "PRICE_BUY = ?, PRICE_SELL = ?, BRAND = ? WHERE LOOKUP_CODE = ?")) {
			update.setString(1, row.value("reference"));
			setDouble(update, 2, row.value("price_buy"));
			setDouble(update, 3, row.value("price_sell"));
			update.setString(4, row.value("brand"));
			update.setString(5, barcode);
			if (update.executeUpdate() == 0) {
				insert.setString(1, barcode);
				insert.setString(2, row.value("reference"));
				setDouble(insert, 3, row.value("price_buy"));
				setDouble(insert, 4, row.value("price_sell"));
				insert.setString(5, row.value("brand"));
				insert.setString(6, "startup-file");
				insert.executeUpdate();
			}
		}
	}

	private static List<Row> parse(byte[] content, char delimiter, String type) {
		String text = new String(content, StandardCharsets.UTF_8);
		String[] lines = text.split("\\R", -1);
		if (lines.length == 0) {
			throw new IllegalArgumentException("empty file");
		}
		List<String> headers = parseLine(lines[0].replace("\uFEFF", ""), delimiter);
		Map<String, Integer> positions = new HashMap<String, Integer>();
		for (int i = 0; i < headers.size(); i++) {
			String header = headers.get(i).trim().toLowerCase(Locale.ROOT);
			if (header.isEmpty() || positions.put(header, Integer.valueOf(i)) != null) {
				throw new IllegalArgumentException("duplicate or empty column in header");
			}
		}
		Set<String> allowed = new HashSet<String>();
		if ("products".equals(type)) {
			allowed.addAll(List.of("barcode", "reference", "name", "category", "price_buy", "price_sell", "brand"));
		} else {
			allowed.addAll(List.of("barcode", "reference", "name", "category_id", "category_name", "price_buy",
					"price_sell", "brand", "family"));
		}
		if (!allowed.containsAll(positions.keySet())) {
			throw new IllegalArgumentException("unknown column in header");
		}
		if (!positions.containsKey("barcode") || !positions.containsKey("name")) {
			throw new IllegalArgumentException("barcode and name columns are required");
		}
		List<Row> result = new ArrayList<Row>();
		for (int lineNumber = 1; lineNumber < lines.length; lineNumber++) {
			if (lines[lineNumber].trim().isEmpty()) {
				continue;
			}
			List<String> values = parseLine(lines[lineNumber], delimiter);
			if (values.size() != headers.size()) {
				throw new IllegalArgumentException("wrong column count at line " + (lineNumber + 1));
			}
			Map<String, String> fields = new HashMap<String, String>();
			for (Map.Entry<String, Integer> entry : positions.entrySet()) {
				fields.put(entry.getKey(), values.get(entry.getValue()).trim());
			}
			validateRow(fields, type, lineNumber + 1);
			result.add(new Row(fields));
		}
		return result;
	}

	private static void validateRow(Map<String, String> fields, String type, int line) {
		for (String required : new String[]{"barcode", "name"}) {
			if (fields.get(required) == null || fields.get(required).isEmpty()) {
				throw new IllegalArgumentException("missing " + required + " at line " + line);
			}
		}
		if ("products".equals(type) && (fields.get("category") == null || fields.get("category").isEmpty())) {
			throw new IllegalArgumentException("missing category at line " + line);
		}
		for (String price : new String[]{"price_buy", "price_sell"}) {
			if (fields.containsKey(price) && !fields.get(price).isEmpty()) {
				try {
					Double.parseDouble(fields.get(price));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("invalid " + price + " at line " + line);
				}
			}
		}
	}

	private static void validateUnique(List<Row> rows, String field) {
		Set<String> values = new HashSet<String>();
		for (Row row : rows) {
			if (!values.add(row.value(field))) {
				throw new IllegalArgumentException("duplicate " + field + ": " + row.value(field));
			}
		}
	}

	private static List<String> parseLine(String line, char delimiter) {
		List<String> fields = new ArrayList<String>();
		StringBuilder field = new StringBuilder();
		boolean quoted = false;
		boolean fieldStart = true;
		for (int i = 0; i < line.length(); i++) {
			char character = line.charAt(i);
			if (quoted) {
				if (character == '"') {
					if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
						field.append('"');
						i++;
					} else {
						quoted = false;
					}
				} else {
					field.append(character);
				}
			} else if (fieldStart && character == '"') {
				quoted = true;
				fieldStart = false;
			} else if (character == delimiter) {
				fields.add(field.toString());
				field.setLength(0);
				fieldStart = true;
			} else {
				field.append(character);
				fieldStart = false;
			}
		}
		if (quoted) {
			throw new IllegalArgumentException("unclosed quoted field");
		}
		fields.add(field.toString());
		return fields;
	}

	private static char delimiter(byte[] content) {
		int lineEnd = 0;
		while (lineEnd < content.length && content[lineEnd] != '\n' && content[lineEnd] != '\r') {
			lineEnd++;
		}
		for (int i = 0; i < lineEnd; i++) {
			if (content[i] == '\t') {
				return '\t';
			}
		}
		return ',';
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

	private static void bindProduct(PreparedStatement statement, String id, Row row, String category)
			throws SQLException {
		statement.setString(1, id);
		statement.setString(2, required(row, "reference"));
		statement.setString(3, required(row, "barcode"));
		statement.setString(4, required(row, "name"));
		setDouble(statement, 5, required(row, "price_buy"));
		setDouble(statement, 6, required(row, "price_sell"));
		statement.setString(7, category);
		statement.setString(8, row.value("brand"));
	}

	private static void bindProductUpdate(PreparedStatement statement, Row row, String category, String id)
			throws SQLException {
		statement.setString(1, required(row, "reference"));
		statement.setString(2, required(row, "name"));
		setDouble(statement, 3, required(row, "price_buy"));
		setDouble(statement, 4, required(row, "price_sell"));
		statement.setString(5, category);
		statement.setString(6, row.value("brand"));
		statement.setString(7, id);
	}

	private static void bindFallback(PreparedStatement statement, String id, String barcode, Row row)
			throws SQLException {
		statement.setString(1, id);
		statement.setString(2, barcode);
		statement.setString(3, row.value("reference"));
		statement.setString(4, required(row, "name"));
		statement.setString(5, row.value("category_id"));
		statement.setString(6, row.value("category_name"));
		setDouble(statement, 7, row.value("price_buy"));
		setDouble(statement, 8, row.value("price_sell"));
		statement.setString(9, row.value("brand"));
		statement.setString(10, row.value("family"));
	}

	private static void bindFallbackUpdate(PreparedStatement statement, Row row, String id) throws SQLException {
		statement.setString(1, row.value("reference"));
		statement.setString(2, required(row, "name"));
		statement.setString(3, row.value("category_id"));
		statement.setString(4, row.value("category_name"));
		setDouble(statement, 5, row.value("price_buy"));
		setDouble(statement, 6, row.value("price_sell"));
		statement.setString(7, row.value("brand"));
		statement.setString(8, row.value("family"));
		statement.setString(9, id);
	}

	private static boolean exists(PreparedStatement statement, String value) throws SQLException {
		statement.setString(1, value);
		try (ResultSet result = statement.executeQuery()) {
			return result.next();
		}
	}

	private static String findId(PreparedStatement statement, String value) throws SQLException {
		statement.setString(1, value);
		try (ResultSet result = statement.executeQuery()) {
			return result.next() ? result.getString(1) : null;
		}
	}

	private static void setDouble(PreparedStatement statement, int index, String value) throws SQLException {
		if (value == null || value.isEmpty()) {
			statement.setNull(index, java.sql.Types.DOUBLE);
		} else {
			statement.setDouble(index, Double.parseDouble(value));
		}
	}

	private static String required(Row row, String field) {
		String value = row.value(field);
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException("missing " + field);
		}
		return value;
	}

	private static Path configuredDirectory(AppProperties properties) {
		String configured = properties.getProperty(DIRECTORY_PROPERTY);
		return configured == null || configured.trim().isEmpty() ? null : pathProperty(configured);
	}

	private static Path pathProperty(String value) {
		return value == null || value.trim().isEmpty() ? null : Path.of(value.trim()).toAbsolutePath().normalize();
	}

	private static boolean isDataFile(Path path) {
		if (!Files.isRegularFile(path)) {
			return false;
		}
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		for (String suffix : DATA_SUFFIXES) {
			if (name.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}

	private static String importType(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		if (lower.startsWith("products-") || lower.startsWith("products.")) {
			return "products";
		}
		if (lower.startsWith("fallback-products-") || lower.startsWith("fallback-products.")) {
			return "fallback-products";
		}
		throw new IllegalArgumentException("file name must start with products- or fallback-products-");
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
		LOGGER.warning("event=catalog_import_rejected file=" + safePath(file) + " reason=" + reason);
		moveIfPresent(file, directory);
		moveIfPresent(signature, directory);
	}

	private static void moveIfPresent(Path source, Path directory) throws IOException {
		if (Files.isRegularFile(source)) {
			Files.move(source, directory.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static String safePath(Path path) {
		return path == null ? "" : path.toString().replace(' ', '_');
	}

	private static final class Row {
		private final Map<String, String> fields;

		private Row(Map<String, String> fields) {
			this.fields = fields;
		}

		private String value(String field) {
			return fields.get(field);
		}

		private boolean has(String field) {
			return fields.containsKey(field) && !fields.get(field).isEmpty();
		}
	}

	private static final class ImportSummary {
		private final int rows;
		private final int inserted;
		private final int updated;

		private ImportSummary(int rows, int inserted, int updated) {
			this.rows = rows;
			this.inserted = inserted;
			this.updated = updated;
		}
	}
}
