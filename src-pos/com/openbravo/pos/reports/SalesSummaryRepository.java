package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Database boundary for the period sales summary. */
public final class SalesSummaryRepository {

	private static final String SUMMARY_SQL = "SELECT " + "COUNT(DISTINCT T.ID), "
			+ "COALESCE(SUM(CASE WHEN T.TICKETTYPE = 0 THEN COALESCE(P.TOTAL, 0) ELSE 0 END), 0), "
			+ "COALESCE(SUM(CASE WHEN T.TICKETTYPE = 1 THEN COALESCE(P.TOTAL, 0) ELSE 0 END), 0), "
			+ "COALESCE(SUM(CASE WHEN T.TICKETTYPE IN (0, 1) THEN COALESCE(X.AMOUNT, 0) ELSE 0 END), 0), "
			+ "COUNT(CASE WHEN T.TICKETTYPE = 1 THEN T.ID ELSE NULL END) " + "FROM TICKETS T "
			+ "JOIN RECEIPTS R ON R.ID = T.ID "
			+ "LEFT JOIN (SELECT RECEIPT, SUM(TOTAL) AS TOTAL FROM PAYMENTS GROUP BY RECEIPT) P "
			+ "ON P.RECEIPT = R.ID "
			+ "LEFT JOIN (SELECT RECEIPT, SUM(AMOUNT) AS AMOUNT FROM TAXLINES GROUP BY RECEIPT) X "
			+ "ON X.RECEIPT = R.ID " + "WHERE R.DATENEW >= ? AND R.DATENEW < ? " + "AND T.TICKETTYPE IN (0, 1)";
	private static final String LOYALTY_SQL = "SELECT COUNT(*), COALESCE(SUM(-L.UNITS * L.PRICE), 0) "
			+ "FROM TICKETLINES L JOIN RECEIPTS R ON R.ID = L.TICKET "
			+ "WHERE R.DATENEW >= ? AND R.DATENEW < ? AND L.LOYALTYREDEMPTION = TRUE";

	public SalesSummary load(Connection connection, SalesSummaryParameters parameters) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SUMMARY_SQL)) {
			statement.setTimestamp(1, new java.sql.Timestamp(parameters.getStartInclusive().getTime()));
			statement.setTimestamp(2, new java.sql.Timestamp(parameters.getEndExclusive().getTime()));
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return new SalesSummary(0, 0.0, 0.0, 0.0);
				}
				return new SalesSummary(result.getInt(1), result.getDouble(2), result.getDouble(3), result.getDouble(4),
						result.getInt(5));
			}
		}
	}

	public SalesSummaryComparison loadComparison(Connection connection, SalesSummaryParameters current,
			SalesSummaryParameters previous) throws SQLException {
		return new SalesSummaryComparison(load(connection, current), load(connection, previous),
				loadLoyalty(connection, current), loadLoyalty(connection, previous));
	}

	private LoyaltyRedemptionSummary loadLoyalty(Connection connection, SalesSummaryParameters parameters)
			throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(LOYALTY_SQL)) {
			statement.setTimestamp(1, new java.sql.Timestamp(parameters.getStartInclusive().getTime()));
			statement.setTimestamp(2, new java.sql.Timestamp(parameters.getEndExclusive().getTime()));
			try (ResultSet result = statement.executeQuery()) {
				result.next();
				return new LoyaltyRedemptionSummary(result.getInt(1), result.getDouble(2));
			}
		}
	}
}
