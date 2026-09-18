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
		List<ReplenishmentEntry> result = new ArrayList<ReplenishmentEntry>();
		String term = search == null ? "" : search.trim();
		String pattern = "%" + term + "%";
		String sql = "SELECT * FROM REPLENISHMENT_ENTRIES WHERE ("
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
						? "AND STATUS <> 'RECEIVED' "
						: "PENDING".equals(status) || "ORDERED".equals(status) || "RECEIVED".equals(status)
								? "AND STATUS = '" + status + "' "
								: "")
				+ "ORDER BY CREATED_AT DESC";
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
		if (!"PENDING".equals(status) && !"ORDERED".equals(status) && !"RECEIVED".equals(status))
			throw new BasicException("Invalid replenishment status");
		try (PreparedStatement ps = connection().prepareStatement(
				"UPDATE REPLENISHMENT_ENTRIES SET STATUS=?, OPEN_PRODUCT_ID=CASE WHEN ?='RECEIVED' THEN NULL ELSE PRODUCT_ID END, UPDATED_AT=?, UPDATED_BY=? WHERE ID=?")) {
			ps.setString(1, status);
			ps.setString(2, status);
			ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			ps.setString(4, user);
			ps.setString(5, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new BasicException(e);
		}
	}

	public void update(ReplenishmentEntry e, String user) throws BasicException {
		try (PreparedStatement ps = connection().prepareStatement(
				"UPDATE REPLENISHMENT_ENTRIES SET PRODUCT_ID=?, PRODUCT_REFERENCE=?, PRODUCT_NAME=?, PRODUCT_EAN=?, MANUAL_DESCRIPTION=?, MANUAL_EAN=?, NOTE=?, CUSTOMER_ID=?, CUSTOMER_NAME=?, STATUS=?, OPEN_PRODUCT_ID=CASE WHEN ?='RECEIVED' THEN NULL ELSE PRODUCT_ID END, UPDATED_AT=?, UPDATED_BY=? WHERE ID=?")) {
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
			ps.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
			ps.setString(13, user);
			ps.setString(14, e.id);
			ps.executeUpdate();
		} catch (SQLException ex) {
			throw new BasicException(ex);
		}
	}

	private void insert(Connection c, ReplenishmentEntry e) throws SQLException {
		String sql = "INSERT INTO REPLENISHMENT_ENTRIES (ID,PRODUCT_ID,PRODUCT_REFERENCE,PRODUCT_NAME,PRODUCT_EAN,VARIANT_SIZE,VARIANT_COLOUR,MANUAL_DESCRIPTION,MANUAL_EAN,NOTE,CUSTOMER_ID,CUSTOMER_NAME,STATUS,OPEN_PRODUCT_ID,CREATED_AT,UPDATED_AT,CREATED_BY,UPDATED_BY) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
			ps.executeUpdate();
		}
	}
	private ReplenishmentEntry get(String id) throws SQLException {
		try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM REPLENISHMENT_ENTRIES WHERE ID=?")) {
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
		e.updatedBy = r.getString("UPDATED_BY");
		return e;
	}
	private Connection connection() throws SQLException {
		return session.getConnection();
	}
}
