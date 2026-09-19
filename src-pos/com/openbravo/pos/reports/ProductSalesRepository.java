package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database boundary for product sales. */
public final class ProductSalesRepository {

	private static final String SALES_SQL = "SELECT L.PRODUCT, P.REFERENCE, P.NAME, C.NAME, "
			+ "SUM(L.UNITS), SUM(L.UNITS * L.PRICE) " + "FROM TICKETLINES L " + "JOIN TICKETS T ON T.ID = L.TICKET "
			+ "JOIN RECEIPTS R ON R.ID = T.ID " + "LEFT JOIN PRODUCTS P ON P.ID = L.PRODUCT "
			+ "LEFT JOIN CATEGORIES C ON C.ID = P.CATEGORY " + "WHERE R.DATENEW >= ? AND R.DATENEW < ? "
			+ "AND T.TICKETTYPE IN (0, 1) " + "GROUP BY L.PRODUCT, P.REFERENCE, P.NAME, C.NAME "
			+ "ORDER BY SUM(L.UNITS * L.PRICE) DESC, P.NAME";

	public List<ProductSalesRow> load(Connection connection, SalesSummaryParameters parameters) throws SQLException {
		List<ProductSalesRow> rows = new ArrayList<ProductSalesRow>();
		try (PreparedStatement statement = connection.prepareStatement(SALES_SQL)) {
			statement.setTimestamp(1, new java.sql.Timestamp(parameters.getStartInclusive().getTime()));
			statement.setTimestamp(2, new java.sql.Timestamp(parameters.getEndExclusive().getTime()));
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					rows.add(new ProductSalesRow(valueOrEmpty(result.getString(2)), valueOrEmpty(result.getString(3)),
							valueOrEmpty(result.getString(4)), result.getDouble(5), result.getDouble(6)));
				}
			}
		}
		return rows;
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
}
