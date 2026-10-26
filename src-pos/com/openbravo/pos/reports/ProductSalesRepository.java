package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** Database boundary for product sales. */
public final class ProductSalesRepository {

	private static final String SALES_SQL = "SELECT L.PRODUCT, P.REFERENCE, P.NAME, P.CATEGORY, P.BRAND, "
			+ "SUM(L.UNITS), SUM(L.UNITS * L.PRICE), COUNT(*) " + "FROM TICKETLINES L "
			+ "JOIN TICKETS T ON T.ID = L.TICKET " + "JOIN RECEIPTS R ON R.ID = T.ID "
			+ "LEFT JOIN PRODUCTS P ON P.ID = L.PRODUCT " + "WHERE R.DATENEW >= ? AND R.DATENEW < ? "
			+ "AND T.TICKETTYPE IN (0, 1) " + "GROUP BY L.PRODUCT, P.REFERENCE, P.NAME, P.CATEGORY, P.BRAND "
			+ "ORDER BY SUM(L.UNITS * L.PRICE) DESC, P.NAME";

	public Map<String, Category> categories(Connection connection) throws SQLException {
		Map<String, Category> categories = new HashMap<>();
		try (PreparedStatement statement = connection.prepareStatement("SELECT ID, NAME, PARENTID FROM CATEGORIES");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				Category category = new Category(result.getString(1), result.getString(2), result.getString(3));
				categories.put(category.id, category);
			}
		}
		return categories;
	}

	static String categoryPath(String id, Map<String, Category> categories) {
		List<String> names = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		while (id != null && visited.add(id) && categories.containsKey(id)) {
			Category category = categories.get(id);
			names.add(0, category.name);
			id = category.parentId;
		}
		return String.join(" › ", names);
	}

	static final class Category {
		final String id;
		final String name;
		final String parentId;

		Category(String id, String name, String parentId) {
			this.id = id;
			this.name = name;
			this.parentId = parentId;
		}
	}

	public List<ProductSalesRow> load(Connection connection, SalesSummaryParameters parameters) throws SQLException {
		List<ProductSalesRow> rows = new ArrayList<ProductSalesRow>();
		try (PreparedStatement statement = connection.prepareStatement(SALES_SQL)) {
			statement.setTimestamp(1, new java.sql.Timestamp(parameters.getStartInclusive().getTime()));
			statement.setTimestamp(2, new java.sql.Timestamp(parameters.getEndExclusive().getTime()));
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					rows.add(new ProductSalesRow(result.getString(1), valueOrEmpty(result.getString(2)),
							valueOrEmpty(result.getString(3)), "", result.getString(4),
							valueOrEmpty(result.getString(5)), result.getString(1) == null, result.getDouble(6),
							result.getDouble(7), result.getLong(8)));
				}
			}
		}
		return rows;
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
}
