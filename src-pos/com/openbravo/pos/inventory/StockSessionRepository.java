package com.openbravo.pos.inventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Shared persisted scanning sessions; only posting a receipt touches stock. */
final class StockSessionRepository {
	static final class Session {
		String id, type, name, supplier, note, location;
		Timestamp started;
		int lines;
	}

	static final class Line {
		String id, product, code, barcode, name, reference, brand;
		double units, stock;
		Double retailPrice;
		boolean ticked;
		Timestamp scanned;
	}

	private final Connection connection;

	StockSessionRepository(Connection connection) {
		this.connection = connection;
	}

	Session open(String supplier, String note, String location, String user) throws SQLException {
		if (supplier == null || supplier.isBlank() || note == null || note.isBlank())
			throw new IllegalArgumentException("Supplier and delivery note are required");
		String id = UUID.randomUUID().toString();
		try (PreparedStatement sql = connection.prepareStatement(
				"INSERT INTO STOCKSESSION " + "(ID,TYPE,NAME,SUPPLIER,DELIVERYNOTE,LOCATION,DATESTART,STATUS,APPUSER) "
						+ "VALUES (?,'RECEIPT',?,?,?,?,?,'OPEN',?)")) {
			sql.setString(1, id);
			sql.setString(2, supplier.trim());
			sql.setString(3, supplier.trim());
			sql.setString(4, note.trim());
			sql.setString(5, location);
			sql.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
			sql.setString(7, user);
			sql.executeUpdate();
		}
		return get(id);
	}

	List<Session> openSessions() throws SQLException {
		List<Session> sessions = new ArrayList<>();
		try (PreparedStatement sql = connection.prepareStatement("SELECT S.ID,S.TYPE,S.NAME,S.SUPPLIER,"
				+ "S.DELIVERYNOTE,S.LOCATION,S.DATESTART,COUNT(L.ID) AS LINES FROM STOCKSESSION S "
				+ "LEFT JOIN STOCKSESSIONLINE L ON L.STOCKSESSION=S.ID WHERE S.STATUS='OPEN' "
				+ "GROUP BY S.ID,S.TYPE,S.NAME,S.SUPPLIER,S.DELIVERYNOTE,S.LOCATION,S.DATESTART "
				+ "ORDER BY S.DATESTART DESC"); ResultSet rs = sql.executeQuery()) {
			while (rs.next())
				sessions.add(session(rs));
		}
		return sessions;
	}

