import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.openbravo.pos.forms.CatalogTsvParser;

public class InsertCatalogRows {
	public static void main(String[] args) throws Exception {
		if (args.length != 5) {
			throw new IllegalArgumentException(
					"usage: InsertCatalogRows JDBC_URL USER PASSWORD categories.tsv batch.tsv");
		}
		String url = args[0];
		String user = args[1];
		String password = args[2];
		String cats = args[3];
		String batch = args[4];
		loadDriver(url);
		Connection c = connect(url, user, password);
		c.setAutoCommit(false);
		try {
			Map<String, String[]> categories = readCategories(cats);
			PreparedStatement find = c.prepareStatement("SELECT ID FROM PRODUCTS WHERE CODE = ?");
			PreparedStatement update = c.prepareStatement(
					"UPDATE PRODUCTS SET PRICEBUY = ?, PRICESELL = ?, BRAND = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE CODE = ?");
			PreparedStatement insert = c
					.prepareStatement("INSERT INTO PRODUCTS (ID, REFERENCE, CODE, CODETYPE, NAME, PRICEBUY, PRICESELL, "
							+ "CATEGORY, TAXCAT, ATTRIBUTESET_ID, STOCKCOST, STOCKVOLUME, IMAGE, ISCOM, ISSCALE, "
							+ "ATTRIBUTES, BRAND, CREATED_AT, UPDATED_AT) VALUES (?, ?, ?, 'EAN13', ?, ?, ?, ?, '001', NULL, NULL, NULL, "
							+ "NULL, ?, ?, NULL, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
			PreparedStatement findCat = c.prepareStatement("SELECT PRODUCT FROM PRODUCTS_CAT WHERE PRODUCT = ?");
			PreparedStatement insertCat = c
					.prepareStatement("INSERT INTO PRODUCTS_CAT (PRODUCT, CATORDER) VALUES (?, NULL)");
			int inserted = 0;
			int updated = 0;
			BufferedReader r = reader(batch);
			String line;
			while ((line = r.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				String[] p = CatalogTsvParser.parse(line);
				if (p.length < 8) {
					throw new IllegalArgumentException("Bad product row: " + line);
				}
				ensureCategory(c, p[4], categories);
				double buy = Double.parseDouble(p[5]);
				double sell = Double.parseDouble(p[6]);
				find.setString(1, p[2]);
				ResultSet existing = find.executeQuery();
				String productId;
				if (existing.next()) {
					productId = existing.getString(1);
					update.setDouble(1, buy);
					update.setDouble(2, sell);
					update.setString(3, p[7]);
					update.setString(4, p[2]);
					update.executeUpdate();
					updated++;
				} else {
					productId = p[0];
					insert.setString(1, productId);
					insert.setString(2, p[1]);
					insert.setString(3, p[2]);
					insert.setString(4, p[3]);
					insert.setDouble(5, buy);
					insert.setDouble(6, sell);
					insert.setString(7, p[4]);
					insert.setBoolean(8, false);
					insert.setBoolean(9, false);
					insert.setString(10, p[7]);
					insert.executeUpdate();
					inserted++;
				}
				existing.close();
				findCat.setString(1, productId);
				ResultSet inCatalog = findCat.executeQuery();
				if (!inCatalog.next()) {
					insertCat.setString(1, productId);
					insertCat.executeUpdate();
				}
				inCatalog.close();
			}
			r.close();
			c.commit();
			System.out.println("inserted=" + inserted + " updated=" + updated);
		} catch (Exception e) {
			c.rollback();
			throw e;
		} finally {
			c.close();
			if (url.startsWith("jdbc:derby:")) {
				try {
					DriverManager.getConnection("jdbc:derby:;shutdown=true");
				} catch (Exception ignored) {
				}
			}
		}
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
		if (user == null || user.isEmpty()) {
			return DriverManager.getConnection(url);
		}
		return DriverManager.getConnection(url, user, password == null ? "" : password);
	}

	private static void ensureCategory(Connection connection, String categoryId, Map<String, String[]> categories)
			throws Exception {
		PreparedStatement find = connection.prepareStatement("SELECT ID FROM CATEGORIES WHERE ID = ?");
		find.setString(1, categoryId);
		ResultSet result = find.executeQuery();
		boolean exists = result.next();
		result.close();
		find.close();
		if (exists) {
			return;
		}
		String[] category = categories.get(categoryId);
		if (category == null) {
			throw new IllegalArgumentException("Category not found in catalog: " + categoryId);
		}
		String parentId = category.length > 2 && category[2].length() > 0 ? category[2] : null;
		if (parentId != null) {
			ensureCategory(connection, parentId, categories);
		}
		PreparedStatement insert = connection
				.prepareStatement("INSERT INTO CATEGORIES (ID, NAME, PARENTID, IMAGE) VALUES (?, ?, ?, NULL)");
		insert.setString(1, category[0]);
		insert.setString(2, category[1]);
		if (parentId == null) {
			insert.setNull(3, java.sql.Types.VARCHAR);
		} else {
			insert.setString(3, parentId);
		}
		insert.executeUpdate();
		insert.close();
	}

	private static Map<String, String[]> readCategories(String path) throws Exception {
		Map<String, String[]> categories = new HashMap<String, String[]>();
		BufferedReader r = reader(path);
		try {
			String line;
			while ((line = r.readLine()) != null) {
				String[] category = CatalogTsvParser.parse(line);
				if (category.length >= 2) {
					categories.put(category[0], category);
				}
			}
		} finally {
			r.close();
		}
		return categories;
	}

	private static BufferedReader reader(String path) throws Exception {
		return new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
	}
}
