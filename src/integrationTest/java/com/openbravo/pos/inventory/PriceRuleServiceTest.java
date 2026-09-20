package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PriceRuleServiceTest {

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
}
