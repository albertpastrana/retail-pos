package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database boundary for taxes grouped by name and rate. */
public final class TaxSummaryRepository {

	private static final String TAX_SQL = "SELECT X.NAME, X.RATE, SUM(L.BASE), SUM(L.AMOUNT) " + "FROM TAXLINES L "
			+ "JOIN TAXES X ON X.ID = L.TAXID " + "JOIN RECEIPTS R ON R.ID = L.RECEIPT "
			+ "JOIN TICKETS T ON T.ID = R.ID " + "WHERE R.DATENEW >= ? AND R.DATENEW < ? "
			+ "AND T.TICKETTYPE IN (0, 1) " + "GROUP BY X.NAME, X.RATE " + "ORDER BY X.NAME, X.RATE";

	public List<TaxSummaryRow> load(Connection connection, SalesSummaryParameters parameters) throws SQLException {
		List<TaxSummaryRow> rows = new ArrayList<TaxSummaryRow>();
		try (PreparedStatement statement = connection.prepareStatement(TAX_SQL)) {
			statement.setTimestamp(1, new java.sql.Timestamp(parameters.getStartInclusive().getTime()));
			statement.setTimestamp(2, new java.sql.Timestamp(parameters.getEndExclusive().getTime()));
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					rows.add(new TaxSummaryRow(result.getString(1), result.getDouble(2), result.getDouble(3),
							result.getDouble(4)));
				}
			}
		}
		return rows;
	}
}
