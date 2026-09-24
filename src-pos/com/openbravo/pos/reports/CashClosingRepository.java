package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Database boundary for cash/session closing totals. */
public final class CashClosingRepository {

	private static final String CLOSINGS_SQL = "SELECT C.MONEY, C.HOST, C.HOSTSEQUENCE, C.DATESTART, C.DATEEND, "
			+ "COUNT(DISTINCT T.ID), " + "COALESCE(SUM(CASE WHEN T.TICKETTYPE = 0 THEN P.TOTAL ELSE 0 END), 0), "
			+ "COALESCE(SUM(CASE WHEN T.TICKETTYPE = 1 THEN P.TOTAL ELSE 0 END), 0) " + "FROM CLOSEDCASH C "
			+ "LEFT JOIN RECEIPTS R ON R.MONEY = C.MONEY "
			+ "LEFT JOIN TICKETS T ON T.ID = R.ID AND T.TICKETTYPE IN (0, 1) "
			+ "LEFT JOIN PAYMENTS P ON P.RECEIPT = R.ID " + "AND P.PAYMENT NOT IN ('paperin', 'paperout') "
			+ "WHERE C.DATESTART >= ? AND C.DATESTART < ? "
			+ "GROUP BY C.MONEY, C.HOST, C.HOSTSEQUENCE, C.DATESTART, C.DATEEND "
			+ "ORDER BY C.DATESTART, C.HOST, C.HOSTSEQUENCE";

	public List<CashClosingRow> load(Connection connection, SalesSummaryParameters parameters) throws SQLException {
		List<CashClosingRow> rows = new ArrayList<CashClosingRow>();
		try (PreparedStatement statement = connection.prepareStatement(CLOSINGS_SQL)) {
			statement.setTimestamp(1, timestamp(parameters.getStartInclusive()));
			statement.setTimestamp(2, timestamp(parameters.getEndExclusive()));
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					rows.add(new CashClosingRow(result.getString(1), result.getString(2), result.getInt(3),
							result.getTimestamp(4), result.getTimestamp(5), result.getInt(6), result.getDouble(7),
							result.getDouble(8)));
				}
			}
		}
		return rows;
	}

	private static Timestamp timestamp(Date value) {
		return new Timestamp(value.getTime());
	}
}
