package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database boundary for customer debt. */
public final class CustomerDebtRepository {

	private static final String DEBT_SQL = "SELECT SEARCHKEY, NAME, COALESCE(CURDEBT, 0), "
			+ "COALESCE(MAXDEBT, 0), COALESCE(MAXDEBT, 0) - COALESCE(CURDEBT, 0) " + "FROM CUSTOMERS "
			+ "ORDER BY NAME, SEARCHKEY";

	public List<CustomerDebtRow> load(Connection connection) throws SQLException {
		List<CustomerDebtRow> rows = new ArrayList<CustomerDebtRow>();
		try (PreparedStatement statement = connection.prepareStatement(DEBT_SQL);
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				rows.add(new CustomerDebtRow(valueOrEmpty(result.getString(1)), valueOrEmpty(result.getString(2)),
						result.getDouble(3), result.getDouble(4), result.getDouble(5)));
			}
		}
		return rows;
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
}