	Session get(String id) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT S.ID,S.TYPE,S.NAME,S.SUPPLIER,"
				+ "S.DELIVERYNOTE,S.LOCATION,S.DATESTART,COUNT(L.ID) AS LINES FROM STOCKSESSION S "
				+ "LEFT JOIN STOCKSESSIONLINE L ON L.STOCKSESSION=S.ID WHERE S.ID=? AND S.STATUS='OPEN' "
				+ "GROUP BY S.ID,S.TYPE,S.NAME,S.SUPPLIER,S.DELIVERYNOTE,S.LOCATION,S.DATESTART")) {
			sql.setString(1, id);
			try (ResultSet rs = sql.executeQuery()) {
				return rs.next() ? session(rs) : null;
			}
		}
	}

	private static Session session(ResultSet rs) throws SQLException {
		Session s = new Session();
		s.id = rs.getString("ID");
		s.type = rs.getString("TYPE");
		s.name = rs.getString("NAME");
		s.supplier = rs.getString("SUPPLIER");
		s.note = rs.getString("DELIVERYNOTE");
		s.location = rs.getString("LOCATION");
		s.started = rs.getTimestamp("DATESTART");
		s.lines = rs.getInt("LINES");
		return s;
	}

	List<Line> lines(String session, boolean byName) throws SQLException {
		return withLockedSession(session, () -> readLines(session, byName));
	}

	private List<Line> readLines(String session, boolean byName) throws SQLException {
		List<Line> result = new ArrayList<>();
		Session s = get(session);
		if (s == null)
			throw new IllegalStateException("Session is no longer open");
		resolveUnknowns(session);
		String variant = hasCurrentVariantColumn() ? " AND S.ATTRIBUTESETINSTANCE_ID IS NULL" : "";
		Set<String> productColumns = productColumns();
		String brand = productColumns.contains("BRAND") ? "P.BRAND" : "CAST(NULL AS CHAR(1))";
		String price = productColumns.contains("PRICESELL") ? "P.PRICESELL" : "CAST(NULL AS DECIMAL(18,4))";
		String tax = productColumns.contains("TAXCAT") && hasTaxRates()
				? "(SELECT MAX(T.RATE) FROM TAXES T WHERE T.CATEGORY=P.TAXCAT "
						+ "AND T.CUSTCATEGORY IS NULL AND T.VALIDFROM <= CURRENT_TIMESTAMP "
						+ "AND T.VALIDFROM=(SELECT MAX(T2.VALIDFROM) FROM TAXES T2 "
						+ "WHERE T2.CATEGORY=P.TAXCAT AND T2.CUSTCATEGORY IS NULL "
						+ "AND T2.VALIDFROM <= CURRENT_TIMESTAMP))"
				: "CAST(NULL AS DECIMAL(18,4))";
		try (PreparedStatement sql = connection.prepareStatement("SELECT L.ID,L.PRODUCT,L.CODE,L.UNITS,L.TICKED,"
				+ "L.DATELAST,P.NAME,P.REFERENCE,P.CODE," + brand + " AS BRAND," + price + " AS PRICESELL," + tax
				+ " AS TAX_RATE,COALESCE((SELECT SUM(S.UNITS) FROM STOCKCURRENT S "
				+ "WHERE S.PRODUCT=L.PRODUCT AND S.LOCATION=?" + variant + "),0) AS CURRENTUNITS "
				+ "FROM STOCKSESSIONLINE L LEFT JOIN PRODUCTS P ON P.ID=L.PRODUCT " + "WHERE L.STOCKSESSION=? ORDER BY "
				+ (byName ? "P.NAME,L.CODE" : "L.DATELAST DESC,L.ID DESC"))) {
			sql.setString(1, s.location);
			sql.setString(2, session);
			try (ResultSet rs = sql.executeQuery()) {
				while (rs.next()) {
					Line line = new Line();
					line.id = rs.getString(1);
					line.product = rs.getString(2);
					line.code = rs.getString(3);
					line.units = rs.getDouble(4);
					line.ticked = rs.getInt(5) == 1;
					line.scanned = rs.getTimestamp(6);
					line.name = rs.getString(7);
					line.reference = rs.getString(8);
					line.barcode = line.product == null ? line.code : rs.getString(9);
					line.brand = rs.getString(10);
					Double priceSell = rs.getObject(11) == null ? null : rs.getDouble(11);
					Double taxRate = rs.getObject(12) == null ? null : rs.getDouble(12);
					line.retailPrice = priceSell == null || taxRate == null ? null : priceSell * (1 + taxRate);
					line.stock = rs.getDouble(13);
					result.add(line);
				}
			}
		}
		return result;
	}

	private Set<String> productColumns() throws SQLException {
		Set<String> columns = new HashSet<>();
		try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM PRODUCTS WHERE 1=0");
				ResultSet rows = statement.executeQuery()) {
			java.sql.ResultSetMetaData metadata = rows.getMetaData();
			for (int index = 1; index <= metadata.getColumnCount(); index++)
				columns.add(metadata.getColumnName(index).toUpperCase(java.util.Locale.ROOT));
		}
		return columns;
	}

	private boolean hasTaxRates() {
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT CATEGORY,CUSTCATEGORY,VALIDFROM,RATE FROM TAXES WHERE 1=0");
				ResultSet ignored = statement.executeQuery()) {
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	private boolean hasCurrentVariantColumn() throws SQLException {
		// Installed catalogues may predate the stock variant column. Inspect the
		// connected schema rather than assuming the migration baseline's shape.
		try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM STOCKCURRENT WHERE 1=0");
				ResultSet rows = statement.executeQuery()) {
			java.sql.ResultSetMetaData columns = rows.getMetaData();
			for (int i = 1; i <= columns.getColumnCount(); i++)
				if ("ATTRIBUTESETINSTANCE_ID".equalsIgnoreCase(columns.getColumnName(i)))
					return true;
		}
		return false;
	}

	private void resolveUnknowns(String session) throws SQLException {
		try (PreparedStatement unknown = connection.prepareStatement(
				"SELECT ID,CODE,UNITS FROM STOCKSESSIONLINE WHERE STOCKSESSION=? AND PRODUCT IS NULL")) {
			unknown.setString(1, session);
			try (ResultSet rows = unknown.executeQuery()) {
				while (rows.next()) {
					try (PreparedStatement match = connection
							.prepareStatement("SELECT ID FROM PRODUCTS WHERE CODE=?")) {
						match.setString(1, rows.getString(2));
						try (ResultSet products = match.executeQuery()) {
							if (!products.next())
								continue;
							String product = products.getString(1);
							if (products.next())
								continue;
							try (PreparedStatement merge = connection.prepareStatement(
									"UPDATE STOCKSESSIONLINE SET UNITS=UNITS+? WHERE STOCKSESSION=? AND PRODUCT=?")) {
								merge.setDouble(1, rows.getDouble(3));
								merge.setString(2, session);
								merge.setString(3, product);
								if (merge.executeUpdate() != 0) {
									remove(session, rows.getString(1));
									continue;
								}
							}
							try (PreparedStatement resolve = connection
									.prepareStatement("UPDATE STOCKSESSIONLINE SET PRODUCT=? WHERE ID=?")) {
								resolve.setString(1, product);
								resolve.setString(2, rows.getString(1));
								resolve.executeUpdate();
							}
						}
					}
				}
			}
		}
	}

	void scan(String session, String code, double quantity, boolean override) throws SQLException {
		scanProduct(session, null, code, quantity, override);
	}

	void scanProduct(String session, String productId, String code, double quantity, boolean override)
			throws SQLException {
		if ((productId == null && (code == null || code.isBlank())) || !wholeQuantity(quantity))
			throw new IllegalArgumentException("Enter a code and a positive quantity");
		withLockedSession(session, () -> {
			scanLocked(session, productId, code, quantity, override);
			return null;
		});
	}

	private void scanLocked(String session, String productId, String code, double quantity, boolean override)
			throws SQLException {
		Session s = get(session);
		if (s == null || !"RECEIPT".equals(s.type))
			throw new IllegalStateException("Receipt is no longer open");
		resolveUnknowns(session);
		String normalized = code == null || code.isBlank() ? productId : code.trim();
		String product = null;
		try (PreparedStatement lookup = connection.prepareStatement(
				productId == null ? "SELECT ID FROM PRODUCTS WHERE CODE=?" : "SELECT ID FROM PRODUCTS WHERE ID=?")) {
			lookup.setString(1, productId == null ? normalized : productId);
			try (ResultSet rs = lookup.executeQuery()) {
				if (rs.next()) {
					product = rs.getString(1);
					if (productId == null && rs.next())
						throw new IllegalArgumentException("Code matches more than one product");
				} else if (productId != null)
					throw new IllegalArgumentException("Product no longer exists");
			}
		}
		Timestamp now = new Timestamp(System.currentTimeMillis());
		// Finder selection and barcode scans accumulate on the same product line.
		try (PreparedStatement update = connection.prepareStatement("UPDATE STOCKSESSIONLINE SET UNITS="
				+ (override ? "?" : "UNITS+?") + ",DATELAST=? WHERE STOCKSESSION=? AND "
				+ (product == null ? "PRODUCT IS NULL AND CODE=?" : "PRODUCT=?"))) {
			update.setDouble(1, quantity);
			update.setTimestamp(2, now);
			update.setString(3, session);
			update.setString(4, product == null ? normalized : product);
			if (update.executeUpdate() != 0)
				return;
		}
		try (PreparedStatement insert = connection.prepareStatement("INSERT INTO STOCKSESSIONLINE "
				+ "(ID,STOCKSESSION,PRODUCT,CODE,UNITS,TICKED,DATEFIRST,DATELAST) VALUES (?,?,?,?,?,0,?,?)")) {
			insert.setString(1, UUID.randomUUID().toString());
			insert.setString(2, session);
			insert.setString(3, product);
			// Resolved lines are identified by product. The original scanned code
			// may be shared by another product, while unknown lines retain that code.
			insert.setString(4, product == null ? normalized : product);
			insert.setDouble(5, quantity);
			insert.setTimestamp(6, now);
			insert.setTimestamp(7, now);
			insert.executeUpdate();
		}
	}

	void quantity(String session, String line, double quantity) throws SQLException {
		if (!wholeQuantity(quantity))
			throw new IllegalArgumentException("Enter a positive quantity");
		change("UPDATE STOCKSESSIONLINE SET UNITS=? WHERE ID=? AND STOCKSESSION=?", session, line, quantity);
	}

	private static boolean wholeQuantity(double quantity) {
		return Double.isFinite(quantity) && quantity > 0 && quantity == Math.rint(quantity);
	}

	void tick(String session, String line, boolean checked) throws SQLException {
		change("UPDATE STOCKSESSIONLINE SET TICKED=? WHERE ID=? AND STOCKSESSION=?", session, line, checked ? 1 : 0);
	}

	void tickAll(String session, boolean checked) throws SQLException {
		withLockedSession(session, () -> {
			try (PreparedStatement sql = connection
					.prepareStatement("UPDATE STOCKSESSIONLINE SET TICKED=? WHERE STOCKSESSION=?")) {
				sql.setInt(1, checked ? 1 : 0);
				sql.setString(2, session);
				sql.executeUpdate();
			}
			return null;
		});
	}

	private void change(String statement, String session, String line, double value) throws SQLException {
		withLockedSession(session, () -> {
			try (PreparedStatement sql = connection.prepareStatement(statement)) {
				sql.setDouble(1, value);
				sql.setString(2, line);
				sql.setString(3, session);
				if (sql.executeUpdate() != 1)
					throw new IllegalArgumentException("Line no longer exists");
			}
			return null;
		});
	}

	void remove(String session, String line) throws SQLException {
		withLockedSession(session, () -> {
			try (PreparedStatement sql = connection
					.prepareStatement("DELETE FROM STOCKSESSIONLINE WHERE ID=? AND STOCKSESSION=?")) {
				sql.setString(1, line);
				sql.setString(2, session);
				sql.executeUpdate();
			}
			return null;
		});
	}

	@FunctionalInterface
	private interface SqlAction<T> {
		T run() throws SQLException;
	}

	private <T> T withLockedSession(String session, SqlAction<T> action) throws SQLException {
		boolean ownTransaction = connection.getAutoCommit();
		try {
			if (ownTransaction)
				connection.setAutoCommit(false);
			try (PreparedStatement lock = connection
					.prepareStatement("SELECT ID FROM STOCKSESSION WHERE ID=? AND STATUS='OPEN' FOR UPDATE")) {
				lock.setString(1, session);
				try (ResultSet rs = lock.executeQuery()) {
					if (!rs.next())
						throw new IllegalStateException("Session is no longer open");
				}
			}
			T result = action.run();
			if (ownTransaction)
				connection.commit();
			return result;
		} catch (SQLException | RuntimeException ex) {
			if (ownTransaction)
				connection.rollback();
			throw ex;
		} finally {
			if (ownTransaction)
				connection.setAutoCommit(true);
		}
	}

	void discard(String session) throws SQLException {
		boolean auto = connection.getAutoCommit();
		try {
			connection.setAutoCommit(false);
			try (PreparedStatement lock = connection.prepareStatement(
					"SELECT ID FROM STOCKSESSION WHERE ID=? AND TYPE='RECEIPT' AND STATUS='OPEN' FOR UPDATE")) {
				lock.setString(1, session);
				try (ResultSet rs = lock.executeQuery()) {
					if (!rs.next())
						throw new IllegalStateException("Receipt is no longer open");
				}
			}
			try (PreparedStatement lines = connection
					.prepareStatement("DELETE FROM STOCKSESSIONLINE WHERE STOCKSESSION=?")) {
				lines.setString(1, session);
				lines.executeUpdate();
			}
			try (PreparedStatement receipt = connection
					.prepareStatement("DELETE FROM STOCKSESSION WHERE ID=? AND TYPE='RECEIPT' AND STATUS='OPEN'")) {
				receipt.setString(1, session);
				if (receipt.executeUpdate() != 1)
					throw new IllegalStateException("Receipt is no longer open");
			}
			connection.commit();
		} catch (SQLException | RuntimeException ex) {
			connection.rollback();
			throw ex;
		} finally {
			connection.setAutoCommit(auto);
		}
	}

	void post(String session, String user) throws SQLException {
		boolean auto = connection.getAutoCommit();
		try {
			connection.setAutoCommit(false);
			Session s;
			try (PreparedStatement lock = connection.prepareStatement(
					"SELECT ID FROM STOCKSESSION WHERE ID=? AND TYPE='RECEIPT' AND STATUS='OPEN' FOR UPDATE")) {
				lock.setString(1, session);
				try (ResultSet rs = lock.executeQuery()) {
					if (!rs.next())
						throw new IllegalStateException("Receipt is no longer open");
				}
			}
			s = get(session);
			List<Line> lines = lines(session, false);
			String variant = hasCurrentVariantColumn() ? " AND ATTRIBUTESETINSTANCE_ID IS NULL" : "";
			if (lines.isEmpty())
				throw new IllegalArgumentException("Add at least one product");
			for (Line line : lines) {
				if (line.product == null) {
					try (PreparedStatement lookup = connection
							.prepareStatement("SELECT ID FROM PRODUCTS WHERE CODE=?")) {
						lookup.setString(1, line.code);
						try (ResultSet rs = lookup.executeQuery()) {
							if (!rs.next())
								throw new IllegalArgumentException("Unknown code: " + line.code);
							line.product = rs.getString(1);
							if (rs.next())
								throw new IllegalArgumentException("Ambiguous code: " + line.code);
						}
					}
					try (PreparedStatement resolve = connection
							.prepareStatement("UPDATE STOCKSESSIONLINE SET PRODUCT=? WHERE ID=?")) {
						resolve.setString(1, line.product);
						resolve.setString(2, line.id);
						resolve.executeUpdate();
					}
				}
				if (!wholeQuantity(line.units))
					throw new IllegalArgumentException("Invalid quantity: " + line.code);
				try (PreparedStatement check = connection.prepareStatement("SELECT ID FROM PRODUCTS WHERE ID=?")) {
					check.setString(1, line.product);
					try (ResultSet rs = check.executeQuery()) {
						if (!rs.next())
							throw new IllegalArgumentException("Product removed: " + line.code);
					}
				}
			}
			for (Line line : lines) {
				try (PreparedStatement update = connection.prepareStatement(
						"UPDATE STOCKCURRENT SET UNITS=UNITS+? " + "WHERE LOCATION=? AND PRODUCT=?" + variant)) {
					update.setDouble(1, line.units);
					update.setString(2, s.location);
					update.setString(3, line.product);
					if (update.executeUpdate() == 0) {
						try (PreparedStatement insert = connection.prepareStatement(
								"INSERT INTO STOCKCURRENT " + "(LOCATION,PRODUCT,UNITS) VALUES (?,?,?)")) {
							insert.setString(1, s.location);
							insert.setString(2, line.product);
							insert.setDouble(3, line.units);
							insert.executeUpdate();
						}
					}
				}
				try (PreparedStatement diary = connection.prepareStatement("INSERT INTO STOCKDIARY "
						+ "(ID,DATENEW,REASON,LOCATION,PRODUCT,UNITS,PRICE,SUPPLIER,DELIVERYNOTE,STOCKSESSION) "
						+ "VALUES (?,?,1,?,?,?,0,?,?,?)")) {
					diary.setString(1, UUID.randomUUID().toString());
					diary.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
					diary.setString(3, s.location);
					diary.setString(4, line.product);
					diary.setDouble(5, line.units);
					diary.setString(6, s.supplier);
					diary.setString(7, s.note);
					diary.setString(8, session);
					diary.executeUpdate();
				}
				try (PreparedStatement ordered = connection.prepareStatement("UPDATE REPLENISHMENT_ENTRIES "
						+ "SET STATUS='RECEIVED',OPEN_PRODUCT_ID=NULL,UPDATED_AT=?,UPDATED_BY=? "
						+ "WHERE OPEN_PRODUCT_ID=? AND STATUS='ORDERED'")) {
					ordered.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
					ordered.setString(2, user);
					ordered.setString(3, line.product);
					ordered.executeUpdate();
				}
			}
			try (PreparedStatement finish = connection
					.prepareStatement("UPDATE STOCKSESSION SET STATUS='POSTED',DATEEND=? WHERE ID=?")) {
				finish.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
				finish.setString(2, session);
				finish.executeUpdate();
			}
			connection.commit();
		} catch (SQLException | RuntimeException ex) {
			connection.rollback();
			throw ex;
		} finally {
			connection.setAutoCommit(auto);
		}
	}
}
