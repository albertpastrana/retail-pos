package com.openbravo.pos.inventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.openbravo.data.loader.Session;

public final class PriceRuleService {

	public static final double DEFAULT_TAX_RATE = 0.21;
	private static final double PRICE_TOLERANCE = 0.011;

	private final Session session;

	public PriceRuleService(Session session) {
		this.session = session;
	}

	public List<PriceRule> findAll() throws SQLException {
		List<PriceRule> rules = new ArrayList<PriceRule>();
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("SELECT ID, BRAND, MARKUP_PERCENT, ROUNDING FROM PRICE_RULES "
						+ "ORDER BY CASE WHEN BRAND IS NULL THEN 0 ELSE 1 END, BRAND");
				ResultSet results = statement.executeQuery()) {
			while (results.next()) {
				rules.add(readRule(results));
			}
		}
		return rules;
	}

	public PriceRule findForBrand(String brand) throws SQLException {
		Connection connection = session.getConnection();
		if (brand != null && !brand.trim().isEmpty()) {
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT ID, BRAND, MARKUP_PERCENT, ROUNDING FROM PRICE_RULES WHERE BRAND = ?")) {
				statement.setString(1, brand.trim());
				try (ResultSet results = statement.executeQuery()) {
					if (results.next()) {
						return readRule(results);
					}
				}
			}
		}
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT ID, BRAND, MARKUP_PERCENT, ROUNDING FROM PRICE_RULES WHERE BRAND IS NULL");
				ResultSet results = statement.executeQuery()) {
			return results.next() ? readRule(results) : new PriceRule("DEFAULT", null, 47.5, PriceRule.ROUND_CHARM);
		}
	}

	public List<String> findBrands() throws SQLException {
		List<String> brands = new ArrayList<String>();
		try (PreparedStatement statement = session.getConnection().prepareStatement(
				"SELECT DISTINCT BRAND FROM PRODUCTS WHERE BRAND IS NOT NULL AND BRAND <> '' ORDER BY BRAND");
				ResultSet results = statement.executeQuery()) {
			while (results.next()) {
				brands.add(results.getString(1));
			}
		}
		return brands;
	}

	public TaxRegime getTaxRegime() throws SQLException {
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("SELECT TAX_REGIME FROM PRICE_RULES WHERE BRAND IS NULL");
				ResultSet results = statement.executeQuery()) {
			return results.next() ? TaxRegime.fromDatabase(results.getString(1))
					: TaxRegime.EQUIVALENCE_SURCHARGE;
		}
	}

	public void saveTaxRegime(TaxRegime regime) throws SQLException {
		try (PreparedStatement statement = session.getConnection()
				.prepareStatement("UPDATE PRICE_RULES SET TAX_REGIME = ? WHERE BRAND IS NULL")) {
			statement.setString(1, regime.name());
			statement.executeUpdate();
		}
	}

	public void save(PriceRule rule) throws SQLException {
		Connection connection = session.getConnection();
		int changed;
		try (PreparedStatement update = connection
				.prepareStatement("UPDATE PRICE_RULES SET MARKUP_PERCENT = ?, ROUNDING = ? WHERE ID = ?")) {
			update.setDouble(1, rule.getMarkupPercent());
			update.setString(2, rule.getRounding());
			update.setString(3, rule.getId());
			changed = update.executeUpdate();
		}
		if (changed == 0) {
			try (PreparedStatement insert = connection.prepareStatement(
					"INSERT INTO PRICE_RULES (ID, BRAND, MARKUP_PERCENT, ROUNDING) VALUES (?, ?, ?, ?)")) {
				insert.setString(1, rule.getId() == null ? UUID.randomUUID().toString() : rule.getId());
				if (rule.getBrand() == null) {
					insert.setNull(2, java.sql.Types.VARCHAR);
				} else {
					insert.setString(2, rule.getBrand());
				}
				insert.setDouble(3, rule.getMarkupPercent());
				insert.setString(4, rule.getRounding());
				insert.executeUpdate();
			}
		}
	}

	public int countProductsUsingOldRule(String brand, PriceRule oldRule) throws SQLException {
		int count = 0;
		TaxRegime regime = getTaxRegime();
		Map<String, Double> taxRates = findCurrentTaxRates();
		String sql = "SELECT PRICEBUY, PRICESELL, TAXCAT FROM PRODUCTS WHERE PRICEBUY > 0" + (brand == null
				? " AND (BRAND IS NULL OR BRAND NOT IN " + "(SELECT BRAND FROM PRICE_RULES WHERE BRAND IS NOT NULL))"
				: " AND BRAND = ?");
		try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
			if (brand != null) {
				statement.setString(1, brand);
			}
			try (ResultSet results = statement.executeQuery()) {
				while (results.next()) {
					double taxRate = taxRate(taxRates, results.getString(3));
					if (matchesRule(results.getDouble(1), results.getDouble(2), taxRate, oldRule, regime)) {
						count++;
					}
				}
			}
		}
		return count;
	}

	public int repriceProductsUsingOldRule(String brand, PriceRule oldRule, PriceRule newRule) throws SQLException {
		Connection connection = session.getConnection();
		boolean oldAutoCommit = connection.getAutoCommit();
		int changed = 0;
		TaxRegime regime = getTaxRegime();
		Map<String, Double> taxRates = findCurrentTaxRates();
		connection.setAutoCommit(false);
		try {
			String sql = "SELECT ID, PRICEBUY, PRICESELL, TAXCAT FROM PRODUCTS WHERE PRICEBUY > 0"
					+ (brand == null
							? " AND (BRAND IS NULL OR BRAND NOT IN "
									+ "(SELECT BRAND FROM PRICE_RULES WHERE BRAND IS NOT NULL))"
							: " AND BRAND = ?");
			try (PreparedStatement select = connection.prepareStatement(sql);
					PreparedStatement update = connection
							.prepareStatement("UPDATE PRODUCTS SET PRICESELL = ? WHERE ID = ?")) {
				if (brand != null) {
					select.setString(1, brand);
				}
				try (ResultSet results = select.executeQuery()) {
					while (results.next()) {
						double buy = results.getDouble(2);
						double taxRate = taxRate(taxRates, results.getString(4));
						if (matchesRule(buy, results.getDouble(3), taxRate, oldRule, regime)) {
							update.setDouble(1, calculateGross(buy, taxRate, newRule, regime) / (1.0 + taxRate));
							update.setString(2, results.getString(1));
							update.addBatch();
							changed++;
						}
					}
				}
				update.executeBatch();
			}
			connection.commit();
			return changed;
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(oldAutoCommit);
		}
	}

	public static double calculateGross(double factoryPrice, PriceRule rule) {
		return calculateGross(factoryPrice, DEFAULT_TAX_RATE, rule, TaxRegime.EQUIVALENCE_SURCHARGE);
	}

	public static double calculateGross(double factoryPrice, double taxRate, PriceRule rule, TaxRegime regime) {
		double cost = calculateGrossCostBasis(factoryPrice, taxRate, regime);
		double raw = cost * (1.0 + rule.getMarkupPercent() / 100.0);
		if (PriceRule.ROUND_NONE.equals(rule.getRounding())) {
			return roundCents(raw);
		}
		if (PriceRule.ROUND_95.equals(rule.getRounding())) {
			return roundTo95(raw);
		}
		return roundCharm(raw);
	}

	public static double calculateEconomicCost(double factoryPrice, double taxRate, TaxRegime regime) {
		if (regime == TaxRegime.NORMAL) {
			return factoryPrice;
		}
		return factoryPrice * (1.0 + taxRate + equivalenceSurchargeRate(taxRate));
	}

	public static double calculateGrossCostBasis(double factoryPrice, double taxRate, TaxRegime regime) {
		if (regime == TaxRegime.NORMAL) {
			return factoryPrice * (1.0 + taxRate);
		}
		return calculateEconomicCost(factoryPrice, taxRate, regime);
	}

	public static double equivalenceSurchargeRate(double taxRate) {
		if (Math.abs(taxRate - 0.21) < 0.0001) {
			return 0.052;
		}
		if (Math.abs(taxRate - 0.10) < 0.0001) {
			return 0.014;
		}
		if (Math.abs(taxRate - 0.04) < 0.0001) {
			return 0.005;
		}
		return 0.0;
	}

	public static double calculateMarginPercent(double cost, double gross) {
		return gross <= 0.0 ? 0.0 : (gross - cost) / gross * 100.0;
	}

	private static boolean matchesRule(double factoryPrice, double storedNet, double taxRate, PriceRule rule,
			TaxRegime regime) {
		double storedGross = storedNet * (1.0 + taxRate);
		return Math.abs(storedGross - calculateGross(factoryPrice, taxRate, rule, regime)) < PRICE_TOLERANCE;
	}

	private Map<String, Double> findCurrentTaxRates() throws SQLException {
		Map<String, Double> rates = new HashMap<String, Double>();
		try (PreparedStatement statement = session.getConnection().prepareStatement(
				"SELECT CATEGORY, RATE FROM TAXES WHERE CUSTCATEGORY IS NULL "
						+ "AND VALIDFROM <= CURRENT_TIMESTAMP ORDER BY VALIDFROM");
				ResultSet results = statement.executeQuery()) {
			while (results.next()) {
				rates.put(results.getString(1), Double.valueOf(results.getDouble(2)));
			}
		}
		return rates;
	}

	private static double taxRate(Map<String, Double> rates, String category) {
		Double rate = rates.get(category);
		return rate == null ? 0.0 : rate.doubleValue();
	}

	private static double roundCharm(double value) {
		if (value >= 10.0) {
			return roundTo95(value);
		}
		int euros = (int) Math.floor(value);
		double[] endings = { 0.25, 0.50, 0.75, 0.95 };
		for (double ending : endings) {
			double candidate = euros + ending;
			if (candidate + 0.0000001 >= value) {
				return candidate;
			}
		}
		double next = euros + 1.0;
		return next >= 10.0 ? next + 0.95 : next + 0.25;
	}

	private static double roundTo95(double value) {
		int euros = (int) Math.floor(value);
		double candidate = euros + 0.95;
		return candidate + 0.0000001 >= value ? candidate : euros + 1.95;
	}

	private static double roundCents(double value) {
		return Math.ceil(value * 100.0 - 0.0000001) / 100.0;
	}

	private static PriceRule readRule(ResultSet results) throws SQLException {
		return new PriceRule(results.getString(1), results.getString(2), results.getDouble(3), results.getString(4));
	}
}
