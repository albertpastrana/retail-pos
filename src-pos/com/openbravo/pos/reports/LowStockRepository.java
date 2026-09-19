package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database boundary for low-stock products. */
public final class LowStockRepository {

	private static final String LOW_STOCK_SQL = "SELECT P.REFERENCE, P.NAME, C.NAME, L.NAME, "
			+ "COALESCE(SUM(SC.UNITS), 0), SL.STOCKSECURITY, COALESCE(SL.STOCKMAXIMUM, 0) " + "FROM STOCKLEVEL SL "
			+ "JOIN PRODUCTS P ON P.ID = SL.PRODUCT " + "JOIN CATEGORIES C ON C.ID = P.CATEGORY "
			+ "JOIN LOCATIONS L ON L.ID = SL.LOCATION "
			+ "LEFT JOIN STOCKCURRENT SC ON SC.LOCATION = SL.LOCATION AND SC.PRODUCT = SL.PRODUCT "
			+ "WHERE SL.STOCKSECURITY IS NOT NULL "
			+ "GROUP BY P.REFERENCE, P.NAME, C.NAME, L.NAME, SL.STOCKSECURITY, SL.STOCKMAXIMUM "
			+ "HAVING COALESCE(SUM(SC.UNITS), 0) < SL.STOCKSECURITY " + "ORDER BY L.NAME, P.NAME";

	public List<LowStockRow> load(Connection connection) throws SQLException {
		List<LowStockRow> rows = new ArrayList<LowStockRow>();
		try (PreparedStatement statement = connection.prepareStatement(LOW_STOCK_SQL);
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				rows.add(new LowStockRow(result.getString(1), result.getString(2), result.getString(3),
						result.getString(4), result.getDouble(5), result.getDouble(6), result.getDouble(7)));
			}
		}
		return rows;
	}
}
