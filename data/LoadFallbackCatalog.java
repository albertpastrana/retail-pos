import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/** Replaces the shared fallback catalogue from the source TSV files. */
public class LoadFallbackCatalog {
	public static void main(String[] args) throws Exception {
		if (args.length != 6) {
			throw new IllegalArgumentException(
					"usage: LoadFallbackCatalog JDBC_URL USER PASSWORD categories.tsv products.tsv prices.tsv");
		}
		String url = args[0];
		loadDriver(url);
		try (Connection connection = connect(url, args[1], args[2])) {
			connection.setAutoCommit(false);
			try {
				ensureTables(connection);
				Map<String, String[]> categories = readCategories(args[3]);
				Map<String, String[]> prices = readPrices(args[5]);
				try (Statement clear = connection.createStatement();
						PreparedStatement product = connection.prepareStatement("INSERT INTO CATALOG_FALLBACK_PRODUCTS "
								+ "(ID, BARCODE, REFERENCE, NAME, CATEGORY_ID, CATEGORY_NAME, PRICE_BUY, PRICE_SELL, BRAND) "
								+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
						PreparedStatement price = connection.prepareStatement("INSERT INTO CATALOG_FALLBACK_PRICES "
								+ "(LOOKUP_CODE, REFERENCE, PRICE_BUY, PRICE_SELL, BRAND, SOURCE) VALUES (?, ?, ?, ?, ?, ?)");
						BufferedReader rows = reader(args[4])) {
					clear.executeUpdate("DELETE FROM CATALOG_FALLBACK_PRICES");
					clear.executeUpdate("DELETE FROM CATALOG_FALLBACK_PRODUCTS");
					int products = loadProducts(rows, product, categories);
					int priceKeys = loadPrices(price, prices);
					connection.commit();
					System.out.println("Loaded fallback products=" + products + " price_keys=" + priceKeys);
				}
			} catch (Exception e) {
				connection.rollback();
				throw e;
			}
		}
	}

	private static int loadProducts(BufferedReader rows, PreparedStatement insert, Map<String, String[]> categories)
			throws Exception {
		int count = 0;
		String line;
		while ((line = rows.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			}
			String[] row = line.split("\t", -1);
			if (row.length < 7) {
				throw new IllegalArgumentException("Bad product row: " + line);
			}
			String[] category = categories.get(row[4]);
			if (category == null) {
				throw new IllegalArgumentException("Category not found for product: " + row[4]);
			}
			insert.setString(1, row[0]);
			insert.setString(2, row[2]);
			insert.setString(3, row[1]);
			insert.setString(4, row[3]);
			insert.setString(5, row[4]);
			insert.setString(6, category[1]);
			setDouble(insert, 7, row[5]);
			setDouble(insert, 8, row[6]);
			insert.setString(9, row.length > 7 ? row[7] : null);
			insert.addBatch();
			count++;
			if (count % 500 == 0) {
				insert.executeBatch();
			}
		}
		insert.executeBatch();
		return count;
	}

	private static int loadPrices(PreparedStatement insert, Map<String, String[]> prices) throws Exception {
		int count = 0;
		for (Map.Entry<String, String[]> entry : prices.entrySet()) {
			String[] row = entry.getValue();
			insert.setString(1, entry.getKey());
			insert.setString(2, row[1]);
			setDouble(insert, 3, row[2]);
			setDouble(insert, 4, row[3]);
			insert.setString(5, row[4]);
			insert.setString(6, row.length > 5 ? row[5] : null);
			insert.addBatch();
			count++;
			if (count % 500 == 0) {
				insert.executeBatch();
			}
		}
		insert.executeBatch();
		return count;
	}

	private static void ensureTables(Connection connection) throws Exception {
		if (!tableExists(connection, "CATALOG_FALLBACK_PRODUCTS")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE TABLE CATALOG_FALLBACK_PRODUCTS ("
						+ "ID VARCHAR(255) NOT NULL PRIMARY KEY, BARCODE VARCHAR(255) NOT NULL UNIQUE, "
						+ "REFERENCE VARCHAR(255), NAME VARCHAR(255), CATEGORY_ID VARCHAR(255), "
						+ "CATEGORY_NAME VARCHAR(255), PRICE_BUY DOUBLE PRECISION, PRICE_SELL DOUBLE PRECISION, "
						+ "BRAND VARCHAR(255))");
			}
		}
		if (!tableExists(connection, "CATALOG_FALLBACK_PRICES")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE TABLE CATALOG_FALLBACK_PRICES ("
						+ "LOOKUP_CODE VARCHAR(255) NOT NULL PRIMARY KEY, REFERENCE VARCHAR(255), "
						+ "PRICE_BUY DOUBLE PRECISION, PRICE_SELL DOUBLE PRECISION, BRAND VARCHAR(255), "
						+ "SOURCE VARCHAR(255))");
			}
		}
	}

	private static boolean tableExists(Connection connection, String name) throws Exception {
		try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
			while (tables.next()) {
				if (name.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
					return true;
				}
			}
			return false;
		}
	}

	private static Map<String, String[]> readCategories(String path) throws Exception {
		Map<String, String[]> categories = new LinkedHashMap<String, String[]>();
		try (BufferedReader rows = reader(path)) {
			String line;
			while ((line = rows.readLine()) != null) {
				String[] row = line.split("\t", -1);
				if (row.length >= 2) {
					categories.put(row[0], row);
				}
			}
		}
		return categories;
	}

	private static Map<String, String[]> readPrices(String path) throws Exception {
		Map<String, String[]> prices = new LinkedHashMap<String, String[]>();
		try (BufferedReader rows = reader(path)) {
			String line;
			boolean header = true;
			while ((line = rows.readLine()) != null) {
				if (header) {
					header = false;
					continue;
				}
				String[] row = line.split("\t", -1);
				if (row.length >= 4) {
					prices.put(row[0], row);
					if (!row[1].isEmpty()) {
						prices.put(row[1], row);
					}
				}
			}
		}
		return prices;
	}

	private static void setDouble(PreparedStatement statement, int index, String value) throws Exception {
		if (value == null || value.isEmpty()) {
			statement.setNull(index, java.sql.Types.DOUBLE);
		} else {
			statement.setDouble(index, Double.parseDouble(value));
		}
	}

	private static BufferedReader reader(String path) throws Exception {
		return new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
	}

	private static void loadDriver(String url) throws Exception {
		if (url.startsWith("jdbc:postgresql:")) {
			Class.forName("org.postgresql.Driver");
		} else if (url.startsWith("jdbc:derby:")) {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		} else if (url.startsWith("jdbc:mysql:")) {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} else {
			throw new IllegalArgumentException("Unsupported JDBC URL: " + url);
		}
	}

	private static Connection connect(String url, String user, String password) throws Exception {
		return DriverManager.getConnection(url, user, password == null ? "" : password);
	}
}
