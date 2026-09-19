package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database boundary for sales grouped by payment method. */
public final class PaymentSalesRepository {

	private static final String SALES_SQL = "SELECT PM.PAYMENT, "
			+ "SUM(CASE WHEN T.TICKETTYPE = 0 THEN PM.TOTAL ELSE 0 END), "
			+ "SUM(CASE WHEN T.TICKETTYPE = 1 THEN PM.TOTAL ELSE 0 END) " + "FROM PAYMENTS PM "
			+ "JOIN RECEIPTS R ON R.ID = PM.RECEIPT " + "JOIN TICKETS T ON T.ID = R.ID "
			+ "WHERE R.DATENEW >= ? AND R.DATENEW < ? " + "AND T.TICKETTYPE IN (0, 1) " + "GROUP BY PM.PAYMENT "
			+ "ORDER BY PM.PAYMENT";

	public List<PaymentSalesRow> load(Connection connection, SalesSummaryParameters parameters) throws SQLException {
		List<PaymentSalesRow> rows = new ArrayList<PaymentSalesRow>();
		try (PreparedStatement statement = connection.prepareStatement(SALES_SQL)) {
			statement.setTimestamp(1, new java.sql.Timestamp(parameters.getStartInclusive().getTime()));
			statement.setTimestamp(2, new java.sql.Timestamp(parameters.getEndExclusive().getTime()));
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					rows.add(new PaymentSalesRow(result.getString(1), result.getDouble(2), result.getDouble(3)));
				}
			}
		}
		return rows;
	}
}
