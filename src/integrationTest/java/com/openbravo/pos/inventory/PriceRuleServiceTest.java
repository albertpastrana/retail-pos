package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.data.loader.Session;
import java.nio.file.Path;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PriceRuleServiceTest {
	@TempDir
	Path temp;

	private final PriceRule charm = new PriceRule("DEFAULT", null, 47.5, PriceRule.ROUND_CHARM);

	@Test
	public void roundsUpToConfiguredCharmEndings() {
		assertEquals(5.25, PriceRuleService.calculateGross(5.00 / 1.475 / 1.262, charm), 0.0001);
		assertEquals(5.50, PriceRuleService.calculateGross(5.26 / 1.475 / 1.262, charm), 0.0001);
		assertEquals(5.75, PriceRuleService.calculateGross(5.586 / 1.475 / 1.262, charm), 0.0001);
		assertEquals(6.25, PriceRuleService.calculateGross(5.96 / 1.475 / 1.262, charm), 0.0001);
		assertEquals(10.95, PriceRuleService.calculateGross(9.96 / 1.475 / 1.262, charm), 0.0001);
		assertEquals(55.95, PriceRuleService.calculateGross(55.86 / 1.475 / 1.262, charm), 0.0001);
	}

	@Test
	public void reportsTrueMarginOnRetailPrice() {
		assertEquals(35.43, PriceRuleService.calculateMarginPercent(7.07, 10.95), 0.01);
	}

	@Test
	public void addsVatAndEquivalenceSurchargeToFactoryCost() {
		assertEquals(12.62, PriceRuleService.calculateEconomicCost(10.0, 0.21, TaxRegime.EQUIVALENCE_SURCHARGE),
				0.0001);
		assertEquals(11.14, PriceRuleService.calculateEconomicCost(10.0, 0.10, TaxRegime.EQUIVALENCE_SURCHARGE),
				0.0001);
		assertEquals(10.45, PriceRuleService.calculateEconomicCost(10.0, 0.04, TaxRegime.EQUIVALENCE_SURCHARGE),
				0.0001);
	}

	@Test
	public void normalAccountingAppliesMarkupBeforeVat() {
		PriceRule noRounding = new PriceRule("DEFAULT", null, 47.5, PriceRule.ROUND_NONE);
		assertEquals(17.85, PriceRuleService.calculateGross(10.0, 0.21, noRounding, TaxRegime.NORMAL), 0.0001);
	}

	@Test
	public void roundsBatchPercentResultUsingConfiguredEndings() {
		assertEquals(13.95, PriceRuleService.roundGross(13.31, PriceRule.ROUND_CHARM), 0.0001);
		assertEquals(13.95, PriceRuleService.roundGross(13.31, PriceRule.ROUND_95), 0.0001);
		assertEquals(13.31, PriceRuleService.roundGross(13.31, PriceRule.ROUND_NONE), 0.0001);
	}

	@Test
	public void onlyProductsActuallyFollowingTheBrandRuleHaveLockedPrices() throws Exception {
		Session session = new Session("jdbc:derby:" + temp.resolve("rules") + ";create=true", null, null);
		try {
			try (Statement sql = session.getConnection().createStatement()) {
				sql.executeUpdate("CREATE TABLE PRICE_RULES (ID VARCHAR(30), BRAND VARCHAR(30), "
						+ "MARKUP_PERCENT DOUBLE, ROUNDING VARCHAR(30), TAX_REGIME VARCHAR(40))");
				sql.executeUpdate("CREATE TABLE TAXES (CATEGORY VARCHAR(30), RATE DOUBLE, "
						+ "CUSTCATEGORY VARCHAR(30), VALIDFROM TIMESTAMP)");
				sql.executeUpdate("CREATE TABLE PRODUCTS (ID VARCHAR(30), BRAND VARCHAR(30), "
						+ "PRICEBUY DOUBLE, PRICESELL DOUBLE, TAXCAT VARCHAR(30))");
				sql.executeUpdate("INSERT INTO PRICE_RULES VALUES ('default', NULL, 47.5, 'NONE', 'NORMAL')");
				sql.executeUpdate("INSERT INTO TAXES VALUES ('vat', 0.21, NULL, CURRENT_TIMESTAMP)");
				sql.executeUpdate(
						"INSERT INTO PRODUCTS VALUES ('calculated', NULL, 10, " + (17.85 / 1.21) + ", 'vat')");
				sql.executeUpdate("INSERT INTO PRODUCTS VALUES ('manual', NULL, 10, 9, 'vat')");
			}
			PriceRuleService rules = new PriceRuleService(session);
			assertTrue(rules.isRulePricedProduct("calculated"));
			assertFalse(rules.isRulePricedProduct("manual"));
		} finally {
			session.close();
		}
	}
}
