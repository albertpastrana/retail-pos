package com.openbravo.pos.inventory;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PriceRuleServiceTest {

	private final PriceRule charm = new PriceRule("DEFAULT", null, 47.5, PriceRule.ROUND_CHARM);

	@Test
	public void roundsUpToConfiguredCharmEndings() {
		assertEquals(5.25, PriceRuleService.calculateGross(5.00 / 1.475, charm), 0.0001);
		assertEquals(5.50, PriceRuleService.calculateGross(5.26 / 1.475, charm), 0.0001);
		assertEquals(5.75, PriceRuleService.calculateGross(5.586 / 1.475, charm), 0.0001);
		assertEquals(6.25, PriceRuleService.calculateGross(5.96 / 1.475, charm), 0.0001);
		assertEquals(10.95, PriceRuleService.calculateGross(9.96 / 1.475, charm), 0.0001);
		assertEquals(55.95, PriceRuleService.calculateGross(55.86 / 1.475, charm), 0.0001);
	}

	@Test
	public void reportsTrueMarginOnRetailPrice() {
		assertEquals(35.43, PriceRuleService.calculateMarginPercent(7.07, 10.95), 0.01);
	}
}
