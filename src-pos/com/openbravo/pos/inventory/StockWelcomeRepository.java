package com.openbravo.pos.inventory;

import com.openbravo.pos.forms.AppView;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/** Read-only catalogue and replenishment facts for the stock landing screen. */
final class StockWelcomeRepository {
	static final class Product {
		String id, name, reference, code, category, tax, brand, rule, status, customer;
		double price, salePercent, units;
		Timestamp lastMovement;
	}

	static final class Queue {
		int pending, ordered, customers;
		Timestamp oldest;
	}

	private final Connection connection;

	StockWelcomeRepository(AppView app) throws SQLException {
		this(app.getSession().getConnection());
	}

	StockWelcomeRepository(Connection connection) {
		this.connection = connection;
	}

	boolean emptyCatalogue() throws SQLException {
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT COUNT(*) FROM PRODUCTS WHERE CATEGORY <> 'gift-vouchers'");
				ResultSet rows = statement.executeQuery()) {
			rows.next();
			return rows.getInt(1) == 0;
		}
	}

	List<Product> search(String text) throws SQLException {
		String sql = "SELECT P.ID, P.NAME, P.REFERENCE, P.CODE, P.PRICESELL, P.SALE_PERCENT, "
				+ "C.NAME AS CATEGORY_NAME, TC.NAME AS TAX_NAME, "
				+ "(SELECT MAX(T.RATE) FROM TAXES T WHERE T.CATEGORY=P.TAXCAT "
				+ "AND T.CUSTCATEGORY IS NULL AND T.VALIDFROM <= CURRENT_TIMESTAMP "
				+ "AND T.VALIDFROM=(SELECT MAX(T2.VALIDFROM) FROM TAXES T2 "
				+ "WHERE T2.CATEGORY=P.TAXCAT AND T2.CUSTCATEGORY IS NULL "
				+ "AND T2.VALIDFROM <= CURRENT_TIMESTAMP)) AS TAX_RATE, " + "P.BRAND, R.MARKUP_PERCENT, R.ROUNDING, "
				+ "(SELECT SUM(S.UNITS) FROM STOCKCURRENT S WHERE S.PRODUCT=P.ID) AS UNITS, "
				+ "(SELECT MAX(D.DATENEW) FROM STOCKDIARY D WHERE D.PRODUCT=P.ID) AS LAST_MOVEMENT, "
				+ "E.STATUS, E.CUSTOMER_NAME FROM PRODUCTS P "
				+ "JOIN CATEGORIES C ON C.ID=P.CATEGORY JOIN TAXCATEGORIES TC ON TC.ID=P.TAXCAT "
				+ "LEFT JOIN PRICE_RULES R ON R.BRAND=P.BRAND "
				+ "LEFT JOIN REPLENISHMENT_ENTRIES E ON E.OPEN_PRODUCT_ID=P.ID "
				+ "WHERE UPPER(P.CODE)=UPPER(?) OR UPPER(P.REFERENCE) LIKE UPPER(?) "
				+ "OR UPPER(P.NAME) LIKE UPPER(?) "
				+ "ORDER BY CASE WHEN UPPER(P.CODE)=UPPER(?) THEN 0 ELSE 1 END, P.NAME";
		List<Product> products = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, text);
			statement.setString(2, "%" + text + "%");
			statement.setString(3, "%" + text + "%");
			statement.setString(4, text);
			statement.setMaxRows(6);
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					Product product = new Product();
					product.id = rows.getString("ID");
					product.name = rows.getString("NAME");
					product.reference = rows.getString("REFERENCE");
					product.code = rows.getString("CODE");
					product.price = rows.getDouble("PRICESELL");
					product.salePercent = rows.getDouble("SALE_PERCENT");
					product.category = rows.getString("CATEGORY_NAME");
					product.tax = rows.getString("TAX_NAME");
					String rate = rows.getString("TAX_RATE");
					if (rate != null) {
						product.tax += " · " + NumberFormat.getPercentInstance().format(rows.getDouble("TAX_RATE"));
					}
					product.brand = rows.getString("BRAND");
					String markup = rows.getString("MARKUP_PERCENT");
					product.rule = markup == null ? null : markup + "% · " + rows.getString("ROUNDING");
					product.units = rows.getDouble("UNITS");
					product.lastMovement = rows.getTimestamp("LAST_MOVEMENT");
					product.status = rows.getString("STATUS");
					product.customer = rows.getString("CUSTOMER_NAME");
					products.add(product);
				}
			}
		}
		return products;
	}

	Queue queue() throws SQLException {
		Queue queue = new Queue();
		try (PreparedStatement statement = connection.prepareStatement("SELECT STATUS, COUNT(*), MIN(CREATED_AT), "
				+ "SUM(CASE WHEN CUSTOMER_ID IS NOT NULL THEN 1 ELSE 0 END) "
				+ "FROM REPLENISHMENT_ENTRIES WHERE STATUS <> 'RECEIVED' GROUP BY STATUS");
				ResultSet rows = statement.executeQuery()) {
			while (rows.next()) {
				if ("PENDING".equals(rows.getString(1))) {
					queue.pending = rows.getInt(2);
					Timestamp created = rows.getTimestamp(3);
					queue.oldest = created;
				} else {
					queue.ordered = rows.getInt(2);
				}
				queue.customers += rows.getInt(4);
			}
		}
		return queue;
	}
}
