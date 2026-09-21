import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class ImportCatalog {
	public static void main(String[] args) throws Exception {
		String db = args[0];
		String cats = args[1];
		String prods = args[2];
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		Connection c = DriverManager.getConnection("jdbc:derby:" + db);
		c.setAutoCommit(false);
		Statement s = c.createStatement();
		try {
			s.executeUpdate("DELETE FROM PRODUCTS_COM");
			s.executeUpdate("DELETE FROM PRODUCTS_CAT");
			s.executeUpdate("DELETE FROM STOCKDIARY");
			s.executeUpdate("DELETE FROM STOCKCURRENT");
			s.executeUpdate("DELETE FROM STOCKLEVEL");
			s.executeUpdate("DELETE FROM PRODUCTS");
			s.executeUpdate("UPDATE CATEGORIES SET PARENTID = NULL");
			s.executeUpdate("DELETE FROM CATEGORIES WHERE ID <> '000'");

			s.executeUpdate("UPDATE TAXCATEGORIES SET NAME = 'IVA 21%' WHERE ID = '001'");
			s.executeUpdate("UPDATE TAXES SET NAME = 'IVA 21%', RATE = 0.21 WHERE ID = '001'");

			PreparedStatement insCat = c
					.prepareStatement("INSERT INTO CATEGORIES (ID, NAME, PARENTID, IMAGE) VALUES (?, ?, ?, NULL)");
			int nCat = 0;
			BufferedReader r = reader(cats);
			String line;
			while ((line = r.readLine()) != null) {
				String[] p = line.split("\t", -1);
				insCat.setString(1, p[0]);
				insCat.setString(2, p[1]);
				if (p.length > 2 && p[2].length() > 0) {
					insCat.setString(3, p[2]);
				} else {
					insCat.setNull(3, java.sql.Types.VARCHAR);
				}
				insCat.addBatch();
				nCat++;
			}
			r.close();
			insCat.executeBatch();

			Map<String, String[]> prices = readPrices(new File(new File(prods).getParentFile(), "import-prices.tsv"));
			PreparedStatement insProd = c.prepareStatement(
					"INSERT INTO PRODUCTS (ID, REFERENCE, CODE, CODETYPE, NAME, PRICEBUY, PRICESELL, CATEGORY, TAXCAT, ATTRIBUTESET_ID, STOCKCOST, STOCKVOLUME, IMAGE, ISCOM, ISSCALE, ATTRIBUTES, BRAND) VALUES (?, ?, ?, 'EAN13', ?, ?, ?, ?, '001', NULL, NULL, NULL, NULL, 0, 0, NULL, ?)");
			PreparedStatement insProdCat = c
					.prepareStatement("INSERT INTO PRODUCTS_CAT (PRODUCT, CATORDER) VALUES (?, NULL)");
			int nProd = 0;
			r = reader(prods);
			while ((line = r.readLine()) != null) {
				String[] p = line.split("\t", -1);
				String[] priced = prices.get(p[2]);
				if (priced == null) {
					priced = prices.get(p[1]);
				}
				String buy = p[5];
				String sell = p[6];
				if (priced != null && priced.length >= 4) {
					buy = priced[2];
					sell = priced[3];
				}
				insProd.setString(1, p[0]);
				insProd.setString(2, p[1]);
				insProd.setString(3, p[2]);
				insProd.setString(4, p[3]);
				insProd.setDouble(5, Double.parseDouble(buy));
				insProd.setDouble(6, Double.parseDouble(sell));
				insProd.setString(7, p[4]);
				insProd.setString(8, p.length > 7 ? p[7] : null);
				insProd.addBatch();
				insProdCat.setString(1, p[0]);
				insProdCat.addBatch();
				nProd++;
				if (nProd % 500 == 0) {
					insProd.executeBatch();
					insProdCat.executeBatch();
				}
			}
			r.close();
			insProd.executeBatch();
			insProdCat.executeBatch();
			c.commit();
			System.out.println("Inserted categories=" + nCat + " products=" + nProd);
		} catch (Exception e) {
			c.rollback();
			throw e;
		} finally {
			c.close();
			try {
				DriverManager.getConnection("jdbc:derby:;shutdown=true");
			} catch (Exception ignored) {
			}
		}
	}

	private static Map<String, String[]> readPrices(File path) throws Exception {
		Map<String, String[]> prices = new HashMap<String, String[]>();
		if (!path.isFile()) {
			return prices;
		}
		BufferedReader r = reader(path.getPath());
		try {
			String line;
			boolean header = true;
			while ((line = r.readLine()) != null) {
				if (header) {
					header = false;
					continue;
				}
				String[] p = line.split("\t", -1);
				if (p.length >= 4) {
					prices.put(p[0], p);
					prices.put(p[1], p);
				}
			}
		} finally {
			r.close();
		}
		return prices;
	}

	private static BufferedReader reader(String path) throws Exception {
		return new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
	}

	private static byte[] readAll(String path) throws Exception {
		FileInputStream in = new FileInputStream(path);
		byte[] buf = new byte[in.available()];
		int n = in.read(buf);
		in.close();
		if (n < buf.length) {
			byte[] cut = new byte[n];
			System.arraycopy(buf, 0, cut, 0, n);
			return cut;
		}
		return buf;
	}
}
