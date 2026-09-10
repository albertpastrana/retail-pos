package com.openbravo.pos.inventory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.ticket.ProductInfoExt;

public final class SaleService {

	private final Session session;

	public SaleService(Session session) {
		this.session = session;
	}

	public java.util.List<ProductInfoExt> findOnSale() throws BasicException {
		return new PreparedSentence(session,
				"SELECT " + ProductInfoExt.infoColumns()
						+ " FROM PRODUCTS WHERE SALE_PERCENT IS NOT NULL AND SALE_PERCENT > 0 ORDER BY NAME",
				null, ProductInfoExt.getSerializerRead()).list();
	}

	public void setSalePercent(Collection<String> productIds, Double percent) throws SQLException {
		if (productIds.isEmpty()) {
			return;
		}
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("UPDATE PRODUCTS SET SALE_PERCENT = ? WHERE ID = ?")) {
			for (String id : productIds) {
				if (percent == null) {
					statement.setNull(1, java.sql.Types.DOUBLE);
				} else {
					statement.setDouble(1, percent.doubleValue());
				}
				statement.setString(2, id);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}
}
