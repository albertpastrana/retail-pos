package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.forms.BeanFactoryDataSingle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DataLogicReplenishment extends BeanFactoryDataSingle {
	private Session session;

	@Override
	public void init(Session s) {
		session = s;
	}

	public List<ReplenishmentEntry> list(String search, boolean received) throws BasicException {
		return list(search, "ALL", received ? "ALL" : "OPEN");
	}

	public List<ReplenishmentEntry> list(String search, String type, String status) throws BasicException {
		return list(search, type, status, false);
	}

	public List<ReplenishmentEntry> listForExport(String search, String type, String status) throws BasicException {
		return list(search, type, status, true);
	}

	private List<ReplenishmentEntry> list(String search, String type, String status, boolean exportOrder)
			throws BasicException {
		List<ReplenishmentEntry> result = new ArrayList<ReplenishmentEntry>();
		String term = search == null ? "" : search.trim();
		String pattern = "%" + term + "%";
		String sql = "SELECT R.*, P.NAME AS CREATOR_NAME FROM REPLENISHMENT_ENTRIES R "
				+ (exportOrder ? "LEFT JOIN PRODUCTS C ON C.ID = R.PRODUCT_ID " : "")
				+ "LEFT JOIN PEOPLE P ON P.ID = R.CREATED_BY WHERE ("
				+ "UPPER(COALESCE(PRODUCT_REFERENCE,'')) LIKE UPPER(?) "
				+ "OR UPPER(COALESCE(PRODUCT_NAME,'')) LIKE UPPER(?) "
				+ "OR UPPER(COALESCE(PRODUCT_EAN,'')) LIKE UPPER(?) "
				+ "OR UPPER(COALESCE(MANUAL_DESCRIPTION,'')) LIKE UPPER(?) "
				+ "OR UPPER(COALESCE(MANUAL_EAN,'')) LIKE UPPER(?) "
				+ "OR UPPER(COALESCE(CUSTOMER_NAME,'')) LIKE UPPER(?) " + "OR UPPER(COALESCE(NOTE,'')) LIKE UPPER(?) "
				+ ") "
				+ ("REPLENISHMENT".equals(type)
						? "AND CUSTOMER_ID IS NULL "
						: "ENCARGO".equals(type) ? "AND CUSTOMER_ID IS NOT NULL " : "")
				+ ("OPEN".equals(status)
						? "AND STATUS NOT IN ('RECEIVED', 'DELIVERED') "
						: "PENDING".equals(status) || "ORDERED".equals(status) || "RECEIVED".equals(status)
								|| "DELIVERED".equals(status) ? "AND STATUS = '" + status + "' " : "")
				+ (exportOrder
						? "ORDER BY CASE WHEN C.BRAND IS NULL OR TRIM(C.BRAND) = '' THEN 1 ELSE 0 END, "
								+ "UPPER(C.BRAND), CASE WHEN C.FAMILY IS NULL OR TRIM(C.FAMILY) = '' THEN 1 ELSE 0 END, "
								+ "UPPER(C.FAMILY), R.CREATED_AT DESC, R.ID"
						: "ORDER BY R.CREATED_AT DESC");
		try (PreparedStatement ps = connection().prepareStatement(sql)) {
			for (int i = 1; i <= 7; i++)
				ps.setString(i, pattern);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					result.add(read(rs));
			}
		} catch (SQLException e) {
			throw new BasicException(e);
		}
		return result;
	}

	public ReplenishmentEntry add(ReplenishmentEntry e) throws BasicException {
		if (e.id == null)
			e.id = UUID.randomUUID().toString();
		e.status = "PENDING";
		e.createdAt = new Date();
		e.updatedAt = e.createdAt;
		e.updatedBy = e.createdBy;
		e.openProductId = e.productId;
		Connection c;
		try {
			c = connection();
		} catch (SQLException ex) {
			throw new BasicException(ex);
		}
		try {
			c.setAutoCommit(false);
			if (e.productId != null) {
				try (PreparedStatement find = c
						.prepareStatement("SELECT * FROM REPLENISHMENT_ENTRIES WHERE OPEN_PRODUCT_ID = ? FOR UPDATE")) {
					find.setString(1, e.productId);
					try (ResultSet rs = find.executeQuery()) {
						if (rs.next()) {
							String id = rs.getString("ID");
							try (PreparedStatement update = c.prepareStatement(
									"UPDATE REPLENISHMENT_ENTRIES SET NOTE=?, CUSTOMER_ID=?, CUSTOMER_NAME=?, UPDATED_AT=?, UPDATED_BY=? WHERE ID=?")) {
								update.setString(1, e.note);
								update.setString(2, e.customerId);
								update.setString(3, e.customerName);
								update.setTimestamp(4, new Timestamp(e.updatedAt.getTime()));
								update.setString(5, e.updatedBy);
								update.setString(6, id);
								update.executeUpdate();
							}
							c.commit();
							return get(id);
						}
					}
				}
			}
			insert(c, e);
			c.commit();
			return e;
		} catch (SQLException ex) {
			try {
				c.rollback();
			} catch (SQLException ignored) {
			}
			throw new BasicException(ex);
		} finally {
			try {
				c.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	public void updateStatus(String id, String status, String user) throws BasicException {
		if (!isValidStatus(status))
			throw new BasicException("Invalid replenishment status");
		try (PreparedStatement ps = connection().prepareStatement(
				"UPDATE REPLENISHMENT_ENTRIES SET STATUS=?, OPEN_PRODUCT_ID=CASE WHEN ? IN ('RECEIVED', 'DELIVERED') THEN NULL ELSE PRODUCT_ID END, OPEN_UNIQUE_KEY=CASE WHEN ? IN ('RECEIVED', 'DELIVERED') THEN ? ELSE "
						+ openKeyExpression()
						+ " END, UPDATED_AT=?, UPDATED_BY=? WHERE ID=? AND (? <> 'DELIVERED' OR (CUSTOMER_ID IS NOT NULL AND STATUS='RECEIVED'))")) {
			ps.setString(1, status);
			ps.setString(2, status);
			ps.setString(3, status);
			ps.setString(4, "I:" + id);
			ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
			ps.setString(6, user);
			ps.setString(7, id);
			ps.setString(8, status);
			if (ps.executeUpdate() == 0 && "DELIVERED".equals(status))
				throw new BasicException("Only customer orders can be delivered");
		} catch (SQLException e) {
			throw new BasicException(e);
		}
	}

	/**
	 * Apply a status to every selected request, or leave every request unchanged.
	 */
	public void updateStatuses(List<String> ids, String status, String user) throws BasicException {
		if (!isValidStatus(status))
			throw new BasicException("Invalid replenishment status");
		if (ids.isEmpty())
			return;
		Connection c;
		try {
			c = connection();
			boolean autoCommit = c.getAutoCommit();
			try {
				c.setAutoCommit(false);
				try (PreparedStatement ps = c.prepareStatement(
						"UPDATE REPLENISHMENT_ENTRIES SET STATUS=?, OPEN_PRODUCT_ID=CASE WHEN ? IN ('RECEIVED', 'DELIVERED') THEN NULL ELSE PRODUCT_ID END, OPEN_UNIQUE_KEY=CASE WHEN ? IN ('RECEIVED', 'DELIVERED') THEN ? ELSE "
								+ openKeyExpression()
								+ " END, UPDATED_AT=?, UPDATED_BY=? WHERE ID=? AND (? <> 'DELIVERED' OR (CUSTOMER_ID IS NOT NULL AND STATUS='RECEIVED'))")) {
					Timestamp now = new Timestamp(System.currentTimeMillis());
					for (String id : ids) {
						ps.setString(1, status);
						ps.setString(2, status);
						ps.setString(3, status);
						ps.setString(4, "I:" + id);
						ps.setTimestamp(5, now);
						ps.setString(6, user);
						ps.setString(7, id);
						ps.setString(8, status);
						if (ps.executeUpdate() != 1)
							throw new SQLException("Selected request changed or is no longer eligible");
					}
				}
				c.commit();
			} catch (SQLException | RuntimeException failure) {
				try {
					c.rollback();
				} catch (SQLException rollbackFailure) {
					failure.addSuppressed(rollbackFailure);
				}
				throw failure;
			} finally {
				c.setAutoCommit(autoCommit);
			}
		} catch (SQLException e) {
			throw new BasicException(e);
		}
	}

	public void deleteAll(List<String> ids) throws BasicException {
		if (ids.isEmpty())
			return;
		try {
			Connection c = connection();
			boolean autoCommit = c.getAutoCommit();
			try {
				c.setAutoCommit(false);
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM REPLENISHMENT_ENTRIES WHERE ID=?")) {
					for (String id : ids) {
						ps.setString(1, id);
						if (ps.executeUpdate() != 1)
							throw new SQLException("Selected request no longer exists");
					}
				}
				c.commit();
			} catch (SQLException | RuntimeException failure) {
				try {
					c.rollback();
				} catch (SQLException rollbackFailure) {
					failure.addSuppressed(rollbackFailure);
				}
				throw failure;
			} finally {
				c.setAutoCommit(autoCommit);
			}
		} catch (SQLException e) {
			throw new BasicException(e);
		}
	}

	public void update(ReplenishmentEntry e, String user) throws BasicException {
		if (!isValidStatus(e.status) || ("DELIVERED".equals(e.status) && e.customerId == null))
			throw new BasicException("Only customer orders can be delivered");
		try (PreparedStatement ps = connection().prepareStatement(
				"UPDATE REPLENISHMENT_ENTRIES SET PRODUCT_ID=?, PRODUCT_REFERENCE=?, PRODUCT_NAME=?, PRODUCT_EAN=?, MANUAL_DESCRIPTION=?, MANUAL_EAN=?, NOTE=?, CUSTOMER_ID=?, CUSTOMER_NAME=?, STATUS=?, OPEN_PRODUCT_ID=CASE WHEN ? IN ('RECEIVED', 'DELIVERED') THEN NULL ELSE PRODUCT_ID END, OPEN_UNIQUE_KEY=?, UPDATED_AT=?, UPDATED_BY=? WHERE ID=? AND (? <> 'DELIVERED' OR (CUSTOMER_ID IS NOT NULL AND STATUS='RECEIVED'))")) {
			ps.setString(1, e.productId);
			ps.setString(2, e.reference);
			ps.setString(3, e.name);
			ps.setString(4, e.ean);
			ps.setString(5, e.manualDescription);
			ps.setString(6, e.manualEan);
			ps.setString(7, e.note);
			ps.setString(8, e.customerId);
			ps.setString(9, e.customerName);
			ps.setString(10, e.status);
			ps.setString(11, e.status);
			ps.setString(12,
					"RECEIVED".equals(e.status) || "DELIVERED".equals(e.status) || e.productId == null
							? "I:" + e.id
							: "P:" + e.productId);
			ps.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
			ps.setString(14, user);
			ps.setString(15, e.id);
			ps.setString(16, e.status);
			ps.executeUpdate();
		} catch (SQLException ex) {
			throw new BasicException(ex);
		}
	}

	/**
	 * Saved requests keep their product and status; only customer and note can be
	 * edited here.
	 */
	public void updateDetails(String id, String note, String customerId, String customerName, String user)
			throws BasicException {
		try (PreparedStatement ps = connection().prepareStatement(
				"UPDATE REPLENISHMENT_ENTRIES SET NOTE=?, CUSTOMER_ID=?, CUSTOMER_NAME=?, UPDATED_AT=?, UPDATED_BY=? WHERE ID=? AND (STATUS <> 'DELIVERED' OR ? IS NOT NULL)")) {
			ps.setString(1, note);
			ps.setString(2, customerId);
			ps.setString(3, customerName);
			ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			ps.setString(5, user);
			ps.setString(6, id);
			ps.setString(7, customerId);
			if (ps.executeUpdate() == 0)
				throw new BasicException("Cannot remove the customer from a delivered order");
		} catch (SQLException ex) {
			throw new BasicException(ex);
		}
	}

	/**
	 * Save the editable fields of an existing request without changing its product.
	 */
	public void updateDetailsAndStatus(String id, String note, String customerId, String customerName, String status,
			String user) throws BasicException {
		if (!isValidStatus(status) || ("DELIVERED".equals(status) && customerId == null))
			throw new BasicException("Invalid customer order status");
		try (PreparedStatement ps = connection().prepareStatement(
				"UPDATE REPLENISHMENT_ENTRIES SET NOTE=?, CUSTOMER_ID=?, CUSTOMER_NAME=?, STATUS=?, OPEN_PRODUCT_ID=CASE WHEN ? IN ('RECEIVED', 'DELIVERED') THEN NULL ELSE PRODUCT_ID END, OPEN_UNIQUE_KEY=CASE WHEN ? IN ('RECEIVED', 'DELIVERED') THEN ? ELSE "
						+ openKeyExpression()
						+ " END, UPDATED_AT=?, UPDATED_BY=? WHERE ID=? AND (? <> 'DELIVERED' OR STATUS IN ('RECEIVED', 'DELIVERED'))")) {
			ps.setString(1, note);
			ps.setString(2, customerId);
			ps.setString(3, customerName);
			ps.setString(4, status);
			ps.setString(5, status);
			ps.setString(6, status);
			ps.setString(7, "I:" + id);
			ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
			ps.setString(9, user);
			ps.setString(10, id);
			ps.setString(11, status);
			if (ps.executeUpdate() == 0)
				throw new BasicException("Invalid customer order status");
		} catch (SQLException ex) {
			throw new BasicException(ex);
		}
	}

	public void delete(String id) throws BasicException {
		if (id == null)
			return;
		try (PreparedStatement ps = connection().prepareStatement("DELETE FROM REPLENISHMENT_ENTRIES WHERE ID=?")) {
			ps.setString(1, id);
			ps.executeUpdate();
		} catch (SQLException ex) {
			throw new BasicException(ex);
		}
	}

	private void insert(Connection c, ReplenishmentEntry e) throws SQLException {
		String sql = "INSERT INTO REPLENISHMENT_ENTRIES (ID,PRODUCT_ID,PRODUCT_REFERENCE,PRODUCT_NAME,PRODUCT_EAN,VARIANT_SIZE,VARIANT_COLOUR,MANUAL_DESCRIPTION,MANUAL_EAN,NOTE,CUSTOMER_ID,CUSTOMER_NAME,STATUS,OPEN_PRODUCT_ID,CREATED_AT,UPDATED_AT,CREATED_BY,UPDATED_BY,OPEN_UNIQUE_KEY) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			String[] values = {e.id, e.productId, e.reference, e.name, e.ean, e.size, e.colour, e.manualDescription,
					e.manualEan, e.note, e.customerId, e.customerName, e.status, e.openProductId, e.createdBy,
					e.updatedBy};
			for (int i = 0; i < 14; i++)
				ps.setString(i + 1, values[i]);
			ps.setTimestamp(15, new Timestamp(e.createdAt.getTime()));
			ps.setTimestamp(16, new Timestamp(e.updatedAt.getTime()));
			ps.setString(17, e.createdBy);
			ps.setString(18, e.updatedBy);
			ps.setString(19, e.productId == null ? "I:" + e.id : "P:" + e.productId);
			ps.executeUpdate();
		}
	}
	private ReplenishmentEntry get(String id) throws SQLException {
		try (PreparedStatement ps = connection().prepareStatement(
				"SELECT R.*, P.NAME AS CREATOR_NAME FROM REPLENISHMENT_ENTRIES R LEFT JOIN PEOPLE P ON P.ID = R.CREATED_BY WHERE R.ID=?")) {
			ps.setString(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return read(rs);
			}
		}
	}
	private ReplenishmentEntry read(ResultSet r) throws SQLException {
		ReplenishmentEntry e = new ReplenishmentEntry();
		e.id = r.getString("ID");
		e.productId = r.getString("PRODUCT_ID");
		e.reference = r.getString("PRODUCT_REFERENCE");
		e.name = r.getString("PRODUCT_NAME");
		e.ean = r.getString("PRODUCT_EAN");
		e.size = r.getString("VARIANT_SIZE");
		e.colour = r.getString("VARIANT_COLOUR");
		e.manualDescription = r.getString("MANUAL_DESCRIPTION");
		e.manualEan = r.getString("MANUAL_EAN");
		e.note = r.getString("NOTE");
		e.customerId = r.getString("CUSTOMER_ID");
		e.customerName = r.getString("CUSTOMER_NAME");
		e.status = r.getString("STATUS");
		e.createdAt = r.getTimestamp("CREATED_AT");
		e.updatedAt = r.getTimestamp("UPDATED_AT");
		e.createdBy = r.getString("CREATED_BY");
		e.creatorName = r.getString("CREATOR_NAME");
		e.updatedBy = r.getString("UPDATED_BY");
		return e;
	}
	private Connection connection() throws SQLException {
		return session.getConnection();
	}
	private String openKeyExpression() throws SQLException {
		return connection().getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL")
				? "CASE WHEN PRODUCT_ID IS NULL THEN CONCAT('I:', ID) ELSE CONCAT('P:', PRODUCT_ID) END"
				: "CASE WHEN PRODUCT_ID IS NULL THEN 'I:' || ID ELSE 'P:' || PRODUCT_ID END";
	}

	private boolean isValidStatus(String status) {
		return "PENDING".equals(status) || "ORDERED".equals(status) || "RECEIVED".equals(status)
				|| "DELIVERED".equals(status);
	}
}
